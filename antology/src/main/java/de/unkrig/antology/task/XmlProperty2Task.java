
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.collections.ArrayStack;
import de.unkrig.commons.util.collections.Stack;

// SUPPRESS CHECKSTYLE LineLength:30
/**
 * Parses an XML document into ANT properties. Resembles ANT's standard <code><a
 * href="https://ant.apache.org/manual/Tasks/xmlproperty.html">&lt;xmlproperty&gt;</a></code> task, but maps the XML
 * DOM in a different, more detailed way.
 * <p>
 *   This task is the inversion of the {@link PropertyXml2Task} task.
 * </p>
 * <p>
 *   Sets the following properties for the root element of the XML document:
 * </p>
 * <pre>
 * <var>prefix</var>0.<var>root-element-name</var>.$$         = <var>all-texts</var>
 * <var>prefix</var>0.<var>root-element-name</var>._<var>att1-name</var> = <var>att1-value</var>
 * <var>prefix</var>0.<var>root-element-name</var>._<var>att2-name</var> = <var>att2-value</var>
 * <var>prefix</var>0.<var>root-element-name</var>.<var>index</var>.$    = <var>text</var>
 * <var>prefix</var>0.<var>root-element-name</var>.<var>index</var>.#    = <var>comment</var>
 * <var>prefix</var>0.<var>root-element-name</var>.<var>index</var>.!    = <var>cdata-text</var>
 * <var>prefix</var>0.<var>root-element-name</var>.<var>index</var>.?    = <var>processing-instruction-target-and-data</var>
 * </pre>
 * <p>
 *   Sets the following properties for all other elements in the XML document:
 * </p>
 * <pre>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.$$         = <var>all-texts</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>._<var>att1-name</var> = <var>att1-value</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>._<var>att2-name</var> = <var>att2-value</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.$    = <var>text</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.#    = <var>comment</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.!    = <var>cdata-text</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.?    = <var>processing-instruction-target-and-data</var>
 * </pre>
 * <h5>Notes:</h5>
 * <ul>
 *   <li>
 *     The "0" (right after the prefix) reflects the index of the root element (in case the document contains more than
 *     one root element, which is quite uncommon).
 *   </li>
 *   <li>
 *     The <var>index</var> after the <var>element-name</var> reflects the order of the subnodes, and starts at "0".
 *   </li>
 *   <li>
 *     "<var>all-texts</var>" is the concatenated texts of all TEXT subnodes, trimmed.
 *   </li>
 *   <li>
 *     The existence of a property named "{@code *.$$}" indicates the existence of the respective element in the XML
 *     document.
 *   </li>
 * </ul>
 *
 * <h5>Example:</h5>
 * <p>
 *   This document
 * </p>
 * <pre>
 *   &lt;?xml version='1.0' encoding='UTF-8'?>
 *   &lt;project name="prj1">
 *       &lt;target name="trg1">
 *           &lt;echo message="msg" />
 *       &lt;/target>
 *   &lt;/project>
 * </pre>
 * <p>
 *   sets the following properties:
 * </p>
 * <pre>
 *   prefix.0.project.$$                       =
 *   prefix.0.project._name                    = prj1
 *   prefix.0.project.0.$                      = \n\t
 *   prefix.0.project.1.target.$$              =
 *   prefix.0.project.1.target._name           = trg1
 *   prefix.0.project.1.target.0.$             = \n\t\t
 *   prefix.0.project.1.target.1.echo.$$       =
 *   prefix.0.project.1.target.1.echo._message = msg
 *   prefix.0.project.1.target.2.$             = \n\t
 *   prefix.0.project.2.$                      = \n
 * </pre>
 */
public
class XmlProperty2Task extends Task {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    @NotNullByDefault(false)
    class MyErrorHandler implements ErrorHandler {

        private final Project project;
        private boolean       failOnWarning;
        private boolean       suppressWarnings;
        private boolean       failOnError = true;
        private boolean       suppressErrors;

        MyErrorHandler(Project project) { this.project = project; }

        @Override public void
        warning(SAXParseException exception) throws SAXException {
            if (this.failOnWarning) throw exception;
            if (!this.suppressWarnings) this.project.log(exception.getLocalizedMessage(), Project.MSG_WARN);
        }

