
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.antology.type;

import static de.unkrig.antology.type.ResourceTransformer.Transformation.CONTENT;
import static de.unkrig.antology.type.ResourceTransformer.Transformation.CONTENT_AS_FILE;
import static de.unkrig.antology.type.ResourceTransformer.Transformation.NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Transforms a resource collection into another resource collection with the same size by creating a "resulting
 * resource" from each "input resource".
 */
public
class ResourceTransformer extends ProjectComponent implements ResourceCollection, Iterable<Resource> {

    /** How the name/content of the result resource ist to be computed. */
    public enum Transformation { NAME, NAME_AS_FILE, CONTENT, CONTENT_AS_FILE }

    @Nullable private ResourceCollection delegate;
    private Transformation               name           = NAME;
    private Transformation               content        = CONTENT;
    private Charset                      contentCharset = Charset.defaultCharset();

    /**
     * Determines the <em>name</em> of the resulting resource.
     * <dl>
     *   <dt>{@link Transformation#NAME}:</dt>
     *   <dd>
     *     The input resource's <em>name</em>
     *   </dd>
     *   <dt>{@link Transformation#CONTENT}:</dt>
     *   <dd>
     *     The input resource's <em>content</em> (decoded with the {@link #setContentCharset(String)})
     *   </dd>
     * </dl>
     *
     * @ant.defaultValue {@link Transformation#NAME}
     */
    public void
    setName(Transformation value) { this.name = value; }

    /**
     * Determines the <em>contents</em> of the resulting resource.
     * <dl>
     *   <dt>{@link Transformation#NAME}:</dt>
     *   <dd>
     *     The input resource's <em>name</em> (encoded with {@link #setContentCharset(String)})
     *   </dd>
     *   <dt>{@link Transformation#NAME_AS_FILE}:</dt>
     *   <dd>
     *     The content of the file named by the input resource's name
     *   </dd>
     *   <dt>{@link Transformation#CONTENT}:</dt>
     *   <dd>
     *     The <em>content</em> of the input resource
     *   </dd>
     *   <dt>{@link Transformation#CONTENT_AS_FILE}:</dt>
     *   <dd>
     *     The content of the file named by the content of the input resource (decoded with the {@link
     *     #setContentCharset(String)})
     *   </dd>
     * </dl>
     *
     * @ant.defaultValue {@link Transformation#CONTENT}
     */
    public void
    setContent(Transformation value) { this.content = value; }

    /**
     * Relevant for the {@link #setName(Transformation)} and/or {@link #setContent(Transformation)} attributes; see
     * there.
     */
    public void setContentCharset(String charset) { this.contentCharset = Charset.forName(charset); }

    /**
     * The resources to process (may occur multiply).
     */
    public void
    addConfigured(ResourceCollection value) {
        if (this.delegate != null) throw new BuildException("No more than one resource collection subelement allowed");
        this.delegate = value;
    }

    // IMPLEMENTATION OF ResourceCollection

    @Override public boolean
    isFilesystemOnly() {
        return this.content == CONTENT || this.content == CONTENT_AS_FILE;
    }

    @Override public Iterator<Resource>
    iterator() {

        final ResourceCollection delegate = this.delegate;
        if (delegate == null) throw new BuildException("Resource collection subelement missing");

        return new Iterator<Resource>() {

            @SuppressWarnings("unchecked") final Iterator<Resource> iter = delegate.iterator();

            @Override public boolean
            hasNext() { return this.iter.hasNext(); }

            @Override public Resource
            next() {
                final Resource resource = this.iter.next();

                String name;
                switch (ResourceTransformer.this.name) {
                case NAME:
                    name = resource.getName();
                    break;
                case CONTENT:
                    name = ResourceTransformer.this.readContents(resource);
                    break;
                default:
                    throw new BuildException(
                        "'nameTransformation=\""
                        + ResourceTransformer.this.name
                        + "\" is not allowed"
                    );
                }
                Resource result = new Resource(name) {

                    @Override public long
                    getSize() {
                        switch (ResourceTransformer.this.content) {
                        case NAME:
                            return this.getName().getBytes(ResourceTransformer.this.contentCharset).length;
                        case NAME_AS_FILE:
                            return (int) new File(resource.getName()).length();
                        case CONTENT:
                            return resource.size();
                        case CONTENT_AS_FILE:
                            return (int) new File(ResourceTransformer.this.readContents(resource)).length();
                        default:
                            throw new IllegalArgumentException(
                                String.valueOf(ResourceTransformer.this.content)
                            );
                        }
                    }

                    @Override public InputStream
                    getInputStream() throws IOException {
                        switch (ResourceTransformer.this.content) {
                        case NAME:
                            {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                OutputStreamWriter    w    = new OutputStreamWriter(baos);
                                w.write(resource.getName());
                                w.close();
                                return new ByteArrayInputStream(baos.toByteArray());
                            }
                        case NAME_AS_FILE:
                            return new FileInputStream(new File(resource.getName()));
                        case CONTENT:
                            return resource.getInputStream();
                        case CONTENT_AS_FILE:
                            return new FileInputStream(new File(ResourceTransformer.this.readContents(resource)));
                        default:
                            throw new IllegalArgumentException(
                                String.valueOf(ResourceTransformer.this.content)
                            );
                        }
                    }

                    @Override public boolean
                    isExists() {
                        switch (ResourceTransformer.this.content) {
                        case NAME:
                            return true;
                        case NAME_AS_FILE:
                            return new File(resource.getName()).exists();
                        case CONTENT:
                            return true;
                        case CONTENT_AS_FILE:
                            return new File(ResourceTransformer.this.readContents(resource)).exists();
                        default:
                            throw new IllegalArgumentException(
                                String.valueOf(ResourceTransformer.this.content)
                            );
                        }
                    }
                };
                result.setProject(ResourceTransformer.this.getProject());
                return result;
            }

            @Override public void
            remove() { this.iter.remove(); }
        };
    }

    @Override public int
    size() {

        final ResourceCollection delegate = this.delegate;
        if (delegate == null) throw new BuildException("Resource collection subelement missing");
        return delegate.size();
    }

    private String
    readContents(Resource resource) {
        try {
            InputStreamReader r = new InputStreamReader(
                resource.getInputStream(),
                ResourceTransformer.this.contentCharset
            );
            try {
                String content = ResourceTransformer.readAll(r);
                r.close();
                return content;
            } finally {
                try { r.close(); } catch (Exception e) {}
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    private static String
    readAll(Reader r) throws IOException {
        char[] buffer = new char[8192];

        StringWriter sw = new StringWriter();
        for (;;) {
            int n = r.read(buffer);
            if (n == -1) break;
            sw.write(buffer, 0, n);
        }
        return sw.toString();
    }
}
