
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import de.unkrig.commons.io.IoUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.protocol.ConsumerWhichThrows;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Installs an {@link Authenticator} whichs determines user name and password through its configuration, or prompts the
 * user interactively for user name and password through a SWING {@link JOptionPane}.
 *
 * @see Authenticator#setDefault(Authenticator)
 * @see #addConfiguredCredentials(CredentialsElement)
 */
public
class SetAuthenticatorTask extends Task {
    
    public enum CacheMode { NONE, USER_NAMES, USER_NAMES_AND_PASSWORDS }
    
    public enum StoreMode { NONE, USER_NAMES, USER_NAMES_AND_PASSWORDS }

    /**
     * Stores username-password-pairs, persistently or not.
     */
    private
    interface CredentialsStore {

        /**
         * @return The least recently {@link #put(String, String, char[]) put} <var>userName</var> for the
         *         <var>key</var>
         */
        @Nullable String
        getUserName(String key);
        
        /**
         * @return The least recently {@link #put(String, String, char[]) put} <var>password</var> for the
         *         <var>key</var>
         */
        @Nullable char[]
        getPassword(String key);

        void
        put(String key, String userName, @Nullable char[] password);
    }

    public static final File      CREDENTIALS_STORE_FILE = new File(
        System.getProperty("user.home"),
        ".antology_setAuthenticator_credentials"
    );

    private CacheMode                      cacheMode   = CacheMode.USER_NAMES_AND_PASSWORDS;
    private StoreMode                      storeMode   = StoreMode.NONE;
    private final List<CredentialsElement> credentials = new ArrayList<CredentialsElement>();

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
     *   If no {@link #setUserName(String) user name} and/or no {@link #setPassword(String) password} are configured,
     *   then the user is prompted for the missing user name and/or password.
     * </p>
     */
    public static
    class CredentialsElement extends ProjectComponent {

        @Nullable private Regex  requestingHost;
        @Nullable private Regex  requestingSite;
        @Nullable private Regex  requestingPort;
        @Nullable private Regex  requestingProtocol;
        @Nullable private Regex  requestingPrompt;
        @Nullable private Regex  requestingScheme;
        @Nullable private Regex  requestingUrl;
        @Nullable private Regex  requestorType;
        @Nullable private String userName;
        @Nullable private char[] password;

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

        /**
         * The password to use iff this {@code <credentials>} element matches. Value "{@code -}" is equivalent to
         * <em>not</em> configuring a password.
         */
        public void
        setPassword(String password) {
            if (!password.isEmpty() && !"-".equals(password)) this.password = password.toCharArray();
        }

        // This is used as the key for the "cache" and the "store".
        public String
        key() {
            String sep = "\0";
            return (
                this.requestingHost
                + sep
                + this.requestingSite
                + sep
                + this.requestingPort
                + sep
                + this.requestingProtocol
                + sep
                + this.requestingPrompt
                + sep
                + this.requestingScheme
                + sep
                + this.requestingUrl
                + sep
                + this.requestorType
            );
        }
    }

    class MyAuthenticator extends Authenticator {

        private final List<CredentialsElement>            credentials = new ArrayList<CredentialsElement>();
        private final Map<Object, PasswordAuthentication> cache       = new HashMap<Object, PasswordAuthentication>();

        void
        addCredentials(Collection<CredentialsElement> credentials) { this.credentials.addAll(credentials); }

        @Override @Nullable protected PasswordAuthentication
        getPasswordAuthentication() {

            String userName;
            char[] password;
            String key;
            if (this.credentials.isEmpty()) {

                // Handle the special case with NO "<credentials>" subelement: ALWAYS ask for both user name and
                // password.
                userName = null;
                password = null;
                key = "global";
            } else {

                // Search for the first applicable "<credentials>" subelement.
                COMPUTE_CREDENTIALS: {
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
                            password = ce.password;
                            key      = ce.key();
                            break COMPUTE_CREDENTIALS;
                        }
                    }

                    // No applicable "<credentials>" subelement; give up.
                    return null;
                }
            }

            if (userName == null || password == null) {
                switch (SetAuthenticatorTask.this.cacheMode) {
                
                case NONE:
                    ;
                    
                case USER_NAMES:
                    synchronized (this.cache) {
                        PasswordAuthentication result = this.cache.get(key);
                        if (result != null) userName = result.getUserName();
                    }
                    break;
                    
                case USER_NAMES_AND_PASSWORDS:
                    synchronized (this.cache) {
                        PasswordAuthentication result = this.cache.get(key);
                        if (result != null) return result;
                    }
                    break;
                }
            }
            
            if (userName == null || password == null) {
                switch (SetAuthenticatorTask.this.storeMode) {
                
                case NONE:
                    ;
                    
                case USER_NAMES:
                    userName = this.passwordStore.getUserName(key);
                    break;
                    
                case USER_NAMES_AND_PASSWORDS:
                    userName = this.passwordStore.getUserName(key);
                    password = this.passwordStore.getPassword(key);
                    break;
                }
            }

            if (userName == null || password == null) {

                // The user name and/or the password are missing, so prompt the user for the missing element(s).
                JTextField userNameField = new JTextField();
                if (userName != null) {
                    userNameField.setText(userName);
                    userNameField.setEnabled(false);
                } else {
                    SwingUtil.focussify(userNameField);
                }

                JPasswordField passwordField = new JPasswordField();
                if (password != null) {
                    passwordField.setText(String.valueOf(password));
                    passwordField.setEnabled(false);
                } else {
                    if (userName != null) SwingUtil.focussify(passwordField);
                }

                String message;
                {
                    message = "Authenticating to ";

                    String requestingPrompt = this.getRequestingPrompt();
                    if (requestingPrompt != null) message += "'" + requestingPrompt + "' on ";

                    message += "'" + this.getRequestingHost() + ":" + this.getRequestingPort() + "'";

                    if (this.getRequestorType() == RequestorType.PROXY) message += " proxy";

                    URL requestingURL = this.getRequestingURL();
                    if (requestingURL != null) {
                        message += " / " + SetAuthenticatorTask.truncate(requestingURL.toString(), 100);
                    }

                    message += ":";
                }

                if (JOptionPane.showOptionDialog(
                    null,                                     // parentComponent
                    new Object[] {                            // message
                        new JLabel(message),
                        new JLabel("User ID:"),
                        userNameField,
                        new JLabel("Password:"),
                        passwordField
                    },
                    (                                         // title
                        this.getRequestorType() == RequestorType.PROXY
                        ? "HTTP Proxy Authentication"
                        : "HTTP Authentication"
                    ),
                    JOptionPane.OK_CANCEL_OPTION,             // optionType
                    JOptionPane.PLAIN_MESSAGE,                // messageType
                    null,                                     // icon
                    null,                                     // options
                    null                                      // initialValue
                ) != JOptionPane.OK_OPTION) {
                    throw new BuildException("HTTP authentication password dialog canceled");
                }

                userName = userNameField.getText();
                password = passwordField.getPassword();
            }

            PasswordAuthentication result = new PasswordAuthentication(userName, password);

            if (SetAuthenticatorTask.this.cacheMode != CacheMode.NONE) {
                synchronized (this.cache) {
                    this.cache.put(key, result);
                }
            }
            
            switch (SetAuthenticatorTask.this.storeMode) {
            
            case NONE:
                ;
                break;
                
            case USER_NAMES:
                this.passwordStore.put(key, userName, null);
                break;
                
            case USER_NAMES_AND_PASSWORDS:
                this.passwordStore.put(key, userName, password);
                break;
            }

            return result;
        }

