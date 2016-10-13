
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2015, Arno Unkrig
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

package de.unkrig.antology;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;

import de.unkrig.antology.type.Subelement;
import de.unkrig.antology.type.Subelement.Name_Value;
import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.time.PointOfTime;

/**
 * Abstract base class for {@link URLConnection}-related tasks.
 */
public
class AbstractUrlConnectionTask extends Task {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * The URL configured for this task.
     */
    @Nullable protected URL url;

    /**
     * Whether the "httpFollowRedirects2" feature is configured.
     *
     * @see #setHttpFollowRedirects2(boolean)
     */
    protected boolean httpFollowRedirects2;

    private final List<Subelement.Name_Value> requestProperties = new ArrayList<Subelement.Name_Value>();
    @Nullable private Boolean                 allowUserInteraction;
    private int                               connectTimeout  = -1;
    private int                               readTimeout     = -1;
    private long                              ifModifiedSince = -1L;
    @Nullable private Boolean                 useCaches;
    private int                               httpChunkLength   = -1;
    private int                               httpContentLength = -1;
    @Nullable private Boolean                 httpFollowRedirects;

    // ATTRIBUTE SETTERS

    /**
     * Whether the underlying URL should be examined in a context in which it makes sense to allow user interactions,
     * such as popping up an authentication dialog.
     *
     * @see URLConnection#setAllowUserInteraction(boolean)
     */
    public void
    setAllowUserInteraction(boolean v) { this.allowUserInteraction = v; }

    /**
     * The timeout, in milliseconds, to be used when opening a communications link to the resource referenced by the
     * underlying {@link URLConnection}. If the timeout expires before the connection can be established, a
     * {@link java.net.SocketTimeoutException} is raised. A timeout of zero is interpreted as an infinite timeout.
     *
     * @see URLConnection#setConnectTimeout(int)
     */
    public void
    setConnectTimeout(int milliseconds) { this.connectTimeout = milliseconds; }

    /**
     * The read timeout, in milliseconds. If the timeout expires before there is data available for read, a
     * {@link java.net.SocketTimeoutException} is raised. A timeout of zero is interpreted as an infinite timeout.
     *
     * @see URLConnection#setReadTimeout(int)
     */
    public void
    setReadTimeout(int milliseconds) { this.readTimeout = milliseconds; }

    /**
     * Set the value of the {@code ifModifiedSince} field of the underlying {@link URLConnection}.
     *
     * @see URLConnection#setIfModifiedSince(long)
     */
    public void
    setIfModifiedSince(PointOfTime value) { this.ifModifiedSince = value.milliseconds(); }

    /**
     * Set the value of the {@code useCaches} field of the underlying {@link URLConnection}.
     *
     * @see URLConnection#setUseCaches(boolean)
     */
    public void
    setUseCaches(boolean v) { this.useCaches = v; }

    /**
     * Enables the "chunked streaming mode". A negative value or zero configures a "default chunk size" (typically 4096
     * bytes), otherwise the given chunk length.
     *
     * @see HttpURLConnection#setChunkedStreamingMode(int)
     * @see #setHttpContentLength(int)
     */
    public void
    setHttpChunkLength(int chunkLength) { this.httpChunkLength = chunkLength; }

    /**
     * Enables the "fixed length streaming mode". The given value must equal the number of bytes that pose the request
     * body, or an error will occur.
     *
     * @see HttpURLConnection#setFixedLengthStreamingMode(int)
     * @see #setHttpChunkLength(int)
     */
    public void
    setHttpContentLength(int contentLength) { this.httpContentLength = contentLength; }

    /**
     * Whether HTTP redirects (requests with response code 3xx) should be automatically followed.
     *
     * @ant.defaultValue true
     * @see #setHttpFollowRedirects2(boolean)
     * @see HttpURLConnection#setFollowRedirects(boolean)
     */
    public void
    setHttpFollowRedirects(boolean v) { this.httpFollowRedirects = v; }

