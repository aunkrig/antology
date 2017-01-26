
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

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.security.auth.Destroyable;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;

import de.unkrig.antology.util.Regex;
import de.unkrig.antology.util.SwingUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.crypto.Cryptors;
import de.unkrig.commons.lang.crypto.PasswordAuthenticationStore;
import de.unkrig.commons.lang.crypto.PasswordAuthenticationStores;
import de.unkrig.commons.lang.security.DestroyableString;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Installs an {@link Authenticator} whichs determines user name and password through its configuration, or prompts the
 * user interactively for user name and password through a SWING {@link JOptionPane}.
 *
 * @see Authenticator#setDefault(Authenticator)
 * @see #addConfiguredCredentials(CredentialsElement)
 */
public
class SetAuthenticatorTask extends Task implements Destroyable {
    
    public enum CacheMode { NONE, USER_NAMES, USER_NAMES_AND_PASSWORDS }
    
    public enum StoreMode { NONE, USER_NAMES, USER_NAMES_AND_PASSWORDS }
    
    private static final File
    KEY_STORE_FILE = new File(System.getProperty("user.home"), ".antology_setAuthenticator_keystore");
    
    private static final char[]
    KEY_STORE_PASSWORD = new char[0];
    
    private static final String
    KEY_ALIAS = "setAuthenticatorKey";

    public static final File
    CREDENTIALS_STORE_FILE = new File(System.getProperty("user.home"), ".antology_setAuthenticator_credentials");
    
    private static final char[]
    KEY_PROTECTION_PASSWORD = new char[0];

    private static final String
    CREDENTIALS_STORE_COMMENTS = " The credentials store of the <setAuthenticator> task of http://antology.unkrig.de.";

    private CacheMode                      cacheMode   = CacheMode.USER_NAMES_AND_PASSWORDS;
    private StoreMode                      storeMode   = StoreMode.NONE;
    private final List<CredentialsElement> credentials = new ArrayList<CredentialsElement>();

    private boolean destroyed;

