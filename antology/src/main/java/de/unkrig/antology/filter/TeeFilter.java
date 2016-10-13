
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2014, Arno Unkrig
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

package de.unkrig.antology.filter;

import java.io.FilterReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.ChainableReader;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.ReaderInputStream;

import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.io.WyeReader;
import de.unkrig.commons.nullanalysis.NotNull;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Passes data through unmodified, and re-uses it for some other purpose.
 * <p>
 *   The following purposes are implemented:
 * </p>
 * <ul>
 *   <li>Copying the data to STDOUT</li>
 *   <li>Reading the data as a properties file</li>
 *   <li>Writing the data into a resource</li>
 *   <li>Storing the data in a property</li>
 *   <li>Nothing (the default if none of the above are configured)</li>
 * </ul>
 * <p>
 *   The data may optionally be piped through another filter chain before it is processed.
 * </p>
 * <p>
 *   The following attributes and subelements are mutually exclusive:
 * </p>
 * <ul>
 *   <li>{@link #setStdout(boolean)}</li>
 *   <li>{@link #addConfiguredProperties(PropertiesSink)}</li>
 *   <li>{@link #setTofile(Resource)}</li>
 *   <li>{@link #setProperty(String)}</li>
 * </ul>
 */
@NotNullByDefault(false) public
class TeeFilter extends ProjectComponent implements ChainableReader {

    // ---------------- Implementation of ChainableReader ----------------

    @Override public Reader
    chain(final Reader reader) {

        // Create a pipe.
        final PipedWriter pipedWriter = new PipedWriter();
        PipedReader       pipedReader;
        try {
            pipedReader = new PipedReader(pipedWriter);
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        }

        // Tee into the pipe.
        final Reader wyeReader = new WyeReader(reader, pipedWriter);

        // Wrap the read end of the pipe into the configured filter chains.
        final Reader reader2;
        {
            ChainReaderHelper crh = new ChainReaderHelper();
            crh.setPrimaryReader(pipedReader);
            crh.setFilterChains(this.filterChains);
            crh.setProject(this.getProject());
            reader2 = crh.getAssembledReader();
        }

        // Start a background thread that reads the the pipe (through the filter chains) into the sink.
        final Thread thread = new Thread() {

            @Override public void
            run() {
                try {
                    TeeFilter.this.sink.readAll(reader2);
                } catch (IOException ioe) {
                    throw new BuildException(ioe);
                }
            }
        };
        thread.start();

        // Take care that the background thread is JOINed when the reader is closed.
        return new FilterReader(wyeReader) {

            @Override public void
            close() throws IOException {
                super.close();

                // Close the write end of the pipe.
                pipedWriter.close();

                // The background thread should terminate quickly because the read end of the pipe will signal
                // 'end-of-input'.
                try {
                    thread.join();
                } catch (InterruptedException ie) {
                    throw new BuildException(ie);
                }
            }
        };
    }

    // ---------------- ANT attribute setters. ----------------

    /** If {@code true}, then the contents is copied to STDOUT. */
    public void
    setStdout(boolean value) { if (value) this.setSink(TeeFilter.STDOUT_SINK); }

    /**
     * If configured, then the contents is read into a set of properties.
     * Extends the <a href="https://ant.apache.org/manual/Tasks/property.html">{@code <property>} task</a>.
     */
    public void
    addConfiguredProperties(PropertiesSink propertiesSink) { this.setSink(propertiesSink); }

    /** If {@code true}, then the contents is copied into the given resource. */
    public void
    setTofile(final Resource resource) { this.setSink(new ResourceSink(resource)); }

    /** If {@code true}, then the contents is stored in the named property. */
    public void
    setProperty(final String propertyName) {
        this.setSink(new Sink() {

            @Override public void
            readAll(Reader in) throws IOException {
                TeeFilter.this.getProject().setNewProperty(propertyName, IoUtil.readAll(in));
            }
        });
    }

    /**
     * The filter chain to pipe the "feed" data through before processing it. See <a
     * href="https://ant.apache.org/manual/Types/filterchain.html">ant filter chains</a>.
     */
    public final void
    addFilterChain(FilterChain filterChain) { this.filterChains.addElement(filterChain); }

    private final Vector<FilterChain> filterChains = new Vector<FilterChain>();

    // ---------------------------------- IMPLEMENTATION -----------------------------------

    /** Something that consumes the character stream produced by a {@link Reader}. */
    public
    interface Sink {

        /** Consume (typically all) the characters (a.k.a. 'contents') that the given {@link Reader} produces. */
        void readAll(Reader in) throws IOException;
    }

    private void
    setSink(@NotNull Sink sink) {

        if (this.sink != TeeFilter.DEFAULT_SINK) {
            throw new BuildException(
                "Must configure at most one of 'tofile=\"...\"', 'property=\"...\"', "
                + "'stdout=\"true\"' and '<properties>'"
            );
        }

        this.sink = sink;
    }

    private static final Sink NULL_SINK = new Sink() { @Override public void readAll(Reader in) {} };

    private static final Sink STDOUT_SINK = new Sink() {
        @Override public void readAll(Reader in) throws IOException { IoUtil.copy(in, System.out); }
    };

    private static final Sink DEFAULT_SINK = TeeFilter.NULL_SINK;

    @NotNull private Sink sink = TeeFilter.DEFAULT_SINK;

    /***/
    public static
    class PropertiesSink implements Sink {

        class MyPropertyTask extends org.apache.tools.ant.taskdefs.Property {

            @Override public void // Make this method publicly accessible.
            addProperties(Properties props) { super.addProperties(props); }
        }

        private boolean isXml;
        private final MyPropertyTask delegate = new MyPropertyTask();

        /**
         * Whether the content is in XML format ({@code true}) or in properties file format ({@code false}).
         */
        public void
        setIsXml(boolean value) { this.isXml = value; }

        /**
         * Prefix to apply to properties. A "." is appended to the prefix if not specified.
         */
        public void setPrefix(String prefix) { this.delegate.setPrefix(prefix); }

        /**
         * Whether to apply the prefix when expanding properties on the right hand side of a properties file as well.
         */
        public void setPrefixValues(boolean b) { this.delegate.setPrefixValues(b); }

        @Override public void
        readAll(Reader r) throws IOException {

            Properties properties = new Properties();

            if (this.isXml) {
                properties.loadFromXML(new ReaderInputStream(r));
            } else {
                properties.load(r);
            }

            // Use "org.apache.tools.ant.taskdefs.Property" to prefix keys, expand values, and then set the
            // properties.
            this.delegate.addProperties(properties);
        }
    }

    /** A {@link Sink} that copies the contents into a resource. */
    public static
    class ResourceSink implements Sink {
        private final Resource resource;

        public
        ResourceSink(Resource resource) { this.resource = resource; }

        @Override public void
        readAll(Reader in) throws IOException {
            IoUtil.copy(in, this.resource.getOutputStream(), Charset.defaultCharset());
        }
    }
}