        @Override public void
        error(SAXParseException exception) throws SAXException {
            if (this.failOnError) throw exception;
            if (!this.suppressErrors) this.project.log(exception.getLocalizedMessage(), Project.MSG_ERR);
        }

        @Override public void
        fatalError(SAXParseException exception) throws SAXException { throw exception; }
    }

    @Nullable private File                 file;
    private String                         prefix = "";
    private boolean                        lexical;
    private final MyErrorHandler           errorHandler = new MyErrorHandler(this.getProject());
    @Nullable private String               text;
    private final List<ResourceCollection> resourceCollections = new ArrayList<ResourceCollection>();

    /**
     * The file to read an XML document from.
     */
    public void
    setFile(File file) { this.file = file; }

    /**
     * This prefix is prepended to each property being set. The default is "" (the empty string).
     */
    public void
    setPrefix(String prefix) { this.prefix = prefix; }

    /**
     * Whether to also set properties for comments and CDATA sections.
     *
     * @ant.defaultValue false
     */
    public void
    setLexical(boolean lexical) { this.lexical = lexical; }

    /**
     * Whether the task fails when the first warning occurrs while parsing the XML document.
     *
     * @ant.defaultValue false
     */
    public void
    setFailOnWarning(boolean failOnWarning) { this.errorHandler.failOnWarning = failOnWarning; }

    /**
     * Whether warnings are not to be logged.
     *
     * @ant.defaultValue false
     */
    public void
    setSuppressWarnings(boolean suppressWarnings) { this.errorHandler.suppressWarnings = suppressWarnings; }

    /**
     * Whether the task fails when the first (non-fatal) error occurrs while parsing the XML document. ("Fatal" errors
     * always cause the task to fail.)
     *
     * @ant.defaultValue true
     */
    public void
    setFailOnError(boolean failOnError) { this.errorHandler.failOnError = failOnError; }

    /**
     * Whether (non-fatal) errors are not to be logged. ("Fatal" errors always cause the task to fail.)
     *
     * @ant.defaultValue false
     */
    public void
    setSuppressErrors(boolean suppressErrors) { this.errorHandler.suppressErrors = suppressErrors; }

    /**
     * All resources will be parsed as XML documents.
     */
    public void
    addConfigured(ResourceCollection resourceCollection) { this.resourceCollections.add(resourceCollection); }

    /**
     * Literal text between "{@code <xmlProperty2>}" and "{@code </xmlProperty2>}" will also be parsed as an XML
     * document.
     * <p>
     *   It may be useful to use a CDATA section, e.g.:
     * </p>
     * <pre>
     *         &lt;xmlProperty2 prefix="prefix.">
     *             &lt;![CDATA[<span style="color:red">&lt;?xml version='1.0' encoding='UTF-8'?>
     * &lt;project name="prj1">
     *     &lt;target name="trg1">
     *         &lt;echo message="msg" />
     *     &lt;/target>
     * &lt;/project>
     *             </span>]]>
     *         &lt;/xmlProperty2>
     * </pre>
     */
    public void
    addText(String text) {
        text = text.trim();
        if (!text.isEmpty()) this.text = text;
    }