    @Override public void
    destroy() {
        for (CredentialsElement ce : this.credentials) ce.destroy();
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
     *   Iff there are <em>zero</em> "{@code <credentials>}" subelements, then always ask for both the user name and
     *   the password.
     * </p>
     */
    public void
    addConfiguredCredentials(CredentialsElement credentials) {
        this.credentials.add(credentials);
    }

    /**
     * A {@code <credentials>} element matches iff the requesting host, site, port, protocol, url, scheme and/or
     * requestor type match the respective attributes.
     * <p>
     *   If no {@link #setUserName(String) user name} and/or no {@link #setPassword(DestroyableString) password} are
     *   configured, then the user is prompted for the missing user name and/or password.
     * </p>
     */
    public static final
    class CredentialsElement extends ProjectComponent implements Destroyable {

        @Nullable private Regex             requestingHost;
        @Nullable private Regex             requestingSite;
        @Nullable private Regex             requestingPort;
        @Nullable private Regex             requestingProtocol;
        @Nullable private Regex             requestingPrompt;
        @Nullable private Regex             requestingScheme;
        @Nullable private Regex             requestingUrl;
        @Nullable private Regex             requestorType;
        @Nullable private String            userName;
        @Nullable private DestroyableString password;

        private boolean destroyed;

        @Override protected void
        finalize() { this.destroy(); }

        @Override public void
        destroy() {
            if (this.password != null) this.password.destroy();
            this.destroyed = true;
        }

        @Override public boolean
        isDestroyed() { return this.destroyed; }

        /**
         * Hostname of the site or proxy.
         */
        public void
        setRequestingHost(Regex regex) { this.requestingHost = regex; }

        /**
         * {@link InetAddress} of the site.
         */
        public void
        setRequestingSite(Regex regex) { this.requestingSite = regex; }

        /**
         * Port number for the requested connection.
         */
        public void
        setRequestingPort(Regex regex) { this.requestingPort = regex; }

        /**
         * The protocol that's requesting the connection.
         */
        public void
        setRequestingProtocol(Regex regex) { this.requestingProtocol = regex; }

        /**
         * The prompt string given by the requestor (the "realm" for <a href="http://www.ietf.org/rfc/rfc2617.txt">HTTP
         * authentication</a>).
         */
        public void
        setRequestingPrompt(Regex regex) { this.requestingPrompt = regex; }

        /**
         * The scheme of the requestor.
         */
        public void
        setRequestingScheme(Regex regex) { this.requestingScheme = regex; }

        /**
         * The URL that resulted in this request for authentication.
         */
        public void
        setRequestingURL(Regex regex) { this.requestingUrl = regex; }

        /**
         * Whether the requestor is a Proxy or a Server.
         * <dl>
         *   <dt>{@code PROXY}</dt>
         *   <dd>Entity requesting authentication is a HTTP proxy server.</dd>
         *   <dt>{@code SERVER}</dt>
         *   <dd>Entity requesting authentication is a HTTP origin server.</dd>
         * </dl>
         */
        public void
        setRequestorType(Regex regex) { this.requestorType = regex; }

        /**
         * The user name to use iff this {@code <credentials>} element matches. Value "{@code -}" is equivalent to
         * <em>not</em> configuring a user name.
         */
        public void
        setUserName(String userName) {
            if (!userName.isEmpty() && !"-".equals(userName)) this.userName = userName;
        }

        public @Nullable CharSequence
        getPassword() { return this.password; }

        /**
         * The password to use iff this {@code <credentials>} element matches. Value "{@code -}" is equivalent to
         * <em>not</em> configuring a password.
         */
        public void
        setPassword(
            @Nullable DestroyableString password // Callee takes ownership!
        ) {
            if (password != null && (password.length() == 0 || (password.length() == 1 && password.charAt(0) == '-'))) {
                password.destroy();
                password = null;
            } 

            if (this.password != null) this.password.destroy();
            
            this.password = password;
        }

        @Override public int
        hashCode() {
            return (
                ObjectUtil.hashCode(this.requestingHost)
                + ObjectUtil.hashCode(this.requestingSite)
                + ObjectUtil.hashCode(this.requestingPort)
                + ObjectUtil.hashCode(this.requestingProtocol)
                + ObjectUtil.hashCode(this.requestingPrompt)
                + ObjectUtil.hashCode(this.requestingScheme)
                + ObjectUtil.hashCode(this.requestingUrl)
                + ObjectUtil.hashCode(this.requestorType)
                + ObjectUtil.hashCode(this.userName)
                + ObjectUtil.hashCode(this.password)
            );
        }

        @Override public boolean
        equals(@Nullable Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            CredentialsElement that = (CredentialsElement) obj;
            return (
                ObjectUtil.equals(this.requestingHost,        that.requestingHost)
                && ObjectUtil.equals(this.requestingSite,     that.requestingSite)
                && ObjectUtil.equals(this.requestingPort,     that.requestingPort)
                && ObjectUtil.equals(this.requestingProtocol, that.requestingProtocol)
                && ObjectUtil.equals(this.requestingPrompt,   that.requestingPrompt)
                && ObjectUtil.equals(this.requestingScheme,   that.requestingScheme)
                && ObjectUtil.equals(this.requestingUrl,      that.requestingUrl)
                && ObjectUtil.equals(this.requestorType,      that.requestorType)
                && ObjectUtil.equals(this.userName,           that.userName)
                && ObjectUtil.equals(this.password,           that.password)
            );
        }
    }

    class MyAuthenticator extends Authenticator {

        private final List<CredentialsElement>            credentials = new ArrayList<CredentialsElement>();
        private final Map<Object, PasswordAuthentication> cache       = new HashMap<Object, PasswordAuthentication>();

        private final PasswordAuthenticationStore passwordStore;
        
        MyAuthenticator() throws IOException, GeneralSecurityException {

            // Set up an (unencrypted) username/password store.
            PasswordAuthenticationStore pas = PasswordAuthenticationStores.propertiesPasswordAuthenticationStore(
                PasswordAuthenticationStores.propertiesFileSecureProperties(
                    CREDENTIALS_STORE_FILE,
                    CREDENTIALS_STORE_COMMENTS
                )
            );

            // Get a key for password encryption.
            SecretKey secretKey = Cryptors.adHocSecretKey(
                KEY_STORE_FILE,         // keyStoreFile
                KEY_STORE_PASSWORD,     // keyStorePassword
                KEY_ALIAS,              // keyAlias 
                KEY_PROTECTION_PASSWORD // keyProtectionPassword 
            );
            
            // Wrap the username/password store for password encryption.
            this.passwordStore = PasswordAuthenticationStores.encryptPasswords(secretKey, pas);
        }

        public void
        addCredentials(Collection<CredentialsElement> credentials) {
            
            for (CredentialsElement newCe : credentials) {
                if (!this.credentials.contains(newCe)) this.credentials.add(newCe);
            }
        }

        @Override @Nullable protected PasswordAuthentication
        getPasswordAuthentication() {

            // Search for the first applicable "<credentials>" subelement.

            String            userName = null;
            DestroyableString password = null;
            
            for (CredentialsElement ce : this.credentials) {

                if (
                    this.matches(ce.requestingHost,        this.getRequestingHost())
                    && this.matches(ce.requestingSite,     this.getRequestingSite())
                    && this.matches(ce.requestingPort,     this.getRequestingPort())
                    && this.matches(ce.requestingProtocol, this.getRequestingProtocol())
                    && this.matches(ce.requestingPrompt,   this.getRequestingPrompt())
                    && this.matches(ce.requestingScheme,   this.getRequestingScheme())
                    && this.matches(ce.requestingUrl,      this.getRequestingURL())
                    && this.matches(ce.requestorType,      this.getRequestorType())
                ) {
                    userName = ce.userName;
                    if (userName != null) {
                        CharSequence passwordCs = ce.getPassword();
                        if (passwordCs != null) {
                            
                            // The matching <credentials> subelement declares BOTH user name AND password;
                            // return that tuple without any user interaction.
                            return new PasswordAuthentication(userName, toCharArray(passwordCs));
                        }
                    }
                    break;
                }
            }

            // Because the <credentials> subelement did not provide a user name-password pair, we have to go
            // interactive, i.e. check the cache and/or the store, and raise a SWING dialog.

            String key = (
                this.getRequestorType().toString()
                + '/'
                + ObjectUtil.or(this.getRequestingProtocol(), "-")
                + '/'
                + ObjectUtil.or(this.getRequestingHost(), "-")
                + '/'
                + Integer.toString(this.getRequestingPort())
                + '/'
                + ObjectUtil.or(this.getRequestingScheme(), "-")
            );

            // Check the authentication cache.
            switch (SetAuthenticatorTask.this.cacheMode) {
            
            case NONE:
                ;
                break;
                
            case USER_NAMES:
                synchronized (this.cache) {
                    PasswordAuthentication result = this.cache.get(key);
                    if (result != null) userName = result.getUserName();
                }
                break;
                
            case USER_NAMES_AND_PASSWORDS:
                synchronized (this.cache) {
                    PasswordAuthentication result = this.cache.get(key);
                    if (result != null) {
                        userName = result.getUserName();
                        password = new DestroyableString(result.getPassword());
                    }
                }
                break;
            }
            
            // Check the authentication store.
            if (userName == null) {
                userName = this.passwordStore.getUserName(key);
                if (userName != null) {
                    password = this.passwordStore.getPassword(key, userName);
                }
            } else
            if (password == null && userName.equals(this.passwordStore.getUserName(key))) {
                password = this.passwordStore.getPassword(key, userName);
            }

            // Now prompt the user for user name and passwords, and present the values found so far as proposals.
            JTextField userNameField = new JTextField();
            if (userName != null) userNameField.setText(userName);

            JPasswordField passwordField = new JPasswordField();
            if (password != null) passwordField.setText(new String(password.toCharArray()));
            
            SwingUtil.focussify(userName != null && password == null ? passwordField : userNameField);

            String message;
            {
                StringBuilder sb = new StringBuilder();
                
                String requestingPrompt = this.getRequestingPrompt();
                if (requestingPrompt != null) sb.append(requestingPrompt).append(": ");
                
                sb.append(key);
                
                message = sb.toString();
            }

            if (JOptionPane.showOptionDialog(
                null,                                     // parentComponent
                new Object[] {                            // message
                    new JLabel(message),
                    new JLabel("User name:"),
                    userNameField,
                    new JLabel("Password:"),
                    passwordField
                },
                this.getRequestingProtocol().toUpperCase() + ( // title
                    this.getRequestorType() == RequestorType.PROXY
                    ? " Proxy Authentication"
                    : " Authentication"
                ),
                JOptionPane.OK_CANCEL_OPTION,             // optionType
                JOptionPane.PLAIN_MESSAGE,                // messageType
                null,                                     // icon
                null,                                     // options
                null                                      // initialValue
            ) != JOptionPane.OK_OPTION) {
                throw new BuildException("Authentication dialog canceled");
            }

            userName = userNameField.getText();
            if (password != null) password.destroy();
            password = new DestroyableString(passwordField.getPassword());

            PasswordAuthentication result;
            {
                char[] passwordCa = password.toCharArray();
                result = new PasswordAuthentication(userName, passwordCa);
                Arrays.fill(passwordCa, '\0');
            }
            
            switch (SetAuthenticatorTask.this.cacheMode) {
            
            case NONE:
                ;
                break;
              
            case USER_NAMES:
                synchronized (this.cache) {
                    this.cache.put(key, new PasswordAuthentication(userName, null));
                }
                break;
                
            case USER_NAMES_AND_PASSWORDS:
                synchronized (this.cache) {
                    this.cache.put(key, result);
                }
                break;
            }
            
            try {
                
                switch (SetAuthenticatorTask.this.storeMode) {
                
                case NONE:
                    this.passwordStore.remove(key);
                    break;
                    
                case USER_NAMES:
                    this.passwordStore.put(key, userName);
                    break;
                    
                case USER_NAMES_AND_PASSWORDS:
                    this.passwordStore.put(key, userName, password);
                    break;
                }
            } catch (IOException ioe) {
                throw ExceptionUtil.wrap("Saving password store", ioe, IllegalStateException.class);
            }

            password.destroy();
            
            return result;
        }

        private boolean
        matches(@Nullable Regex regex, @Nullable Object subject) {

            boolean result = regex == null || (
                subject != null
                && regex.pattern.matcher(subject.toString()).matches()
            );

            SetAuthenticatorTask.this.log(
                "pattern=" + regex + ", subject=" + subject + ", result=" + result,
                Project.MSG_DEBUG
            );

            return result;
        }
    }

    @Override public void
    execute() {
        
        if (this.destroyed) throw new IllegalStateException();

        synchronized (SetAuthenticatorTask.class) {
            
            // Install the "MyAuthenticator" exactly ONCE.
            MyAuthenticator ma = SetAuthenticatorTask.myAuthenticator;
            if (ma == null) {
                try {
                    Authenticator.setDefault((ma = (SetAuthenticatorTask.myAuthenticator = new MyAuthenticator())));
                } catch (Exception e) {
                    throw ExceptionUtil.wrap("Initializing authenticator", e, BuildException.class);
                }
            }
            
            // Add the configured credentials to the MyAuthenticator.
            ma.addCredentials(this.credentials);
        }
    }

    /**
     * Our singleton authenticator.
     */
    @Nullable private static MyAuthenticator myAuthenticator;

    /**
     * @return {@code s} iff not longer than {@code lengthLimit}, otherwise a truncated version no longer than {@code
     *         lengthLimit}
     */
    protected static String
    truncate(String s, int lengthLimit) {
        return s.length() <= lengthLimit ? s : s.substring(0, lengthLimit - 3) + "...";
    }

    private static char[]
    toCharArray(CharSequence cs) {
        int l = cs.length();
        
        char[] result = new char[l];
        for (int i = 0; i < l; i++) result[i] = cs.charAt(i);
        return result;
    }
}