        private final CredentialsStore
        passwordStore = new CredentialsStore() {

            @Nullable Properties properties;
            
            @Override @Nullable public String
            getUserName(String key) { return getProperties().getProperty(key + ".userName"); }

            @Override @Nullable public char[]
            getPassword(String key) {  return toCharArray(getProperties().getProperty(key + ".password")); }

            /**
             * Stores the <var>userName</var> and the <var>password</var> in the credentials store.
             * If the <var>password</var> is {@code null}, then this methods <em>erases</em> the password from the
             * credentials store (but still stores the <var>userName</var>!).
             */
            @Override public synchronized void
            put(String key, String userName, @Nullable char[] password) {
                
                String userNamePropertyName = key + ".userName";
                String passwordPropertyName = key + ".password";
                
                Properties p = getProperties();

                String oldUserName = p.getProperty(userNamePropertyName);
                char[] oldPassword = toCharArray(p.getProperty(passwordPropertyName));
                
                if (
                    userName.equals(oldUserName)
                    && (password == null || Arrays.equals(oldPassword, password))
                ) return;
                
                p.setProperty(userNamePropertyName, userName);
                p.setProperty(passwordPropertyName, toString(password));
                
                storeProperties();
            }

            @Nullable private char[]
            toCharArray(@Nullable String subject) { return subject == null ? null : subject.toCharArray(); }
            
            @Nullable private String
            toString(@Nullable char[] subject) { return subject == null ? null : new String(subject); }

            private synchronized Properties
            getProperties() {
                
                Properties p = this.properties;
                if (p != null) return p;
                
                p = new Properties();
                
                try (InputStream is = new FileInputStream(CREDENTIALS_STORE_FILE)) {
                    p.load(is);
                } catch (FileNotFoundException fnfe) {
                    ;
                } catch (IOException ioe) {
                    throw new BuildException("Loading password store", ioe);
                }
                
                return (this.properties = p);
            }
            
            private synchronized void
            storeProperties() {
                
                final Properties p = this.properties;
                assert p != null;

                try {
                    IoUtil.outputFileOutputStream(
                        CREDENTIALS_STORE_FILE,                                // file
                        new ConsumerWhichThrows<OutputStream, IOException>() { // delegate

                            @Override public void
                            consume(OutputStream os) throws IOException {
                                p.store(os, " The credentials store of the <setAuthenticator> task of http://antology.unkrig.de.");
                            }
                        },
                        false                                                  // createMissingParentDirectories
                    );
                } catch (IOException ioe) {
                    throw new BuildException("Updating the credentials store", ioe);
                }
                
                this.properties = p;
            }
        };

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

        synchronized (SetAuthenticatorTask.class) {
            MyAuthenticator ma = SetAuthenticatorTask.myAuthenticator;
            if (ma == null) {
                Authenticator.setDefault((ma = (SetAuthenticatorTask.myAuthenticator = new MyAuthenticator())));
            }
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
}