    @Override public void
    execute() {

        try {
            this.execute2();
        } catch (SAXParseException spe) {

            String publicId = spe.getPublicId();

            throw new BuildException((
                publicId == null
                ? "Line "
                : publicId + ", line "
            ) + spe.getLineNumber() + ", column " + spe.getColumnNumber() + ": " + spe.getMessage(), spe);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void
    execute2() throws IOException, SAXException {

        if (this.text != null) {
            String text = this.getProject().replaceProperties(this.text);
            this.execute3(new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8"))), null);
        }

        if (this.file != null) {
            this.execute3(new FileInputStream(this.file), this.file.getName());
        }

        for (ResourceCollection resourceCollection : this.resourceCollections) {
            for (Iterator<Resource> it = resourceCollection.iterator(); it.hasNext();) {
                Resource resource = it.next();

                this.execute3(resource.getInputStream(), resource.toString());
            }
        }
    }

    private void
    execute3(InputStream is, @Nullable String publicId) throws IOException, SAXException {

        XmlProperty2Task.execute4(
            is,
            publicId,
            this.prefix,
            this.lexical,
            this.getProject(),
            this.errorHandler
        );
    }

    private static void
    execute4(
        InputStream      is,
        @Nullable String publicId,
        String           propertyNamePrefix,
        boolean          lexical,
        Project          project,
        ErrorHandler     errorHandler
    ) throws IOException, SAXException {

        try {
            XmlProperty2Task.execute5(is, publicId, lexical, propertyNamePrefix, project, errorHandler);
        } finally {
            try { is.close(); } catch (Exception e) {}
        }
    }

    private static void
    execute5(
        InputStream      is,
        @Nullable String publicId,
        boolean          lexical,
        final String     propertyNamePrefix,
        final Project    project,
        ErrorHandler     errorHandler
    ) throws IOException, SAXException {

        @NotNullByDefault(false)
        class MyContentAndLexicalHandler implements ContentHandler, LexicalHandler {

            class El {
                final String  prefix;
                int           index;
                StringBuilder text = new StringBuilder();
                boolean       inCdata;

                El(String prefix) { this.prefix = prefix; }
            }
            final Stack<El> elementStack = new ArrayStack<El>();

            // =============== Implement ContentHandler.

            @Override public void
            startDocument() { this.elementStack.push(new El(propertyNamePrefix)); }

            @Override public void
            endDocument() { assert this.elementStack.size() == 1; }

            @Override public void startPrefixMapping(String prefix, String uri) {}
            @Override public void endPrefixMapping(String prefix)               {}

            @Override public void
            startElement(String uri, String localName, String qName, Attributes atts) {

                El     el = this.elementStack.peek();
                String p  = el.prefix + el.index++ + '.' + qName + '.';

                for (int i = 0; i < atts.getLength(); i++) {
                    project.setProperty(p + '_' + atts.getLocalName(i), atts.getValue(i));
                }

                this.elementStack.push(new El(p));
            }

            @Override public void
            endElement(String uri, String localName, String qName) {

                El el = this.elementStack.pop();
                project.setProperty(el.prefix + "$$", el.text.toString().trim());
            }

            @Override public void
            skippedEntity(String name) {}

            @Override public void
            setDocumentLocator(Locator locator) {}

            @Override public void
            processingInstruction(String target, String data) {

                El el = this.elementStack.peek();
                project.setProperty(el.prefix + el.index++ + ".?", target + ' ' + data);
            }

            @Override public void
            ignorableWhitespace(char[] ch, int start, int length) {}

            @Override public void
            characters(char[] ch, int start, int length) {
                String s = new String(ch, start, length);

                El el = this.elementStack.peek();
                project.setProperty(el.prefix + el.index++ + (el.inCdata ? ".!" : ".$"), s);

                el.text.append(s);
            }

            // =============== Implement LexicalHandler.

            // Entities don't really work... E.g. "&amp;" invokes "startEntity("amp")", then "endEntity("amp")" (so
            // far so good), then "characters("&")" (!?). Even worse, when we later attempt to re-produce the entity
            // (""Document.createEntityReference("amp")"), we get ""!?.
            @Override public void startEntity(String name) {}
            @Override public void endEntity(String name)   {}

            // We're not (yet) interested in these.
            @Override public void startDTD(String name, String publicId, String systemId) {}
            @Override public void endDTD()                                                {}

            @Override public void startCDATA() { this.elementStack.peek().inCdata = true;  }
            @Override public void endCDATA()   { this.elementStack.peek().inCdata = false; }

            @Override public void
            comment(char[] ch, int start, int length) {
                String s = new String(ch, start, length);

                El el = this.elementStack.peek();
                project.setProperty(el.prefix + el.index++ + ".#", s);
            }
        }

        SAXParser saxParser;
        {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            saxParserFactory.setValidating(false);
            try {
                saxParser = saxParserFactory.newSAXParser();
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
        MyContentAndLexicalHandler contentAndLexicalHandler = new MyContentAndLexicalHandler();

        XMLReader xmlReader = saxParser.getXMLReader();

        xmlReader.setErrorHandler(errorHandler);
        xmlReader.setContentHandler(contentAndLexicalHandler);

        // See "https://docs.oracle.com/javase/tutorial/jaxp/sax/events.html".
        if (lexical) xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", contentAndLexicalHandler);

        InputSource inputSource = new InputSource(is);
        inputSource.setPublicId(publicId);

        xmlReader.parse(inputSource);
    }
}
