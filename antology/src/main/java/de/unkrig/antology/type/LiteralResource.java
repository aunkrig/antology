
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
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.resources.ImmutableResourceException;
import org.apache.tools.ant.types.resources.StringResource;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A read-only resource with a name and a literal value - configurable either through a {@link #setValue(String)}
 * attribute or {@linkplain #addText(String) nested text}.
 */
public
class LiteralResource extends StringResource {

    @Nullable private String value;

    /**
     * The name of the resource.
     */
    @Override public void
    setName(@Nullable String name) { super.setName(name); }

    /**
     * The value of the resource.
     */
    @Override public void
    setValue(@Nullable String value) {
        if (this.getValue() != null) throw new BuildException(new ImmutableResourceException());
        this.value = value;
    }

    /**
     * Get the value of this StringResource, resolving to the root reference if needed.
     */
    @Override @Nullable public String
    getValue() { return this.value; }

    /** Set the directory attribute; if {@code true}, this resource is a directory. */
    @Override public void setDirectory(boolean directory) { super.setDirectory(directory); }
    /** Set the exists attribute; if {@code true}, this resource exists. */
    @Override public void setExists(boolean exists) { super.setExists(exists); }
    /** Set the last modification attribute, in milliseconds since 01.01.1970. */
    @Override public void setLastModified(long milliseconds) { super.setLastModified(milliseconds); }
    /** Set the size of this Resource. */
    @Override public void setSize(long size) { super.setSize(size); }
    /** The encoding to use when reading from this string. */
    @NotNullByDefault(false) @Override public void setEncoding(String charset) { super.setEncoding(charset); }
    /** (Unclear.) */
    @NotNullByDefault(false) @Override public void setRefid(Reference r) { super.setRefid(r); }
    /** Add nested text to the value of this resource. Properties will be expanded during this process. */
    @NotNullByDefault(false) @Override public void addText(String text) { super.addText(text); }

    @Override public String
    toString() { return this.getName() + "=" + this.getValue(); }
}
