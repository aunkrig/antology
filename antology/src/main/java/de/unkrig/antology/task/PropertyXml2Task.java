
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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

// SUPPRESS CHECKSTYLE LineLength:26
/**
 * Creates an XML document from ANT properties; the counterpart of the {@link XmlProperty2Task &lt;xmlProperty2>} task.
 * <p>
 *   Uses the following properties to create the root element of the XML document:
 * </p>
 * <pre>
 * prefix.0.<var>root-element-name</var>._<var>att1-name</var> = <var>att1-value</var>
 * prefix.0.<var>root-element-name</var>._<var>att2-name</var> = <var>att2-value</var>
 * prefix.0.<var>root-element-name</var>.<var>index</var>.$    = <var>text</var>
 * prefix.0.<var>root-element-name</var>.<var>index</var>.#    = <var>comment</var>
 * prefix.0.<var>root-element-name</var>.<var>index</var>.!    = <var>cdata-text</var>
 * prefix.0.<var>root-element-name</var>.<var>index</var>.?    = <var>processing-instruction-target-and-data</var>
 * </pre>
 * <p>
 *   The <var>index</var>es must be integral values (typically, but not necessarily starting at zero and increasing
 *   with step size 1), and determine the order of the subnodes.
 *   In addition to text, comment, cdata, processing instruction and entity nodes, subelements, sub-subelements and so
 *   forth can be defined in the same manner:
 * </p>
 * <pre>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>._<var>att1-name</var> = <var>att1-value</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>._<var>att2-name</var> = <var>att2-value</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.$    = <var>text</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.#    = <var>comment</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.!    = <var>cdata-text</var>
 * ...<var>parent-element-name</var>.<var>index</var>.<var>subelement-name</var>.<var>index</var>.?    = <var>processing-instruction-target-and-data</var>
 * </pre>
 * <h3>Example:</h3>
 * <p>
 *   The build script
 * </p>
 * <pre>{@literal
 * <property name="prefix.0.project._name"                    value="prj1"          />
 * <property name="prefix.0.project.0.$"                      value="&#10;&#9;"     />
 * <property name="prefix.0.project.1.target._name"           value="trg1"          />
 * <property name="prefix.0.project.1.target.0.$"             value="&#10;&#9;&#9;" />
 * <property name="prefix.0.project.1.target.1.echo._message" value="msg"           />
 * <property name="prefix.0.project.1.target.2.$"             value="&#10;&#9;"     />
 * <property name="prefix.0.project.2.$"                      value="&#10;"         />
 * <propertyXml2 prefix="prefix." />}</pre>
 * <p>
 *   generates this XML document:
 * </p>
 * <pre>{@literal
 * <?xml version='1.0' encoding='UTF-8'?>
 * <project name="prj1">
 *     <target name="trg1">
 *         <echo message="msg" />
 *     </target>
 * </project>}</pre>
 * <p>
 *   Notice that "{@code &#10;}" denotes a line break and "{@code &#9;}" a TAB character.
 *   If you don't care about proper indentation, you can leave out the "{@code ...$}" properties.
 * </p>
 * <p>
 *   This task is the inversion of the {@link XmlProperty2Task} task. Notice that the "{@code ...$$}" properties
 *   which {@link XmlProperty2Task} sets (which are entirely redundant) are <em>not</em> used by this task.
 * </p>
 */
public
class PropertyXml2Task extends Task {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    @Nullable private File file;
    private String         prefix = "";

    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\..*");

    // ==================== BEGIN CONFIGURATION SETTERS ====================

    /**
     * The file to store the XML document in. If this attribute is not set, then the document is printed to the
     * console.
     */
    public void
    setFile(File file) { this.file = file; }

    /**
     * Determines which properties are used; see task description.
     */
    public void
    setPrefix(String prefix) { this.prefix = prefix; }

    // ==================== END CONFIGURATION SETTERS ====================

