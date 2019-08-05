
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2019, Arno Unkrig
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

package de.unkrig.antology.util;

import java.io.Reader;

import org.apache.tools.ant.filters.util.ChainReaderHelper;

public final
class Compat {

    private Compat() {}

    /**
     * Somewhere between ANT 1.10.1 and 1.10.5, the return of method {@link ChainReaderHelper#getAssembledReader()} was
     * changed from {@link Reader} to {@link ChainReaderHelper}{@code .ChainReader}, which causes {@code
     * java.lang.NoSuchMethodError:
     * org.apache.tools.ant.filters.util.ChainReaderHelper.getAssembledReader()Ljava/io/Reader;}. This wrapper method
     * ensures compatibility with both 1.10.1 and 1.10.5.
     */
    public static Reader
    getAssembledReader(ChainReaderHelper crh) {
        try {
            return (Reader) crh.getClass().getDeclaredMethod("getAssembledReader").invoke(crh);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
