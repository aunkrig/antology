
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

import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Destroyable;
import javax.swing.JOptionPane;

import org.apache.tools.ant.Task;

import de.unkrig.antology.task.CustomAuthenticator.CacheMode;
import de.unkrig.antology.task.CustomAuthenticator.CredentialsSpec;
import de.unkrig.antology.task.CustomAuthenticator.StoreMode;
import de.unkrig.commons.lang.security.DestroyableString;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Installs an {@link Authenticator} whichs determines user name and password through its configuration, or prompts the
 * user interactively for user name and password through a SWING {@link JOptionPane}.
 *
 * @see Authenticator#setDefault(Authenticator)
 * @see #addConfiguredCredentials(CredentialsSpec)
 */
public
class SetAuthenticatorTask extends Task implements Destroyable {
    
    private CacheMode                   cacheMode   = CacheMode.USER_NAMES_AND_PASSWORDS;
    private StoreMode                   storeMode   = StoreMode.NONE;
    private final List<CredentialsSpec> credentials = new ArrayList<CredentialsSpec>();

    private boolean destroyed;

    @Override public void
    destroy() {
        for (CredentialsSpec ce : this.credentials) ce.destroy();
        this.destroyed = true;
    }

    @Override public boolean
    isDestroyed() { return this.destroyed; }

    /**
     * @ant.defaultValue USER_NAMES_AND_PASSWORDS
     */
    public void
    setCache(CacheMode value) { this.cacheMode = value; }
    
    /**
     * @ant.defaultValue NONE
     */
    public void
    setStore(StoreMode value) { this.storeMode = value; }
    
    /**
     * Every time a server requests user name/password authentication, the {@code <credentials>} subelements are
     * checked, and the <b>first</b> that matches the request determines the user name and password.
     * <p>
     *   A {@code <credentials>} element matches iff the requesting host, site, port, protocol, url, scheme and/or
     *   requestor type match the respective attributes.
     * </p>
     * <p>
     *   If no {@link #setUserName(String) user name} and/or no {@link #setPassword(DestroyableString) password} are
     *   configured, then the user is prompted for the missing user name and/or password.
     * </p>
     */
    public void
    addConfiguredCredentials(CredentialsSpec credentials) {
        this.credentials.add(credentials);
    }

    @Override public void
    execute() {
        
        if (this.destroyed) throw new IllegalStateException();

        synchronized (SetAuthenticatorTask.class) {
            
            // Install the "MyAuthenticator" exactly ONCE.
            CustomAuthenticator ma = SetAuthenticatorTask.myAuthenticator;
            if (ma == null) {
                ma = SetAuthenticatorTask.myAuthenticator = new CustomAuthenticator(this.cacheMode, this.storeMode);
                Authenticator.setDefault(ma);
            }
            
            // Add the configured credentials to the MyAuthenticator.
            ma.addCredentials(this.credentials);
        }
    }

    /**
     * Our singleton authenticator.
     */
    @Nullable private static CustomAuthenticator myAuthenticator;
}
