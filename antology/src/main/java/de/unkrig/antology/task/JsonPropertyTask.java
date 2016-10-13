
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import de.unkrig.antology.type.JsonAsProperties;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Sets a number of properties according to the JSON document read from a file, in analogy with the standard
 * <a href="https://ant.apache.org/manual/Tasks/xmlproperty.html">{@code <xmlproperty>}</a> ANT task.
 *
 * <h3>Property setting schema</h3>
 *
 * <h4>Objects</h4>
 *
 * <p>
 *   If the document contains a JSON <i>object</i> ("<code>{ "name1": "value1", "name2": "value2" }</code>"), then
 *   properties are set as follows:
 * </p>
 * <pre>
 *  <var>name</var>.name1 = value1
 *  <var>name</var>.name2 = value2
 * </pre>
 * <p>
 *   <b>Notice:</b> If a JSON object does not only contain string values, then the property names are formed by
 *   applying the rules recursively.
 * </p>
 *
 * <h4>Arrays</h4>
 *
 * <p>
 *   If the document contains a JSON <i>array</i> ("{@code [ "elem0", "elem1" ]}"), then properties are set as follows:
 * </p>
 * <pre>
 *  <var>name</var>.0 = elem0
 *  <var>name</var>.1 = elem1
 * </pre>
 * <p>
 *   <b>Notice:</b> If a JSON array does not only contain string values, then the property names are formed by applying
 *   the rules recursively.
 * </p>
 * <p>
 *   <b>Notice:</b> If the array size is 10 or greater, then the array index parts of the property names are
 *   left-padded with zeros for equal width. E.g. if the array size is 1000, then the property names are
 *   <var>name</var>{@code .000} through <var>name</var>{@code .999}.
 * </p>
 *
 * <h4>Strings, Numbers, Booleans, {@code null}</h4>
 *
 * <p>
 *   If the document contains a string, number, boolean or {@code null} value, then a property is set as follows:
 * </p>
 * <pre>
 *  <var>name</var> = <var>value</var>
 * </pre>
 *
 * <h3>Example</h3>
 *
 * <pre>
 *  {
 *     "name1": "value1",
 *     "name2": null,
 *     "name3": true,
 *     "name4": [
 *        "elem0",
 *        { "name1": "value1", "name2": "value2" },
 *        [ "elem0", "elem1" ]
 *     ],
 *     "name5": 1.0000,   <= Will be scanned as a DOUBLE because in contains at least one of ".eE".
 *     "name6": 0100
 *  }
 * </pre>
 * <p>
 *   results in the following properties being set (assuming that property "name" has value "acme"):
 * </p>
 * <pre>
 *  acme.name1 = value1
 *  acme.name2 = null            <= Notice that the value is indistinguishable from the string value "null".
 *  acme.name3 = true            <= Notice that the value is indistinguishable from the string value "true".
 *  acme.name4.0 = elem0
 *  acme.name4.1.name1 = value1
 *  acme.name4.1.name2 = value2
 *  acme.name4.2.0 = elem0
 *  acme.name4.2.1 = elem1
 *  acme.name5 = 1.0             <= Notice that the DOUBLE was normalized.
 *  acme.name6 = 100             <= Notice that the integer was normalized.
 * </pre>
 */
public
class JsonPropertyTask extends Task {

    @Nullable private File         file;
    private Charset                charset          = Charset.forName("UTF-8");
    private final JsonAsProperties jsonAsProperties = new JsonAsProperties();

    @Override public void
    setProject(@Nullable Project project) {
        this.jsonAsProperties.setProject(project);
        super.setProject(project);
    }

    /**
     * The file to read the JSON document from (mandatory).
     */
    public void
    setFile(File file) { this.file = file; }

    /**
     * The encoding of the {@link #setFile(File)} to read. Defaults to the "platform default encoding".
     */
    public void
    setEncoding(String charsetName) {
        this.charset = Charset.forName(charsetName);
    }

    /**
     * The name prefix of the properties to be set.
     */
    public void
    setName(String namePrefix) { this.jsonAsProperties.setName(namePrefix); }

    @Override public void
    execute() throws BuildException {
        try {

            if (this.file == null) throw new BuildException("Attribute \"file=...\" missing");

            InputStream is = new FileInputStream(this.file);
            try {
                this.jsonAsProperties.execute(new InputStreamReader(is, this.charset));

                is.close();
            } finally {
                try { is.close(); } catch (Exception e) {}
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
