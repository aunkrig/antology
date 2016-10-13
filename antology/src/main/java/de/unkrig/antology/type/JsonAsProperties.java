
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

package de.unkrig.antology.type;

import java.io.BufferedReader;
import java.io.Reader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

import de.unkrig.commons.io.CountingReader;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.json.Json;
import de.unkrig.commons.text.json.JsonParser;

/**
 * An ANT element that parses a character string as a JSON document and sets a set of properties accordingly.
 */
public
class JsonAsProperties extends ProjectComponent {

    @Nullable private String name;

    /**
     * The name prefix of the properties to be set.
     */
    public void
    setName(String propertyNamePrefix) { this.name = propertyNamePrefix; }

    /**
     * Parses a JSON document from a {@link Reader} and sets a set of properties, starting with the configured {@link
     * #setName(String)}.
     */
    public void
    execute(Reader r) throws BuildException {

        CountingReader cr;
        {
            BufferedReader br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
            cr = new CountingReader(br);
        }

        Json.Value value;
        try {
            value = new JsonParser(cr).parseValue();
        } catch (Exception pe) {
            throw new BuildException(
                "Line " + cr.lineNumber() + ", column " + cr.columnNumber() + ": " + pe.getMessage(),
                pe
            );
        }

        class SetPropertyVisitor implements Json.ValueVisitor {

            final String propertyName;

            SetPropertyVisitor(String propertyName) { this.propertyName = propertyName; }

            @Override public void
            visit(Json.Null nulL) { this.setProperty("null"); }

            @Override public void
            visit(Json.False falsE) { this.setProperty("false"); }

            @Override public void
            visit(Json.True truE) { this.setProperty("true"); }

            @Override public void
            visit(Json.Array array) {
                String format = "%0" + Integer.toString(array.elements.size() - 1).length() + "d";
                for (int i = 0; i < array.elements.size(); i++) {
                    Json.Value element = array.elements.get(i);
                    element.accept(new SetPropertyVisitor(
                        this.propertyName + '.' + String.format(format, i)
                    ));
                }
            }

            @Override public void
            visit(Json.ObjecT object) {
                for (Json.Member member : object.members) {
                    member.value.accept(new SetPropertyVisitor(this.propertyName + '.' + member.name.text));
                }
            }

            @Override public void
            visit(Json.NumbeR number) {
                this.setProperty(number.value.toString());
            }

            @Override public void
            visit(Json.StrinG string) { this.setProperty(string.text); }

            private void
            setProperty(String value) { JsonAsProperties.this.getProject().setProperty(this.propertyName, value); }
        }

        String name = this.name;
        if (name == null) throw new BuildException("Attribute 'name' missing");

        value.accept(new SetPropertyVisitor(name));
    }
}
