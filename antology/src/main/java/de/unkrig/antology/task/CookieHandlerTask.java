
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2018, Arno Unkrig
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

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A task which performs various actions on the JRE's "cookie handler".
 *
 * @see CookieHandler#setDefault(CookieHandler)
 * @see CookieHandler#get(java.net.URI, java.util.Map)
 */
public
class CookieHandlerTask extends Task {

    public static final
    class PrintElement {

        @Nullable private URI uri;

        public void
        setUri(URI uri) { this.uri = uri; }
    }
    private final List<Runnable> actions = new ArrayList<Runnable>();

    /**
     * Prints all currently stored cookies to STDOUT.
     *
     * @see CookieHandler#get(URI, Map)
     */
    public void
    addConfiguredPrint(PrintElement element) {

        final URI uri = element.uri;
        if (uri == null) throw new BuildException("\"uri=...\" attribute must be set");

        this.actions.add(new Runnable() {

            @Override public void
            run() {

                CookieHandler ch = CookieHandler.getDefault();
                if (ch == null) throw new BuildException("No cookie handler installed");

                Map<String, List<String>> cookieHeaders;
                try {
                    cookieHeaders = ch.get(uri, Collections.<String, List<String>>emptyMap());
                } catch (IOException e) {
                    throw new BuildException(e);
                }

                for (Entry<String, List<String>> e : cookieHeaders.entrySet()) {
                    String       headerName   = e.getKey();
                    List<String> headerValues = e.getValue();
                    System.out.println(headerName + ":");
                    for (String headerValue : headerValues) {
                        System.out.println(headerValue);
                    }
                }
            }
        });
    }

    @Override public void
    execute() throws BuildException {
        for (Runnable a : this.actions) a.run();
    }
}
