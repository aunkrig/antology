
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

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.StringResource;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Returns the <em>names</em> of the elements of the delegate {@link ResourceCollection}.
 *
 * @see ResourceTransformer
 */
public
class ResourceNames extends ProjectComponent implements ResourceCollection, Iterable<Resource> {

    @Nullable private ResourceCollection delegate;

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
    isFilesystemOnly() { return false; }

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

                // Return the resource's name as a StringResource.
                Resource resource = this.iter.next();
                String   name     = resource.getName();
                return new StringResource(ResourceNames.this.getProject(), name);
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
}