    @Override public void
    execute() {
        try {
            this.execute2();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void
    execute2() throws ParserConfigurationException, TransformerException {

        Map<String, Object> allProperties = this.getProject().getProperties();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder        documentBuilder        = documentBuilderFactory.newDocumentBuilder();
        Document               document               = documentBuilder.newDocument();

        documentBuilder.setEntityResolver(new EntityResolver() {

            @NotNullByDefault(false) @Override public InputSource
            resolveEntity(String publicId, String systemId) {
                return new InputSource("((" + publicId + "||" + systemId + "))");
            }
        });

        PropertyXml2Task.createSubelements(allProperties, this.prefix, document, document);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer        transformer        = transformerFactory.newTransformer();
        DOMSource          source             = new DOMSource(document);

        StreamResult result = this.file != null ? new StreamResult(this.file) : new StreamResult(System.out);

        transformer.transform(source, result);

    }

    private static Element
    createElement(Map<String, Object> allProperties, String prefix, Document document) {
        document.createElement("company");

        String              elementName = null;
        Map<String, String> attributes  = new HashMap<String, String>();
        for (Entry<String, Object> e : allProperties.entrySet()) {
            String propertyName  = e.getKey();
            Object propertyValue = e.getValue();

            if (!propertyName.startsWith(prefix)) continue;

            int pos;
            {
                pos = propertyName.indexOf('.', prefix.length());
                String en = (
                    pos == -1
                    ? propertyName.substring(prefix.length())
                    : propertyName.substring(prefix.length(), pos)
                );
                if (elementName == null) {
                    elementName = en;
                } else
                if (!en.equals(elementName)) {
                    throw new BuildException(
                        "Property \""
                        + propertyName
                        + "\": Inconsistent element name: \""
                        + elementName
                        + "\" vs. \""
                        + en
                        + "\""
                    );
                }
                pos++;
            }

            String s = propertyName.substring(pos);
            if (s.startsWith("_")) {
                attributes.put(s.substring(1), (String) propertyValue);
            }
        }

        if (elementName == null) {
            throw new BuildException("No valid subelement for property name prefix \"" + prefix + "\"");
        }

        Element element;
        try {
            element = document.createElement(elementName);
        } catch (DOMException de) {
            throw ExceptionUtil.wrap("Element \"" + elementName + "\"", de, RuntimeException.class);
        }

        for (Entry<String, String> att : attributes.entrySet()) {
            String attributeName  = att.getKey();
            String attributeValue = att.getValue();

            element.setAttribute(attributeName, attributeValue);
        }

        PropertyXml2Task.createSubelements(allProperties, prefix + elementName + '.', element, document);

        return element;
    }

    private static void
    createSubelements(Map<String, Object> allProperties, String prefix, Node parent, Document document) {

        Set<Integer> indexes = new HashSet<Integer>();
        for (String propertyName : allProperties.keySet()) {

            if (!propertyName.startsWith(prefix)) continue;

            String  s = propertyName.substring(prefix.length());
            Matcher m;
            if ((m = PropertyXml2Task.PATTERN.matcher(s)).matches()) {
                int idx = Integer.parseInt(m.group(1));
                indexes.add(idx);
            }
        }

        int[] indexes2 = new int[indexes.size()];
        {
            int i = 0;
            for (int index : indexes) indexes2[i++] = index;
        }

        Arrays.sort(indexes2);

        for (int index : indexes2) {
            String prefix2 = prefix + index + '.';

            String text = (String) allProperties.get(prefix2 + '$');
            if (text != null) {
                parent.appendChild(document.createTextNode(text));
                continue;
            }

            String comment = (String) allProperties.get(prefix2 + '#');
            if (comment != null) {
                parent.appendChild(document.createComment(comment));
                continue;
            }

            String cdata = (String) allProperties.get(prefix2 + '!');
            if (cdata != null) {
                parent.appendChild(document.createCDATASection(cdata));
                continue;
            }

            String pi = (String) allProperties.get(prefix2 + '?');
            if (pi != null) {
                int spc = pi.indexOf(' ');
                if (spc == -1) throw new BuildException("Value of property \"" + prefix2 + "?\" lacks a space");
                String target = pi.substring(0,  spc);
                String data   = pi.substring(spc + 1);
                parent.appendChild(document.createProcessingInstruction(target, data));
                continue;
            }

            parent.appendChild(PropertyXml2Task.createElement(allProperties, prefix2, document));
        }
    }
}
