
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2013, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.antology.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.filters.StringInputStream;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;

import de.unkrig.antology.AbstractUrlConnectionTask;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.protocol.Consumer;
import de.unkrig.commons.net.UrlConnections;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * When a given file or resource grows, then this task processes the data that was added, e.g. it prints it to
 * STDOUT.
 * <p>
 *   The name refers to the 'follow mode' of the LESS utility (or the 'follow' option of the TAIL command), which does
 *   more or less the same.
 * </p>
 * <p>
 *   For files, it is easy and fast to test whether a file has grown; for other resources it is not. For resources
 *   {@link #setUrl(URL)} addressed by an HTTP URL}, the following optimizations are implemented:
 * </p>
 * <ul>
 *   <li>
 *     Instead of reading the resource repeatedly, its size and modification are checked with a HEAD request.
 *   </li>
 *   <li>
 *     Instead of reading the <em>entire</em> contents of the resource repeatedly, only the part that was
 *     (presumably) added is retrieved by using the "<code><a
 *     href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35.2">Range</a></code>" request header.
 *   </li>
 * </ul>
 */
public
class FollowTask extends AbstractUrlConnectionTask {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private OutputStream              out           = IoUtil.NULL_OUTPUT_STREAM;
    @Nullable private Charset         charset;
    private final Vector<FilterChain> filterChains  = new Vector<FilterChain>();
    private int                       periodTime    = 3000; // ms
    private int                       timeout       = 300;
    private boolean                   failOnTimeout = true;

    // ATTRIBUTE SETTERS

    /**
     * The file to monitor.
     */
    public void
    setFile(File file) throws MalformedURLException { this.setUrl(new URL("file", null, file.getPath())); }

    /**
     * Write the additional data to the named file.
     */
    public void
    setOutputFile(File outputFile) throws FileNotFoundException {
        this.assertOutIsDefault();
        this.out = new FileOutputStream(outputFile);
    }

    /**
     * Write the additional data to STDOUT.
     */
    public void
    setStdout(boolean stdout) {
        if (!stdout) return;

        this.assertOutIsDefault();
        this.out = System.out;
    }

    /**
     * Write the additional data to STDERR.
     */
    public void
    setStderr(boolean stderr) {
        if (!stderr) return;

        this.assertOutIsDefault();
        this.out = System.err;
    }

    /**
     * If a {@link #addConfigured(FilterChain)} is configured, then the data from the file is DECODED with this
     * encoding, then fed through the {@code <filterchain>}, and then ENCODED again. Defaults to the system default
     * encoding.
     */
    public void
    setEncoding(String encoding) { this.charset = Charset.forName(encoding); }

    // SUBELEMENT ADDERS

    /**
     * If given, then the data that is added to the file is fed through this filter chain, and the task will complete
     * only when the filter chain produces some output.
     * This is useful to check if a specific line of text was appended to the file.
     */
    public void
    addConfigured(FilterChain filterChain) { this.filterChains.add(filterChain); }

    /**
     * The period in milliseconds to check the file's size.
     *
     * @ant.defaultValue 500
     */
    public void
    setPeriodTime(int periodTime) { this.periodTime = periodTime; }

    /**
     * The time (in seconds) when to give up if the file size does not change.
     * A value of '0' indicates to wait indefinitely.
     *
     * @ant.defaultValue 300
     */
    public void
    setTimeout(int timeout) { this.timeout = timeout; }

    /**
     * Whether the execution of the current target should fail or not if the file size does not change.
     *
     * @ant.defaultValue true
     */
    public void
    setFailOnTimeout(boolean failOnTimeout) { this.failOnTimeout = failOnTimeout; }

    @Override public void
    execute() {

        try {
            this.execute2();
        } catch (BuildException be) {
            throw be;
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    interface Followable {

        /**
         * @return {@code null} if the underlying resource has not grown since the last invocation
         */
        @Nullable InputStream getInputStream() throws IOException;
    }

    private void
    execute2() throws Exception {

        final URL url  = this.url;
        if (url == null) throw new BuildException("Source missing - specify \"file=...\" or \"url=...\"");

        final Followable followable = this.urlFollowable(url);

        long expiration = this.timeout <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + 1000L * this.timeout;

        final InputStreamSucker copier = this.copier(this.out);

        // Do NOT check if the file exists, because it is a common use case that a 'log file' does not yet
        // exist. Thus a non-existant file is equivalent with a zero-size file.

        for (;;) {

            InputStream is = followable.getInputStream();
            if (is != null) {
                try {
                    copier.suck(is);
                    is.close();
                } finally {
                    try { is.close(); } catch (Exception e) {}
                }
            }

            if (System.currentTimeMillis() >= expiration) {
                if (this.failOnTimeout) {
                    throw new BuildException(
                        "<follow> timed out at "
                        + new Date()
                        + " after "
                        + this.timeout
                        + " seconds"
                    );
                } else {
                    return;
                }
            }

            try {
                Thread.sleep(this.periodTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BuildException("INTERRUPTED"); // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
        }
    }

    private Followable
    urlFollowable(final URL url) throws IOException {

        return new Followable() {

            long previousSize; // -1 if unknown
            {
                try {
                    URLConnection conn = url.openConnection();
                    this.previousSize = conn.getContentLength();
                } catch (FileNotFoundException fnfe) {

                    // Catch the case where the resource does not exist, because it is a common use case that a "log
                    // file" does not yet exist. Thus a non-existant resource is equivalent with a zero-size resource.
                    this.previousSize = 0;
                }
            }

            long previousModificationTime; // 0 if unknown
            {
                try {
                    URLConnection conn = url.openConnection();
                    this.previousModificationTime = conn.getLastModified();
                } catch (FileNotFoundException fnfe) {

                    // Catch the case where the resource does not exist, because it is a common use case that a "log
                    // file" does not yet exist. Thus a non-existant resource is equivalent with a zero-size resource.
                    this.previousModificationTime = 0;
                }
            }

            @Override @Nullable public InputStream
            getInputStream() throws IOException {

                URLConnection conn;
                try {
                    conn = url.openConnection();

                    FollowTask.this.configureUrlConnection(conn);

                    HttpURLConnection httpConn;
                    if (conn instanceof HttpURLConnection) {
                        httpConn = (HttpURLConnection) conn;

                        FollowTask.this.configureHttpUrlConnection(httpConn);
                        httpConn.setRequestMethod("HEAD");

                        if (FollowTask.this.httpFollowRedirects2) {
                            conn     = UrlConnections.followRedirects2(httpConn);
                            httpConn = conn instanceof HttpURLConnection ? (HttpURLConnection) conn : null;
                        }
                    } else {
                        httpConn = null;
                    }

                    long newSize             = conn.getContentLength();
                    long newModificationTime = conn.getLastModified();

                    // Check whether the file size remains equal.
                    if (newSize != -1 && newSize == this.previousSize) return null;

                    // Check whether the modification time remains equal.
                    if (newModificationTime != 0 && newModificationTime == this.previousModificationTime) return null;

                    if (newSize != -1 && newSize < this.previousSize) {

                        // The file shrank or was removed; this typically means that the file was rotated (i.e. moved
                        // away and created anew). Sadly we cannot get any data that was appended to the OLD file
                        // before the rotation, but at least we can process any new data in the new file.
                        this.previousSize = 0;
                    }

                    if (httpConn != null) {
                        httpConn.disconnect();
                        httpConn = (HttpURLConnection) (conn = url.openConnection());
                        FollowTask.this.configureUrlConnection(httpConn);
                        FollowTask.this.configureHttpUrlConnection(httpConn);
                        if (this.previousSize != -1) {
                            httpConn.addRequestProperty("Range", "bytes=" + this.previousSize + "-");
                        }
                    }

                    InputStream is = conn.getInputStream();
                    try {
                        if (httpConn == null || this.previousSize == -1 || newSize == -1) {
                            if (this.previousSize == -1) {
                                this.previousSize = is.skip(Long.MAX_VALUE);
                                return is;
                            }
                            if (is.skip(this.previousSize) != this.previousSize) {
                                throw new IOException("Could not position input stream");
                            }
                        }
                        if (newSize != -1) {
                            this.previousSize = newSize;
                            return is;
                        } else {
                            return IoUtil.wye(is, IoUtil.lengthWritten(new Consumer<Integer>() {

                                @SuppressWarnings("unqualified-field-access") @Override public void
                                consume(Integer n) { previousSize += n; }
                            }));
                        }
                    } catch (IOException ioe) {
                        try { is.close(); } catch (Exception e) {}
                        throw ioe;
                    } catch (RuntimeException re) {
                        try { is.close(); } catch (Exception e) {}
                        throw re;
                    }
                } catch (FileNotFoundException fnfe) {
                    this.previousSize = 0;
                    return null;
                }
            }
        };
    }

    private
    interface InputStreamSucker {
        long suck(InputStream in) throws IOException;
        void close() throws IOException;
    }

    private InputStreamSucker
    copier(final OutputStream out) {

        if (this.filterChains.isEmpty()) {

            if (this.charset != null) {
                throw new BuildException(
                    "The 'encoding' attribute takes effect only if a '<filterchain>' is configured"
                );
            }

            return new InputStreamSucker() {
                @Override public long suck(InputStream in) throws IOException { return IoUtil.copy(in, out); }
                @Override public void close()                                 {}
            };
        } else {

            final ChainReaderHelper crh;
            {
                crh = new ChainReaderHelper();
                crh.setBufferSize(8192);
                crh.setFilterChains(this.filterChains);
                crh.setProject(this.getProject());
            }

            Charset charset = this.charset;
            if (charset == null) charset = Charset.defaultCharset();

            final Writer w = new OutputStreamWriter(out, charset);

            class ProxyInputStream extends FilterInputStream {

                ProxyInputStream() { super(new StringInputStream("")); }

                /***/
                void setDelegate(InputStream in) { this.in = in; }
            }
            final ProxyInputStream pis = new ProxyInputStream();
            crh.setPrimaryReader(new InputStreamReader(pis, charset));

            // Notice: 'ChainReaderHelper.getAssembledReader()' is anything but omnipotent - it creates the complete
            // filter chain.
            final Reader r = new BufferedReader(crh.getAssembledReader());

            return new InputStreamSucker() {

                @Override public long
                suck(InputStream in) throws IOException {

                    pis.setDelegate(in);
                    return IoUtil.copy(r, w);
                }

                @Override public void close() throws IOException { r.close(); }
            };
        }
    }

    private void
    assertOutIsDefault() {
        if (this.out != IoUtil.NULL_OUTPUT_STREAM) {
            throw new BuildException("Only one of 'outputFile=...', 'stdout=true' and 'stderr=true' allowed");
        }
    }
}
