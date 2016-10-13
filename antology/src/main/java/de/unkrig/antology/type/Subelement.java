
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Utility class which declares various static nested classes which are intended to be used as beans for ANT type
 * subelements.
 */
public final
class Subelement {

    private Subelement() {}

    /**
     * An XML element that specifies a "value", either through nested text, or through a {@code value} attribute.
     */
    public static
    class Value extends ProjectComponent {

        /**
         * The (optional) value specified by this XML element.
         */
        @Nullable public String value;

        /**
         * The "value" text - may alternatively be configured by the "{@code value="..."}" attribute.
         */
        public void
        addText(@Nullable String text) {
            if (text == null) return;
            if (this.value != null) {
                throw new BuildException("'value' attribute and element text are mutually exclusive");
            }
            this.value = this.getProject().replaceProperties(text);
        }

        /**
         * The "value" text - may alternatively be configured by text nested between start and end tags.
         */
        public void
        setValue(String value) { this.value = value; }

        @Override public String
        toString() { return "value='" + this.value + "'"; }
    }

    /**
     * An XML element that specifies a name-value pair.
     */
    public static
    class Name_Value extends Value { // SUPPRESS CHECKSTYLE TypeName

        /** The string value configured by the {@code name="..."} attribute. */
        @Nullable public String name;

        /**
         * The "name" of the name-value pair.
         */
        public void
        setName(String name) { this.name = name; }

        @Override public String
        toString() { return "name='" + this.name + "'&" + super.toString(); }
    }
}