    /**
     * Similar to {@link #setHttpFollowRedirects(boolean)}, but additionally allows for redirects across protocols,
     * e.g. from "{@code http://host/index.html}" to "{@code https://host/index.html}".
     *
     * @ant.defaultValue false
     */
    public void
    setHttpFollowRedirects2(boolean v) { this.httpFollowRedirects2 = v; }

    /**
     * Designates the resource to access.
     */
    public void
    setUrl(URL url) {
        if (this.url != null) throw new BuildException("Cannot set more than one source");
        this.url = url;
    }

    // SUBELEMENT ADDERS

    /**
     * The URL can be specified as a string and/or by syntactic components, as described in <a
     * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>, section 3.
     * The individual syntactic components take precendence or those in the string.
     * If a component is neither configured individually, nor in the string, then default values take effect as
     * documented below.
     * <p>The following attributes and subelements are mutually exclusive:</p>
     * <dl>
     *   <dd>{@code context="..."}</dd>
     *   <dd>{@code <context>}</dd>
     * </dl>
     */
    public static
    class UrlElement extends ProjectComponent {

        @Nullable private URL    context;
        @Nullable private String spec;
        @Nullable private String protocol;
        @Nullable private String host;
        private int              port = -1;
        @Nullable private String userInfo;
        @Nullable private String path;
        @Nullable private String query;
        @Nullable private String ref;

        /**
         * A textual definition of the URL. Individual components can be overridden with other attributes.
         */
        public void
        addText(String text) {
            text = text.trim();
            if (text.isEmpty()) return;

            if (this.spec != null) throw new BuildException("Element has more than one text");
            this.spec = this.getProject().replaceProperties(text);
        }

        /**
         * The "base URL" for a relative URL (a simplified alternative for the {@code <context>} subelement described
         * below) (defaults to none).
         */
        public void
        setContext(String contextUrl) throws MalformedURLException {
            if (this.context != null) throw new BuildException("'context=...' and '<context>' are mutually exclusive");
            this.context = new URL(contextUrl);
        }

        /**
         * The "protocol" component of the URL.
         *
         *  @ant.defaultValue http
         */
        public void
        setProtocol(String schema) { this.protocol = schema; }

        /**
         * The "host" component of the URL.
         *
         * @ant.defaultValue localhost
         */
        public void
        setHost(String host) { this.host = host; }

        /**
         * The "port" component of the URL.
         * The default value Depends on the "protocol" component of the URL
         */
        public void
        setPort(int portNumber) { this.port = portNumber; }

        /**
         * The "userinfo" component of the URL, like "<var>user</var> {@code [ ':'}<var>password</var>{@code ]}".
         * Defaults to none.
         */
        public void
        setUserInfo(String userInfo) { this.userInfo = userInfo; }

        /**
         * The "path" component of the URL. (An absolute path begins with a slash ("/"), a relative path doesn't.)
         *
         * @ant.defaultValue /
         */
        public void
        setPath(String path) { this.path = path; }

        /**
         * The "query" component of the URL; is appended to the URL with a question mark ("{@code ?}").
         * Can alternatively be configured through {@code <formField>} subelements.
         */
        public void
        setQuery(String queryString) { this.query = queryString; }

        /**
         * The "ref" component (also known as "anchor" or "fragment identifier").
         * Defaults to none.
         */
        public void
        setRef(String anchor) { this.ref = anchor; }

        /**
         * These subelements are encoded and added to the query part of the URL as described in
         * <a href="http://www.ietf.org/rfc/rfc1866.txt">RFC 1866</a>, section 8.2.2.
         */
        public void
        addConfiguredFormField(Name_Value subelement) throws UnsupportedEncodingException {
            String s = URLEncoder.encode(subelement.name, "UTF-8") + '=' + URLEncoder.encode(subelement.value, "UTF-8");
            this.query = this.query == null ? s : this.query + '&' + s;
        }

        /**
         * Configures the "base URL" for a relative URL.
         * <p>
         * An alternative to {@code context=...}; must appear at most once.
         */
        public void
        addConfiguredContext(UrlElement context) throws MalformedURLException {
            if (this.context != null) throw new BuildException("'context=...' and '<context>' are mutually exclusive");
            this.context = context.getURL();
        }

