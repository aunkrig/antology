
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.StringResource;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Selects the subset of resources that was added, deleted or modified since the last check.
 */
public
class ResourceCollectionDelta implements ResourceCollection, Iterable<Resource> {

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private File stateFile = new File(System.getProperty("user.home"), ".resource-collection.delta.properties");

    @Nullable private String stateKey;

    private boolean                      added;
    private boolean                      deleted;
    private boolean                      modified;
    @Nullable private ResourceCollection delegate;

    /**
     * The file to store the state information in.
     */
    public void
    setStateFile(File file) { this.stateFile = file; }

    /**
     * Identifies the "state" to check against; use different values if you want to check more than one
     * resource collection (or use different {@link #setStateFile(File)}s).
     */
    public void
    setStateKey(String key) { this.stateKey = key; }

    /**
     * Whether the result includes the resources that were <em>added</em> since the last execution of this task with
     * {@link #setAdded(boolean) setAdded="true"} and the same {@link #setStateFile(File)} and {@link
     * #setStateKey(String)}.
     */
    public void
    setAdded(boolean includeAddedResources) { this.added = includeAddedResources; }

    /**
     * Whether the result includes the resources that were <em>deleted</em> since the last execution of this task with
     * {@link #setDeleted(boolean) setDeleted="true"} and the same {@link #setStateFile(File)} and {@link
     * #setStateKey(String)}.
     */
    public void
    setDeleted(boolean includeDeletedResources) { this.deleted = includeDeletedResources; }

    /**
     * Whether the result includes the resources that were <em>modified</em> since the last execution of this task with
     * {@link #setModified(boolean) setModified="true"} and the same {@link #setStateFile(File)} and {@link
     * #setStateKey(String)}.
     */
    public void
    setModified(boolean includeModifiedResources) { this.modified = includeModifiedResources; }

    /** The collection of resources to check. */
    public void
    addConfigured(ResourceCollection value) {
        if (this.delegate != null) throw new BuildException("No more than one resource collection subelement allowed");
        this.delegate = value;
    }

    @Nullable private Properties state;

    // IMPLEMENTATION OF ResourceCollection

    @Override public boolean
    isFilesystemOnly() {
        final ResourceCollection delegate = this.delegate;
        assert delegate != null;
        return delegate.isFilesystemOnly();
    }

    @Override public Iterator<Resource>
    iterator() {

        final ResourceCollection delegate = this.delegate;
        if (delegate == null) throw new BuildException("Resource collection subelement missing");
        final String stateKey = this.stateKey;
        if (stateKey == null) throw new BuildException("'stateKey=...' attribute missing");

        // Restore the state if it exists.
        Properties state;
        try {
            state = this.state;
            if (state == null) {
                state = new Properties();
                try {
                    final FileInputStream is = new FileInputStream(this.stateFile);
                    try {
                        state.load(is);
                        is.close();
                    } finally {
                        try { is.close(); } catch (final Exception e) {}
                    }
                } catch (final FileNotFoundException fnfe) {
                    ;
                }
            }
        } catch (final IOException ioe) {
            throw new BuildException(ioe);
        }

        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        df.setTimeZone(ResourceCollectionDelta.GMT);

        final Properties newState = new Properties();
        final Resources  result   = new Resources();
        for (final Iterator<Resource> it = delegate.iterator(); it.hasNext();) {
            final Resource subject = it.next();

            String key = stateKey + '@' + subject.getName();

            final String oldValue = state.getProperty(key);

            final String newValue = df.format(subject.getLastModified());

            if (oldValue == null) {
                if (this.added) {
                    newState.setProperty(key, newValue);
                    result.add(new StringResource(subject.getName()));
                }
            } else
            if (newValue.equals(oldValue)) {
                newState.setProperty(key, newValue);
            } else
            {
                if (this.modified) {
                    newState.setProperty(key, newValue);
                    result.add(new StringResource(subject.getName()));
                } else {
                    newState.setProperty(key, oldValue);
                }
            }
        }

        for (final Entry<Object, Object> entry : state.entrySet()) {
            final String key      = (String) entry.getKey();
            final String oldValue = (String) entry.getValue();
            if (!newState.containsKey(key)) {
                if (key.startsWith(stateKey + '@') && this.deleted) {
                    result.add(new StringResource(key.substring(stateKey.length() + 1)));
                } else {
                    newState.setProperty(key, oldValue);
                }
            }
        }

        final Iterator<Resource> tmp = result.iterator();
        return new Iterator<Resource>() {

            @Override public boolean
            hasNext() {

                if (tmp.hasNext()) return true;

                File stateFile    = ResourceCollectionDelta.this.stateFile;
                File oldStateFile = new File(stateFile.getParent(), stateFile.getName() + ",old");
                File newStateFile = new File(stateFile.getParent(), stateFile.getName() + ",new");

                {
                    OutputStream os;
                    try {
                        os = new FileOutputStream(newStateFile);
                    } catch (final FileNotFoundException fnfe) {
                        throw new BuildException(fnfe.getMessage(), fnfe);
                    }

                    try {
                        newState.store(os, null);
                        os.close();
                    } catch (final IOException ioe) {
                        throw new BuildException(ioe.getMessage(), ioe);
                    } finally {
                        try { os.close(); } catch (final Exception e) {}
                    }
                }

                if (stateFile.exists()) {
                    oldStateFile.delete();
                    ResourceCollectionDelta.rename(stateFile, oldStateFile);
                    ResourceCollectionDelta.rename(newStateFile, stateFile);
                    ResourceCollectionDelta.delete(oldStateFile);
                } else {
                    ResourceCollectionDelta.rename(newStateFile, stateFile);
                }

                ResourceCollectionDelta.this.state = newState;

                return false;
            }

            @Override public Resource
            next() { return tmp.next(); }

            @Override public void
            remove() { tmp.remove(); }
        };
    }

    @Override public int
    size() {
        int result = 0;
        for (@SuppressWarnings("unused") Resource unused : this) result++;
        return result;
    }

    private static void
    delete(File file) {
        if (!file.delete()) {
            throw new BuildException("Could not delete '" + file);
        }
    }

    private static void
    rename(File source, File destination) {
        if (!source.renameTo(destination)) {
            throw new BuildException("Could not rename '" + source + "' to '" + destination);
        }
    }
}
