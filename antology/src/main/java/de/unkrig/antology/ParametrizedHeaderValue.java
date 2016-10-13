
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

package de.unkrig.antology;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Parser for a 'parametrized value' of an HTTP header like:
 * <pre>
 * Content-Type: text/plain; charset=ASCII
 * </pre>
 * 'text/plain' is the so-called 'token', 'char=ASCII' is a so-called 'parameter'.
 */
public
class ParametrizedHeaderValue {

    private final String token;

    /** upper-cased-name => value */
    private final Map<String, String> parameters = new HashMap<String, String>();

    public
    ParametrizedHeaderValue(String s) {
        int idx = s.indexOf(';');
        if (idx == -1) {
            this.token = s.trim();
            return;
        }

        this.token = s.substring(0, idx).trim();
        for (;;) {
            int idx2 = s.indexOf('=', idx + 1);
            if (idx2 == -1) break;
            int idx3 = s.indexOf(';', idx2 + 1);
            if (idx3 == -1) idx3 = s.length();
            this.parameters.put(s.substring(idx + 1, idx2).trim().toUpperCase(), s.substring(idx2 + 1, idx3).trim());
            if (idx3 == s.length()) break;
            idx = idx3;
        }
    }

    /** @see ParametrizedHeaderValue */
    public String
    getToken() {
        return this.token;
    }

    /**
     * @param name The (case-insensitive) parameter name
     * @return     The value of the named parameter, or {@code null} iff a paramater with that name does not exist
     * @see        ParametrizedHeaderValue
     */
    @Nullable public String
    getParameter(String name) {
        return this.parameters.get(name.toUpperCase());
    }

    /** @return The parameters of this {@link ParametrizedHeaderValue}, in unspecified order */
    public Collection<Entry<String, String>>
    getParameters() { return Collections.unmodifiableSet(this.parameters.entrySet()); }

    /**
     * Adds the given parameter, or changes the value of an existing parameter with that {@code name}.
     *
     * @return The previous value of the named parameter, or {@code null} iff a parameter with that {@code name} did
     *         not exist before
     */
    @Nullable public String
    setParameter(String name, String value) {
        return this.parameters.put(name.toUpperCase(), value);
    }

    @Override public String
    toString() {
        if (this.parameters.isEmpty()) return this.token;
        StringBuilder sb = new StringBuilder(this.token);
        for (Entry<String, String> entry : this.parameters.entrySet()) {
            sb.append("; ").append(entry.getKey().toLowerCase()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
