
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2017, Arno Unkrig
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

import java.net.Authenticator;

import javax.swing.JPasswordField;
import javax.swing.text.GapContent;
import javax.swing.text.PlainDocument;

import de.unkrig.commons.lang.AssertionUtil;
import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Utility methods around {@link JPasswordField}s.
 */
public
class JPasswordFields extends Authenticator {

    static { AssertionUtil.enableAssertionsForThisClass(); }

    /**
     * Works around {@link JPasswordField}'s painfully missing "{@code setPassword(char[])}" method.
     * 
     * @see <a href="http://stackoverflow.com/questions/26975275/fill-a-jpasswordfield-programmatically-without-crea
     *ting-a-string-object">This article on STACKOVERFLOW</a>
     */
    public static void
    setPassword(JPasswordField jPasswordField, final char[] password) {
        
        // A GapContent with the "replace()" method made accessible.
        class MyGapContent extends GapContent {
            
            private static final long serialVersionUID = 1L;

            @NotNullByDefault(false) @Override public void
            replace(int position, int rmSize, Object addItems, int addSize) {
                super.replace(position, rmSize, addItems, addSize);
            }
        }
        
        MyGapContent myGapContent = new MyGapContent();
        
        PlainDocument document = new PlainDocument(myGapContent);

        // Must be called AFTER the PlainDocument is created, for otherwise the echo characters don't appear as
        // expected:
        myGapContent.replace(
            0,              // position
            0,              // rmSize
            password,       // addItems
            password.length // addSize
        );
        
        jPasswordField.setDocument(document);
    }
}
