
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;

import de.unkrig.antology.AbstractUrlConnectionTask;
import de.unkrig.antology.ParametrizedHeaderValue;
import de.unkrig.antology.type.JsonAsProperties;
import de.unkrig.antology.type.Subelement;
import de.unkrig.antology.util.Compat;
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Connects to a resource, writes data to it and/or reads data from it.
 * <p>To configure HTTP authentication, use the {@link SetAuthenticatorTask} task.</p>
 *
 * <p>The following attributes are mutually exclusive:</p>
 * <dl>
 *   <dd>{@link #setHttpChunkLength(int)}</dd>
 *   <dd>{@link #setHttpContentLength(int)}</dd>
 * </dl>
 *
 * <p>Also the following attributes and subelements are mutually exclusive:</p>
 * <dl>
 *   <dd>{@link #setUrl(URL)}</dd>
 *   <dd>{@link #addConfiguredUrl(de.unkrig.antology.AbstractUrlConnectionTask.UrlElement)}</dd>
 * </dl>
 *
 * <p>Also the following attributes are mutually exclusive:</p>
 * <dl>
 *   <dd>{@link #setDirect(boolean)}</dd>
 *   <dd>{@link #setHttpProxy(String)}</dd>
 *   <dd>{@link #setSocksProxy(String)}</dd>
 * </dl>
 *
 * <p>
 *   To debug HTTP communication, you may want to execute the following <em>before</em> the {@code <urlConnection>}
 *   task:
 * </p>
 * <pre>{@code <!-- Configure super-verbose logging on the HTTP connection. -->
 * <logging>
 *     <logger name="sun.net.www.protocol.http.HttpURLConnection" level="FINEST">
 *         <handler className="java.util.logging.ConsoleHandler">
 *             <attribute name="level" value="FINEST" />
 *         </handler>
 *     </logger>
 * </logging>}
 * </pre>
 */
public
class UrlConnectionTask extends AbstractUrlConnectionTask {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    private static final Logger LOGGER = Logger.getLogger(UrlConnectionTask.class.getName());

    /**
     * The charset that applies if the HTTP "Content-Type:" header is missing or has no "charset" parameter.
     * <p>
     *  See <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>, sections "3.4.1 Missing Charset" and "3.7.1
     *  Canonicalization and Text Defaults".
     *</p>
     */
    public static final Charset HTTP_DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

    @Nullable private Input  input;
    @Nullable private Output output;
    @Nullable private String httpRequestMethod;

    // ATTRIBUTE SETTERS

    /**
     * @see HttpURLConnection#setRequestMethod(java.lang.String)
     */
    public void
    setHttpRequestMethod(String method) { this.httpRequestMethod = method; }

    // SUBELEMENT ADDERS

    /**
     * Configures that data should be copied to the resource.
     *
     * @see URLConnection#getOutputStream()
     * @see URLConnection#setDoOutput(boolean)
     */
    public void
    addConfiguredOutput(OutputElement oe) {

        if (this.output != null) throw new BuildException("Only one '<output>' subelement allowed");
        if (oe.output == null) {
            throw new BuildException((
                "One of 'value=\"...\"', 'file=\"...\"', '<multipartFormData>', '<applicationXWwwFormUrlencoded>' or "
                + "element text must be defined for '<output>'"
            ));
        }
        this.output = oe.output;
    }

    /**
     * <p>The following attributes and subelements are mutually exclusive:</p>
     * <dl>
     *   <dd>{@code file="..."}</dd>
     *   <dd>{@code value="..."}</dd>
     *   <dd>{@code <applicationXWwwFormUrlencoded>}</dd>
     *   <dd>{@code <multipartFormData>}</dd>
     *   <dd>(element text)</dd>
     * </dl>
     */
    public static
    class OutputElement extends ProjectComponent {

        @Nullable private Output output;
        private Charset          charset = Charset.defaultCharset();

        /**
         * The <var>text</var> is written to the resource.
         */
        public void
        setValue(final String text) {

            this.setOutput(new Output() {

                @Override public void
                write(URLConnection conn) throws IOException {
                    OutputStream os = conn.getOutputStream();
                    assert os != null;
                    os.write(text.getBytes(OutputElement.this.charset));
                }

                @Override public String
                toString() { return "Value '" + text + "'"; }
            });
        }

        /**
         * The encoding to use when the {@code value="..."} is written to the resource.
         */
        public void
        setEncoding(String charsetName) { this.charset = Charset.forName(charsetName); }

        /**
         * The contents of the configured file is written to the resource.
         */
        public void
        setFile(final File file) {

            this.setOutput(new Output() {

                @Override public void
                write(URLConnection conn) throws IOException {
                    OutputStream os = conn.getOutputStream();
                    assert os != null;

                    IoUtil.copy(file, os, true);
                }

                @Override public String
                toString() { return "File '" + file + "'"; }
            });
        }

        /**
         * Changes the request property "Content-Type" to "multipart/formdata" and adds the given {@code field} to
         * the output.
         * <p>
         *   See also <a href="http://www.ietf.org/rfc/rfc2388.txt">RFC 2388</a>.
         * </p>
         */
        public void
        addConfiguredMultipartFormData(final MultipartFormDataField field) {

            // Verify that the field's name and value were set.
            {
                String name = field.name;
                if (name == null) throw new BuildException("'multipart/form-data' fields lacks name");

                if (field.value == null) {
                    throw new BuildException("'multipart/form-data' fields '" + name + "' lacks value");
                }
            }

            class MultipartFormDataOutput extends ProjectComponent implements Output  {

                final List<MultipartFormDataField> fields = new ArrayList<MultipartFormDataField>();

                @Override public void
                write(URLConnection conn) throws IOException {

                    // Check and set the 'Content-Type' header, and determine the boundary.
                    final String boundary;
                    {
                        List<String> values = conn.getRequestProperties().get("Content-Type");
                        if (values != null) {
                            if (values.size() != 1) throw new BuildException("More than one 'Content-Type' header");
                            String value = values.get(0);
                            assert value != null;

                            // A header 'Content-Type' exists, verify that it is 'multipart/form-data'.
                            ParametrizedHeaderValue phv = new ParametrizedHeaderValue(value);
                            if (!"multipart/form-data".equalsIgnoreCase(phv.getToken())) {
                                throw new BuildException("Invalid content type '" + value + "'");
                            }
                            String p = phv.getParameter("boundary");
                            if (p == null) {
                                throw new BuildException("Content type '" + value + "' lacks 'boundary' parameter");
                            }
                            boundary = p;
                        } else {

                            // Header 'Content-Type: multipart/form-data; boundary=xyz' does not exist; create it.
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < 10; i++) sb.append((char) ('a' + new Random().nextInt(26)));
                            boundary = sb.toString();
                            conn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        }
                    }

                    OutputStream os = conn.getOutputStream();

                    // Write the fields to the output stream, in 'multipart/form-data' format.
                    this.log("fields=" + this.fields, Project.MSG_DEBUG);
                    for (MultipartFormDataField field : this.fields) {
                        Writable value = field.value;
                        assert value != null;
                        this.log("field=" + field, Project.MSG_DEBUG);
                        os.write(("\r\n--" + boundary + "\r\n").getBytes());
                        {
                            String s = "Content-Disposition: form-data";
                            if (field.name != null) s += "; name=\"" + field.name + "\"";
                            if (field.fileName != null) s += "; filename=\"" + field.fileName + "\"";
                            s += "\r\n";
                            os.write(s.getBytes());
                        }
                        for (Subelement.Name_Value header : field.headers) {
                            os.write((header.name + ": " + header.value + "\r\n").getBytes());
                        }
                        os.write("\r\n".getBytes());
                        this.log("field.value=" + value, Project.MSG_DEBUG);
                        value.write(os);
                    }
                    os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                }

                @Override public String
                toString() {
                    return "multipart/form-data: " + this.fields;
                }
            }

            // Create the 'Output' object, if necessary.
            MultipartFormDataOutput mfdo;
            {
                if (this.output instanceof MultipartFormDataOutput) {
                    mfdo = (MultipartFormDataOutput) this.output;
                    assert mfdo != null;
                } else {
                    mfdo = new MultipartFormDataOutput();
                    mfdo.setProject(this.getProject());
                    this.setOutput(mfdo);
                }
            }

            // Add the field to the Output object.
            mfdo.fields.add(field);
        }

        /**
         * Adds another form field.
         * Also verifies that the "Content-Type" header is either not defined or has the value
         * "application/x-www-form-urlencoded".
         */
        public void
        addConfiguredApplicationXWwwFormUrlencoded(Subelement.Name_Value field) {

            class ApplicationXWwwFormUrlencodedOutput implements Output {

                final List<Subelement.Name_Value> fields = new ArrayList<Subelement.Name_Value>();

                @Override public void
                write(URLConnection conn) throws IOException {

                    // Check and set the 'Content-Type' header.
                    CONTENT_TYPE:
                    {
                        List<String> values = conn.getRequestProperties().get("Content-Type");
                        if (values != null) {
                            if (values.size() != 1) throw new BuildException("More than one 'Content-Type' header");
                            String value = values.get(0);
                            assert value != null;

                            ParametrizedHeaderValue phv = new ParametrizedHeaderValue(value);
                            if (!"application/x-www-form-urlencoded".equalsIgnoreCase(phv.getToken())) {
                                throw new BuildException("Invalid content type '" + phv.getToken() + "'");
                            }

                            break CONTENT_TYPE;
                        }

                        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    }

                    OutputStream os = conn.getOutputStream();

                    boolean first = true;
                    for (Subelement.Name_Value field : this.fields) {
                        if (first) {
                            first = false;
                        } else {
                            os.write('&');
                        }
                        os.write(URLEncoder.encode(field.name, "UTF-8").getBytes());
                        os.write('=');
                        os.write(URLEncoder.encode(field.value, "UTF-8").getBytes());
                    }
                }

                @Override public String
                toString() {
                    return "application/x-www-form-urlencoded: " + this.fields;
                }
            }

            ApplicationXWwwFormUrlencodedOutput axwfuo;
            {
                if (this.output instanceof ApplicationXWwwFormUrlencodedOutput) {
                    axwfuo = (ApplicationXWwwFormUrlencodedOutput) this.output;
                    assert axwfuo != null;
                } else {
                    this.setOutput((axwfuo = new ApplicationXWwwFormUrlencodedOutput()));
                }
            }

            axwfuo.fields.add(field);
        }

        /**
         * A textual definition of the output; equivalent with the {@code value="..."} attribute.
         */
        public void
        addText(String text) {
            text = text.trim();
            if (!text.isEmpty()) this.setValue(this.getProject().replaceProperties(text));
        }

        private void
        setOutput(Output output) {

            if (this.output != null) {
                throw new BuildException((
                    "Only one of 'value=...', 'file=...', '<multipartFormData>', '<applicationXWwwFormUrlencoded>' or "
                    + "element text must be defined"
                ));
            }

            this.output = output;
        }
    }

    /**
     * Configures that the content of the resource should be retrieved.
     */
    public void
    addConfiguredInput(InputElement ie) {

        if (this.input != null) throw new BuildException("Only one '<input>' subelement allowed");
        this.input = ie.getInput();
    }

    /**
     * The following attributes and subelements are mutually exclusive:
     * <dl>
     *   <dd>{@code discard="true"}</dd>
     *   <dd>{@code file="..."}</dd>
     *   <dd>{@code stderr="true"}</dd>
     *   <dd>{@code stdout="true"}</dd>
     *   <dd>{@code property="..."}</dd>
     *   <dd>{@code <jsonAsproperties>}</dd>
     * </dl>
     */
    public static final
    class InputElement extends ProjectComponent {

        @Nullable private Input           input;
        private final Vector<FilterChain> filterChains = new Vector<FilterChain>();
        private Charset                   charset      = Charset.defaultCharset();
        private boolean                   append;

        /**
         * Whether the contents of the resource should be discarded.
         */
        public void
        setDiscard(boolean discard) {
            if (!discard) return;
            this.setInput(new Input() {

                @Override public void
                read(URLConnection conn) throws IOException {
                    InputStream is = conn.getInputStream();
                    assert is != null;

                    if (InputElement.this.filterChains.isEmpty()) {
                        is.skip(Long.MAX_VALUE);
                    } else {
                        InputElement.this.wrapInFilterChains(
                            new InputStreamReader(is, UrlConnectionTask.getConnectionCharset(conn))
                        ).skip(Long.MAX_VALUE);
                    }
                }
            });
        }

        /**
         * The content of the resource is written to STDOUT.
         */
        public void
        setStdout(boolean stdout) {
            if (!stdout) return;

            this.setInput(new OutputStreamInput(System.out));
        }

        /**
         * The content of the resource is written to STDERR.
         */
        public void
        setStderr(boolean stderr) {
            if (!stderr) return;

            this.setInput(new OutputStreamInput(System.err));
        }

        /**
         * The content of the resource is written to the designated file.
         */
        public void
        setFile(final File file) {
            this.setInput(new Input() {

                @Override public void
                read(URLConnection conn) throws IOException {
                    InputStream is = conn.getInputStream();
                    assert is != null;

                    if (InputElement.this.filterChains.isEmpty()) {
                        IoUtil.copy(is, true, file, InputElement.this.append);
                    } else {
                        IoUtil.copy(
                            InputElement.this.wrapInFilterChains(
                                new InputStreamReader(is, UrlConnectionTask.getConnectionCharset(conn))
                            ),
                            true,
                            file,
                            InputElement.this.append,
                            InputElement.this.charset
                        );
                    }
                }
            });
        }

        /**
         * Relevant iff {@code file="..."} is configured.
         */
        public void
        setAppend(boolean value) { this.append = value; }

        /**
         * The content of the resource is stored in the named property.
         */
        public void
        setProperty(final String propertyName) {
            this.setInput(new Input() {

                @Override public void
                read(URLConnection conn) throws IOException {
                    InputStream is = conn.getInputStream();
                    assert is != null;

                    StringWriter sw = new StringWriter();
                    IoUtil.copy(
                        InputElement.this.wrapInFilterChains(
                            new InputStreamReader(is, UrlConnectionTask.getConnectionCharset(conn))
                        ),
                        true,    // closeReader
                        sw,
                        false    // closeWriter
                    );
                    InputElement.this.getProject().setProperty(propertyName, sw.toString());
                }
            });
        }

        /**
         * Assume the retrieved document is in JSON format, and parse it into properties.
         *
         * @see JsonPropertyTask
         */
        public void
        addConfiguredJsonAsProperties(final JsonAsProperties jsonAsProperties) {

            this.setInput(new Input() {

                @Override public void
                read(URLConnection conn) throws IOException {
                    InputStream is = conn.getInputStream();
                    assert is != null;

                    // Parse the resource contents as a JSON document and set a set of properties accordingly.
                    try {
                        jsonAsProperties.execute(
                            new InputStreamReader(is, UrlConnectionTask.getConnectionCharset(conn))
                        );
                    } catch (Exception e) {
                        throw new BuildException("Parsing JSON input: " + e, e);
                    }
                }
            });
        }

        /**
         * Configures a filter chain for the resource content retrieval.
         */
        public void
        addConfigured(FilterChain filterChain) { this.filterChains.add(filterChain); }

        /**
         * Relevant iff {@code file="..."} is configured.
         */
        public void
        setEncoding(String encoding) {
            this.charset = Charset.forName(encoding);
        }

        private void
        setInput(Input input) {
            if (this.input != null) {
                throw new BuildException(
                    "Only one of 'discard=true', 'stdout=true', 'stderr=true', 'file=...', 'property=...' and "
                    + "'<jsonAsProperties>' allowed"
                );
            }
            this.input = input;
        }

        private Input
        getInput() {
            Input input = this.input;
            if (input == null) {
                throw new BuildException(
                    "Exactly one of 'discard=true', 'stdout=true', 'stderr=true', 'file=...', 'property=...' and "
                    + "'<jsonAsProperties>' must be configured"
                );
            }
            return input;
        }

        private Reader
        wrapInFilterChains(Reader r) {
            ChainReaderHelper crh = new ChainReaderHelper();
            crh.setBufferSize(8192);
            crh.setPrimaryReader(r);
            crh.setFilterChains(this.filterChains);
//            crh.setProject(UrlConnectionTask.this.getProject());
            return new BufferedReader(Compat.getAssembledReader(crh));
        }

        private
        class OutputStreamInput implements Input {

            private final OutputStream out;

            OutputStreamInput(OutputStream out) { this.out = out; }

            @Override public void
            read(URLConnection conn) throws IOException {
                InputStream is = conn.getInputStream();
                assert is != null;

                if (InputElement.this.filterChains.isEmpty()) {
                    IoUtil.copy(is, true, this.out, false);
                    this.out.flush();
                } else {
                    Writer w = new OutputStreamWriter(this.out);
                    IoUtil.copy(
                        InputElement.this.wrapInFilterChains(
                            new InputStreamReader(is, UrlConnectionTask.getConnectionCharset(conn))
                        ),
                        true,  // closeReader
                        w,
                        false  // closeWriter
                    );
                    w.flush();
                }
            }
        }
    }

    // END SUBELEMENT ADDERS

    /**
     * An entity that reads from a {@link URLConnection}.
     */
    public
    interface Input {

        /**
         * Reads from the given {@link URLConnection}.
         */
        void read(URLConnection conn) throws IOException;
    }

    /**
     * An entity that writes to an {@link URLConnection}.
     */
    public
    interface Output {

        /**
         * Writes some data to the given {@link URLConnection}.
         *
         * @param conn The connection to write to
         */
        void write(URLConnection conn) throws IOException;
    }

    /**
     * An entity that writes data to an {@link OutputStream}.
     */
    public
    interface Writable {

        /** Writes some data to the given {@link OutputStream}. */
        void write(OutputStream os) throws IOException;
    }

    /**
     * Representation of a field in a 'multipart/form-data'-encoded document.
     * <p>The following attributes are mutually exclusive:</p>
     * <dl>
     *   <dd>{@code value="..."}</dd>
     *   <dd>{@code file="..."}</dd>
     * </dl>
     */
    public static
    class MultipartFormDataField extends ProjectComponent {

        @Nullable private String                  name, fileName;
        @Nullable private Writable                value;
        private final List<Subelement.Name_Value> headers = new ArrayList<Subelement.Name_Value>();

        /**
         * The name of the data field (mandatory).
         */
        public void
        setName(String name) { this.name = name; }

        /**
         * The "file name" of the data field (optional, defaults to none).
         */
        public void
        setFileName(String fileName) { this.fileName = fileName; }

        /**
         * The value of the data field.
         */
        public void
        setValue(final String text) {

            if (this.value != null) {
                throw new BuildException("'value=\"...\"' and 'file=\"...\"' are mutually exclusive");
            }

            this.value = new Writable() {
                @Override public void   write(OutputStream os) throws IOException { os.write(text.getBytes()); }
                @Override public String toString()                                { return '"' + text + '"';   }
            };
        }

        /**
         * The value of the data field is read from the designated file.
         */
        public void
        setFile(final File file) {

            if (this.value != null) {
                throw new BuildException("'value=\"...\"' and 'file=\"...\"' are mutually exclusive");
            }

            this.value = new Writable() {
                @Override public void   write(OutputStream os) throws IOException { IoUtil.copy(file, os, false); }
                @Override public String toString()                                { return file.toString();       }
            };
        }

        /**
         * Adds a header to the data field.
         */
        public void
        addConfiguredHeader(Subelement.Name_Value header) { this.headers.add(header); }

        @Override public String
        toString() {
            return (
                "name='"
                + this.name
                + "', fileName='"
                + this.fileName
                + "', value="
                + this.value
                + ", headers="
                + this.headers
            );
        }
    }

    // EXECUTE

    @Override public void
    execute() throws BuildException {

        try {
            this.execute2();
        } catch (BuildException be) {
            throw be;
        } catch (Exception e) {
            throw new BuildException(this.getTaskName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Executes the task and handles {@link #setHttpFollowRedirects2(boolean) httpFollowRedirects2}.
     */
    private void
    execute2() throws IOException {

        URLConnection conn = this.openConnection();

        for (int attempt = 0; attempt < 10; attempt++) {

            this.log("conn=" + conn, Project.MSG_DEBUG);

            this.configureUrlConnection(conn);

            if (this.input  != null) conn.setDoInput(true);
            if (this.output != null) conn.setDoOutput(true);

            this.log("conn=" + conn, Project.MSG_DEBUG);

            if (conn instanceof HttpURLConnection) {

                URL redirectLocation = this.execute3((HttpURLConnection) conn);
                if (redirectLocation == null) return;

                // Received a REDIRECT; open connection to that location and continue.
                conn = this.openConnection(redirectLocation);
            } else {

                this.execute3(conn);
                return;
            }
        }

        throw new IOException("Giving up after 10 REDIRECTs (last location was '" + conn + "')");
    }

    /** For non-{@link HttpURLConnection}s. */
    private void
    execute3(URLConnection conn) throws IOException {

        if (this.output != null) this.output.write(conn);

        if (this.input != null) this.input.read(conn);
    }

    /**
     * For {@link HttpURLConnection}s.
     *
     * @return {@code null}, or, if the server replied with a REDIRECT, the redirection location
     */
    @Nullable private URL
    execute3(HttpURLConnection httpConn) throws IOException {

        this.log("output=" + this.output, Project.MSG_DEBUG);

        this.configureHttpUrlConnection(httpConn);

        if (this.httpRequestMethod != null) httpConn.setRequestMethod(this.httpRequestMethod);

        this.log("httpConn=" + httpConn, Project.MSG_DEBUG);

        final long now = System.currentTimeMillis();
        try {

            this.log("Request header:", Project.MSG_DEBUG);
            for (Entry<String, List<String>> e : httpConn.getRequestProperties().entrySet()) {
                String       headerFieldName   = e.getKey();
                List<String> headerFieldValues = e.getValue();
                for (String headerFieldValue : headerFieldValues) {
                    this.log("  " + headerFieldName + ": " + headerFieldValue, Project.MSG_DEBUG);
                }
            }

            // Write the output, if any, to the URL connection.
            {
                Output output = this.output;
                if (output != null) {
                    this.log("Wrinting request body...", Project.MSG_DEBUG);
                    output.write(httpConn);
                    this.log("... done.", Project.MSG_DEBUG);
                }
            }

            // Now wait for the response.
            this.log("Waiting for response header...", Project.MSG_DEBUG);
            {
                long l = System.currentTimeMillis();
                try {
                    httpConn.getResponseCode();
                } catch (IOException ioe) {
                    this.log(ioe.toString() + " after " + (System.currentTimeMillis() - l) + " ms", Project.MSG_DEBUG);
                    throw ioe;
                } catch (RuntimeException re) {
                    this.log(re.toString() + " after " + (System.currentTimeMillis() - l) + " ms", Project.MSG_DEBUG);
                    throw re;
                }
                this.log(
                    "... response header received after " + (System.currentTimeMillis() - l) + " ms.",
                    Project.MSG_DEBUG
                );
            }

            this.log("Response header:", Project.MSG_DEBUG);
            for (Entry<String, List<String>> e : httpConn.getHeaderFields().entrySet()) {
                String       headerFieldName   = e.getKey();
                List<String> headerFieldValues = e.getValue();
                for (String headerFieldValue : headerFieldValues) {
                    this.log("  " + headerFieldName + ": " + headerFieldValue, Project.MSG_DEBUG);
                }
            }

            // Process the response code, specifically the 3XX codes which indicate REDIRECTion.
            int responseCode = httpConn.getResponseCode();
            if (this.httpFollowRedirects2 && responseCode >= 300 && responseCode < 400) {

                String redirectionLocation = httpConn.getHeaderField("Location");
                if (redirectionLocation == null) {
                    throw new IOException("Response with code " + responseCode + " lacks the 'Location:' header field");
                }

                URL redirectionLocationUrl;
                try {
                    redirectionLocationUrl = new URL(httpConn.getURL(), redirectionLocation);
                } catch (MalformedURLException mue) {
                    throw new IOException("Invalid redirection location \"" + redirectionLocation + "\"", mue);
                }

                // Special handling for response "303 See other".
                if (responseCode == 303) {
                    URLConnection conn = redirectionLocationUrl.openConnection();
                    if (this.input != null) this.input.read(conn);
                    return null;
                }

                return redirectionLocationUrl;
            }

            if (responseCode < 200 || responseCode >= 400) throw new IOException("HTTP request failed");

            // Now read the response body.
            {
                Input input = this.input;

                if (input != null) {
                    this.log("Reading response body...", Project.MSG_DEBUG);
                    input.read(httpConn);
                    this.log("... done.", Project.MSG_DEBUG);
                }
            }

            return null;
        } catch (BuildException be) {
            throw be;
        } catch (Exception e) {

            this.log(e, Project.MSG_ERR);

            // There is no way to check whether debug logging is "on".
            {
                InputStream errorStream = httpConn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(errorStream, UrlConnectionTask.HTTP_DEFAULT_CHARSET)
                    );
                    for (;;) {
                        String line = br.readLine();
                        if (line == null) break;
                        this.log(line, Project.MSG_DEBUG);
                    }
                }
            }

            throw new IOException((
                "After "
                + (System.currentTimeMillis() - now)
                + " milliseconds: "
                + httpConn.getURL().toString()
                + ", response code="
                + httpConn.getResponseCode()
                + ", response message="
                + httpConn.getResponseMessage()
                + (e.getMessage()  == null ? "" : ": " + e.getMessage())
            ), e);
        }
    }

    private static Charset
    getConnectionCharset(URLConnection urlConnection) {

        // Get the response 'Content-Type' header.
        String contentType = urlConnection.getContentType();
        if (contentType != null) {
            UrlConnectionTask.LOGGER.log(
                Level.FINE,
                "''{0}'': Request content type is ''{1}''",
                new Object[] { urlConnection, contentType }
            );

            // Isolate the 'charset' parameter.
            String charsetToken = new ParametrizedHeaderValue(contentType).getParameter("charset");
            if (charsetToken != null) {
                UrlConnectionTask.LOGGER.log(
                    Level.FINE,
                    "''{0}'': Charset is ''{1}''",
                    new Object[] { urlConnection, charsetToken }
                );
                try {
                    return Charset.forName(charsetToken);
                } catch (Exception e) {
                    UrlConnectionTask.LOGGER.log(
                        Level.FINE,
                        "''{0}'': Invalid charset ''{1}''",
                        new Object[] { urlConnection, charsetToken }
                    );
                    ;
                }
            }
        }
        UrlConnectionTask.LOGGER.log(
            Level.FINE,
            "''{0}'': Assuming default charset ''{1}''",
            new Object[] { urlConnection, UrlConnectionTask.HTTP_DEFAULT_CHARSET }
        );
        return UrlConnectionTask.HTTP_DEFAULT_CHARSET;
    }
}