        /**
         * @return {@code t1} if it is not {@code null}, otherwise {@code t2}
         */
        @Nullable private static <T> T
        or(@Nullable T t1, @Nullable T t2) { return t1 != null ? t1 : t2; }

        /**
         * @return {@code i1} if it is not -1, otherwise {@code i2}
         */
        private static int
        or(int i1, int i2) { return i1 != -1 ? i1 : i2; }

        /** @return An URL based on the various attributes and subelements */
        public URL
        getURL() throws MalformedURLException {

            // Create the URL based on the 'context' URL and the 'spec' string.
            URL url = new URL(this.context, this.spec != null ? this.spec : "http://localhost/");

            // Modify individual components of the URL.
            final String protocol = UrlElement.or(UrlElement.this.protocol, url.getProtocol());
            final String userInfo = UrlElement.or(UrlElement.this.userInfo, url.getUserInfo());
            final String host     = UrlElement.or(UrlElement.this.host,     url.getHost());
            final int    port     = UrlElement.or(UrlElement.this.port,     url.getPort());
            final String path     = UrlElement.or(UrlElement.this.path,     url.getPath());
            final String query    = UrlElement.or(UrlElement.this.query,    url.getQuery());
            final String ref      = UrlElement.or(UrlElement.this.ref,      url.getRef());

            final String userInfoHost = userInfo == null ? host : userInfo + '@' + host;

            String file = path;
            if (query != null) file += '?' + query;
            if (ref   != null) file += '#' + ref;

            return new URL(protocol, userInfoHost, port, file);
        }
    }

    /**
     * Designates the resource to access.
     *
     * @see URL#openConnection()
     */
    public void
    addConfiguredUrl(UrlElement urlElement) throws MalformedURLException {
        this.setUrl(urlElement.getURL());
    }

    // SUBELEMENT ADDERS

    /**
     * Configures a "request property". (Request properties have different meanings for different protocols.)
     *
     * @see URLConnection#addRequestProperty(java.lang.String, java.lang.String)
     */
    public void
    addConfiguredRequestProperty(Name_Value requestProperty) {
        this.requestProperties.add(requestProperty);
    }

    /**
     * Sets the (non-HTTP) properties of the <var>conn</var>.
     *
     * @see #setAllowUserInteraction(boolean)
     * @see #setConnectTimeout(int)
     * @see #setReadTimeout(int)
     * @see #setIfModifiedSince(String)
     * @see #setUseCaches(boolean)
     */
    protected void
    configureUrlConnection(URLConnection conn) {

        for (Subelement.Name_Value requestProperty : AbstractUrlConnectionTask.this.requestProperties) {
            conn.addRequestProperty(requestProperty.name, requestProperty.value);
        }
        if (this.allowUserInteraction != null) conn.setAllowUserInteraction(this.allowUserInteraction);
        if (this.connectTimeout       != -1)   conn.setConnectTimeout(this.connectTimeout);
        if (this.readTimeout          != -1)   conn.setReadTimeout(this.readTimeout);
        if (this.ifModifiedSince      != -1L)  conn.setIfModifiedSince(this.ifModifiedSince);
        if (this.useCaches            != null) conn.setUseCaches(this.useCaches);
    }

    /**
     * Sets the "{@code http*}" properties of the <var>httpConn</var>.
     *
     * @see #setHttpChunkLength(int)
     * @see #setHttpContentLength(int)
     * @see #setHttpFollowRedirects(boolean)
     */
    protected void
    configureHttpUrlConnection(HttpURLConnection httpConn) {

        if (this.httpChunkLength != -1) {
            httpConn.setChunkedStreamingMode(this.httpChunkLength);
        }

        if (this.httpContentLength != -1) {
            httpConn.setFixedLengthStreamingMode(this.httpContentLength);
        }

        Boolean httpFollowRedirects = this.httpFollowRedirects;
        if (httpFollowRedirects != null) httpConn.setInstanceFollowRedirects(httpFollowRedirects);
    }
}
