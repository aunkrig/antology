
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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Destroyable;
import javax.swing.JOptionPane;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.net2.FTP2;

import de.unkrig.antology.task.CustomAuthenticator.CacheMode;
import de.unkrig.antology.task.CustomAuthenticator.CredentialsSpec;
import de.unkrig.antology.task.CustomAuthenticator.StoreMode;
import de.unkrig.antology.util.Regex;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Installs an {@link Authenticator} whichs determines user name and password through its configuration, or prompts the
 * user interactively for user name and password through a SWING {@link JOptionPane}.
 * <p>
 *   An "authenticator" is used by all JRE {@link URLConnection}s when a host asks for authentication, e.g. HTTP
 *   "401:Unauthorized" and "407: Proxy Authentication Required". Also the {@link FTP2} task uses the authenticator for
 *   server authentication and proxy authentication.
 * </p>
 * <p>
 *   The exact strategy of this authenticator is as follows:
 * </p>
 * <ul>
 *   <li>
 *     The authentication request is matched against the {@link CredentialsSpec#setRequestingHost(Regex)}, {@link
 *     CredentialsSpec#setRequestingSite(Regex)}, {@link CredentialsSpec#setRequestingPort(Regex)}, {@link
 *     CredentialsSpec#setRequestingProtocol(Regex)}, {@link CredentialsSpec#setRequestingPrompt(Regex)}, {@link
 *     CredentialsSpec#setRequestingScheme(Regex)}, {@link CredentialsSpec#setRequestingURL(Regex)} and {@link
 *     CredentialsSpec#setRequestorType(Regex)} of all {@link CredentialsSpec} subelements,
 *     in the given order. This process stops at the first match.
 *   </li>
 *   <li>
 *     If the matching {@link #addConfiguredCredentials(CustomAuthenticator.CredentialsSpec)} subelement has both
 *     {@link CredentialsSpec#setUserName(String)} <em>and</em> {@link CredentialsSpec#setPassword(char[])}
 *     configured, then that user name-password pair is returned.
 *   </li>
 *   <li>
 *     Otherwise the user is prompted with a {@link JOptionPane} dialog for a user name and a password. (Iff the
 *     matching {@link #addConfiguredCredentials(CustomAuthenticator.CredentialsSpec)} subelement configured a {@link
 *     CredentialsSpec#setUserName(String)}, then that user name is pre-filled in.)<br />
 *     <img width="372" height="342" src="doc-files/setAuthenticator_httpAuthentication.png" />
 *   </li>
 *   <li>
 *     After the user has filled in the missing data, the user name and password are returned.
 *   </li>
 * </ul>
 * <p>
 *   Iff {@link #setCache(CustomAuthenticator.CacheMode)} is set to a value different from {@link CacheMode#NONE}, then
 *   the entered user name and/or password are remembered and pre-filled in the next time the authentication dialog
 *   pops up. The "remembered" data is not persisted and is lost when the JVM terminates.
 * </p>
 * <p>
 *   Iff {@link #setStore(CustomAuthenticator.StoreMode)} is set to a value different from {@link StoreMode#NONE}, then
 *   the entered user name and/or password are stored in a persistent "authentication store". That store is a
 *   properties file in the user's home directory, and the passwords stored therein are encrypted with a secret key,
 *   which is generated ad hoc and stored in another file in the user's home directory (the "key store"). The secret
 *   key is protected by a password (called the "master password"), so that an attacker can not compromise the
 *   passwords in the authentication store, even if he steals the key store file.
 * </p>
 * <p>
 *   When the secret key is created, the user is prompted to choose the master password:
 * </p>
 * <img width="451" height="178" src="doc-files/setAuthenticator_createAuthenticationStore.png" />
 * <p>
 *   When a different JVM instance requires the secret key, it prompts the user to enter the master password:
 * </p>
 * <img width="481" height="191" src="doc-files/setAuthenticator_useAuthenticationStore.png" />
 *
 * @see Authenticator#setDefault(Authenticator)
 * @see #addConfiguredCredentials(CustomAuthenticator.CredentialsSpec)
 */
public
class SetAuthenticatorTask extends Task implements Destroyable {

    // Must be PUBLIC so they are JAVADOC-referencable through "@value". SUPPRESS CHECKSTYLE Javadoc:2
    public static final String DEFAULT_STORE_MODE = "NONE";
    public static final String DEFAULT_CACHE_MODE = "USER_NAMES_AND_PASSWORDS";
    
    @Nullable private String            dialogLabel;
    private CacheMode                   cacheMode   = CacheMode.valueOf(DEFAULT_CACHE_MODE);
    private StoreMode                   storeMode   = StoreMode.valueOf(DEFAULT_STORE_MODE);
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
     * The text of the label in the authentication dialog. If the text starts with "{@code <html>}", then it may
     * contain {@linkplain SwingDialogTask HTML markup}.
     * <p>
     *   The following arguments are replaced within the message:
     * </p>
     * <dl>
     *   <dt>{0}</dt>
     *   <dd>
     *     The "key" to the authentication, which is composed like this:<br />
     *     <var>requestor-type</var>{@code /}<var>requesting-protocol</var>{@code /}<var>requesting-host</var>{@code
     *     /}<var>requesting-port</var>{@code /}<var>requesting-scheme</var>{@code /}</br>
     *     Example value: {@code "PROXY/http/proxy.company.com/8080/Company internet proxy"}
     *   </dd>
     *   
     *   <dt>{1}, {2}</dt>
     *   <dd>The requesting host</dd>
     *   
     *   <dt>{3}, {4}</dt>
     *   <dd>The requesting site</dd>
     *   
     *   <dt>{5}, {6}</dt>
     *   <dd>The requesting port</dd>
     *   
     *   <dt>{7}, {8}</dt>
     *   <dd>The requesting protocol</dd>
     *   
     *   <dt>{9}, {10}</dt>
     *   <dd>The requesting prompt</dd>
     *   
     *   <dt>{11}, {12}</dt>
     *   <dd>The requesting scheme</dd>
     *   
     *   <dt>{13}, {14}</dt>
     *   <dd>The requesting URL</dd>
     *   
     *   <dt>{15}, {16}</dt>
     *   <dd>The requestor type</dd>
     * </dl>
     * <p>
     *   The default value is
     * </p>
     * <blockquote><pre>{@value CustomAuthenticator#DEFAULT_DIALOG_LABEL}</pre></blockquote>
     */
    public void
    setDialogLabel(String value) { this.dialogLabel = value; }

    /**
     * Whether user names, user names and passwords, or none of both are remembered while the JVM is running.
     * This attribute takes effect only on the <em>first</em> execution of this task.
     * 
     * @ant.defaultValue {@value #DEFAULT_CACHE_MODE}
     */
    public void
    setCache(CacheMode value) { this.cacheMode = value; }

    /**
     * Whether user names, user names and passwords, or none of both are persistently stored.
     * This attribute takes effect only on the <em>first</em> execution of this task.
     * 
     * @ant.defaultValue {@value #DEFAULT_STORE_MODE}
     */
    public void
    setStore(StoreMode value) { this.storeMode = value; }

    /**
     * Every time a server requests user name/password authentication, the {@link
     * #addConfiguredCredentials(CustomAuthenticator.CredentialsSpec)} subelements are checked, and the <b>first</b>
     * that matches the request determines the user name and password.
     * <p>
     *   A {@link #addConfiguredCredentials(CustomAuthenticator.CredentialsSpec)} subelement matches iff the requesting
     *   host, site, port, protocol, url, scheme and requestor type all match the respective attributes.
     * </p>
     * <p>
     *   If no {@link CredentialsSpec#setUserName(String)} and/or no {@link
     *   CredentialsSpec#setPassword(char[])} are configured, then the user is prompted for the missing user name
     *   and/or password.
     * </p>
     * <p>
     *   When this task is executed multiply, then the configured {@link
     *   #addConfiguredCredentials(CustomAuthenticator.CredentialsSpec)} <em>add up</em>, i.e. previously configured
     *   credentials are never erased and always take precedence over newly configured ones.
     * </p>
     */
    public void
    addConfiguredCredentials(CredentialsSpec credentials) { this.credentials.add(credentials); }

    @Override public void
    execute() {

        if (this.destroyed) throw new IllegalStateException();

        synchronized (SetAuthenticatorTask.class) {

            // Install the "MyAuthenticator" exactly ONCE.
            CustomAuthenticator ma = SetAuthenticatorTask.myAuthenticator;
            if (ma == null) {
                ma = (SetAuthenticatorTask.myAuthenticator = new CustomAuthenticator(this.cacheMode, this.storeMode));
                if (this.dialogLabel != null) ma.setDialogLabel(this.dialogLabel);
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
