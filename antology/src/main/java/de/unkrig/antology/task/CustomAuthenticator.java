
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

package de.unkrig.antology.task;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.security.auth.Destroyable;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import de.unkrig.antology.util.JPasswordFields;
import de.unkrig.antology.util.Regex;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.lang.ObjectUtil;
import de.unkrig.commons.lang.crypto.PasswordAuthenticationStore;
import de.unkrig.commons.lang.crypto.PasswordAuthenticationStores;
import de.unkrig.commons.lang.crypto.SecretKeys;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * The {@link Authenticator} that is installed by the {@link de.unkrig.antology.task.SetAuthenticatorTask}.
 */
public
class CustomAuthenticator extends Authenticator {

    /**
     * Whether user names, user names and passwords, or none of both are remembered while the JVM is running.
     */
    public enum CacheMode { NONE, USER_NAMES, USER_NAMES_AND_PASSWORDS }

    /**
     * Whether user names, user names and passwords, or none of both are persistently stored.
     */
    public enum StoreMode { NONE, USER_NAMES, USER_NAMES_AND_PASSWORDS }

    /**
     * A pattern that is intended to be matched against the properties of an {@link Authenticator}.
     */
    public static final
    class CredentialsSpec implements Destroyable {

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
        private boolean          deny;

        private boolean destroyed;

        @Override protected void
        finalize() { this.destroy(); }

        @Override public void
        destroy() {
            if (this.password != null) Arrays.fill(this.password, '\0');
            this.destroyed = true;
        }

        @Override public boolean
        isDestroyed() { return this.destroyed; }

        /**
         * Hostname of the site or proxy. {@code null} (the default) means "any host".
         */
        public void
        setRequestingHost(@Nullable Regex regex) { this.requestingHost = regex; }

        /**
         * {@link InetAddress} of the site. {@code null} (the default) means "any site".
         */
        public void
        setRequestingSite(@Nullable Regex regex) { this.requestingSite = regex; }

        /**
         * The pattern to match against the "port number for the requested connection". {@code null} (the default)
         * means "any port".
         */
        public void
        setRequestingPort(@Nullable Regex regex) { this.requestingPort = regex; }

        /**
         * The protocol that's requesting the connection. {@code null} (the default) means "any protocol".
         */
        public void
        setRequestingProtocol(@Nullable Regex regex) { this.requestingProtocol = regex; }

        /**
         * The prompt string given by the requestor (the "realm" for <a href="http://www.ietf.org/rfc/rfc2617.txt">HTTP
         * authentication</a>). {@code null} (the default) means "any prompt string".
         */
        public void
        setRequestingPrompt(@Nullable Regex regex) { this.requestingPrompt = regex; }

        /**
         * The scheme of the requestor. {@code null} (the default) means "any scheme".
         */
        public void
        setRequestingScheme(@Nullable Regex regex) { this.requestingScheme = regex; }

        /**
         * The URL that resulted in this request for authentication. {@code null} (the default) means "any URL".
         */
        public void
        setRequestingURL(@Nullable Regex regex) { this.requestingUrl = regex; }

        /**
         * Whether the requestor is a Proxy or a Server.
         * <dl>
         *   <dt>{@code PROXY}</dt>
         *   <dd>Entity requesting authentication is a HTTP proxy server.</dd>
         *   <dt>{@code SERVER}</dt>
         *   <dd>Entity requesting authentication is a HTTP origin server.</dd>
         * </dl>
         * <p>
         *   {@code null} (the default) means "any requestor type".
         * </p>
         */
        public void
        setRequestorType(@Nullable Regex regex) { this.requestorType = regex; }

        /**
         * The user name to use iff this {@code <credentials>} element matches. Value "{@code -}" is equivalent to
         * <em>not</em> configuring a user name.
         */
        public void
        setUserName(String userName) {
            if (!userName.isEmpty() && !"-".equals(userName)) this.userName = userName;
        }
        
        /**
         * If set to {@code true}, then {@link #setUserName(String)} and {@link #setPassword(char[])} are
         * ignored, and authentication is <em>denied</em> for this spec.
         */
        public void
        setDeny(boolean value) { this.deny = value; }

        /**
         * The password to use iff this {@code <credentials>} element matches. Value "{@code -}" is equivalent to
         * <em>not</em> configuring a password.
         */
        public void
        setPassword(@Nullable char[] password) {

            try {

                if (
                    password == null
                    || password.length == 0
                    || (password.length == 1 && password[0] == '-')
                ) {
                    if (this.password != null) Arrays.fill(this.password, '\0');
                    this.password = null;
                } else {
                    this.password = Arrays.copyOf(password, password.length);
                }
            } finally {
                if (password != null) Arrays.fill(password,  '\0');
            }
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
                + (this.deny ? 1234 : 789312568)
            );
        }

        @Override public boolean
        equals(@Nullable Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            CredentialsSpec that = (CredentialsSpec) obj;
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
                && this.deny == that.deny
            );
        }
    }

    private final CacheMode             cacheMode;
    private final StoreMode             storeMode;
    private final List<CredentialsSpec> credentials = new ArrayList<CredentialsSpec>();

    private MessageFormat dialogLabelMf = new MessageFormat(DEFAULT_DIALOG_LABEL);

    private final Map<String, String> userNameCache = Collections.synchronizedMap(new HashMap<String, String>());
    private final Map<String, char[]> passwordCache = Collections.synchronizedMap(new HashMap<String, char[]>());

    /**
     * This one's initialized lazily by {@link #getPasswordStore()} to avoid unnecessary user interaction, like
     * asking for the "master password".
     */
    @Nullable private PasswordAuthenticationStore passwordStore;

    /**
     * The dialog label to take effect iff <em>no</em> custom label is configured through {@link
     * #setDialogLabel(String)}.
     */
    public static final String DEFAULT_DIALOG_LABEL = (
        ""
        + "<html>\n"
        + "  <table>\n"
        + "    {1,  choice, 0#|1#'<tr><td>Host:    </td><td>'{2}'</td></tr>'}\n"
        + "    {3,  choice, 0#|1#'<tr><td>Site:    </td><td>'{4}'</td></tr>'}\n"
        + "    {5,  choice, 0#|1#'<tr><td>Port:    </td><td>'{6}'</td></tr>'}\n"
        + "    {7,  choice, 0#|1#'<tr><td>Protocol:</td><td>'{8}'</td></tr>'}\n"
        + "    {9,  choice, 0#|1#'<tr><td>Prompt:  </td><td>'{10}'</td></tr>'}\n"
        + "    {11, choice, 0#|1#'<tr><td>Scheme:  </td><td>'{12}'</td></tr>'}\n"
        + "    {13, choice, 0#|1#'<tr><td>URL:     </td><td>'{14}'</td></tr>'}\n"
        + "    {15, choice, 0#|1#'<tr><td>Type:    </td><td>'{16}'</td></tr>'}\n"
        + "  </table>\n"
        + "</html>"
    );

    private static final File
    KEY_STORE_FILE = new File(System.getProperty("user.home"), ".customAuthenticator_keystore");

    private static final char[]
    KEY_STORE_PASSWORD = new char[0];

    private static final String
    KEY_ALIAS = "setAuthenticatorKey";

    private static final File
    CREDENTIALS_STORE_FILE = new File(System.getProperty("user.home"), ".customAuthenticator_credentials");

    private static final String
    CREDENTIALS_STORE_COMMENTS = " The credentials store of the CustomAuthenticator of http://antology.unkrig.de.";

    /**
     * @param cacheMode Whether user names, user names and passwords, or none of both are remembered while the JVM
     *                  is running
     * @param storeMode Whether user names, user names and passwords, or none of both are persistently stored
     */
    public
    CustomAuthenticator(CacheMode cacheMode, StoreMode storeMode) {
        this.cacheMode = cacheMode;
        this.storeMode = storeMode;
    }

    /**
     * The text of the label in the authentication dialog, in {@link MessageFormat} format.
     * <p>
     *   The following arguments are replaced within the message:
     * </p>
     * <dl>
     *   <dt>{0}</dt>
     *   <dd>
     *     The "key" to the authentication, which is composed like this:<br />
     *     <var>requestor-type</var>{@code /}<var>requesting-protocol</var>{@code /}<var>requesting-host</var>{@code
     *     /}<var>requesting-port</var>{@code /}<var>requesting-scheme</var>{@code /}</br>
     *     Example value: {@code "PROXY/http/proxy.company.com/8080/Negotiate"}
     *   </dd>
     *   
     *   <dt>{1}, {2}<sup>*</sup></dt>
     *   <dd>The requesting host; see {@link #getRequestingHost()}</dd>
     *   
     *   <dt>{3}, {4}<sup>*</sup></dt>
     *   <dd>The requesting site; see {@link #getRequestingSite()}</dd>
     *   
     *   <dt>{5}, {6}<sup>*</sup></dt>
     *   <dd>The requesting port; see {@link #getRequestingPort()}</dd>
     *   
     *   <dt>{7}, {8}<sup>*</sup></dt>
     *   <dd>The requesting protocol; see {@link #getRequestingProtocol()}</dd>
     *   
     *   <dt>{9}, {10}<sup>*</sup></dt>
     *   <dd>The requesting prompt; see {@link #getRequestingPrompt()}</dd>
     *   
     *   <dt>{11}, {12}<sup>*</sup></dt>
     *   <dd>The requesting scheme; see {@link #getRequestingScheme()}</dd>
     *   
     *   <dt>{13}, {14}<sup>*</sup></dt>
     *   <dd>The requesting URL; see {@link #getRequestingURL()}</dd>
     *   
     *   <dt>{15}, {16}<sup>*</sup></dt>
     *   <dd>The requestor type; see {@link #getRequestorType()}</dd>
     * </dl>
     * <p>
     *   <sup>*</sup> Where the above list presents <em>two</em> placeholders, they expand as follows:
     * </p>
     * <ul>
     *   <li>
     *     If the value is either {@code null} or {@code ""} or {@code -1}, the two placeholders expand to {@code "0"}
     *     and {@code ""}.
     *   </li>
     *   <li>
     *     Otherwise, the two placeholders expand to {@code "1"} and the value.
     *   </li>
     * </ul>
     * <p>
     *   The default value is
     * </p>
     * <pre>
     * {@value #DEFAULT_DIALOG_LABEL}
     * </pre>
     */
    public void
    setDialogLabel(String message) { this.dialogLabelMf = new MessageFormat(message); }
    
    /**
     * Adds a set of {@link CredentialsSpec}s to this {@link CustomAuthenticator}. Earlier specs take precedence over
     * later specs.
     * <p>
     *   Specs that equal any previously added spec are ignored, because they are irrelevant.
     * </p>
     * <p>
     *   If no specs are added, then the {@link CustomAuthenticator} will prompt the user for user name and/or
     *   password, and optionally cache the entered data (transiently and/or persistently, depending on the
     *   configured cache mode and store mode).
     * </p>
     * 
     * @see #CustomAuthenticator(CacheMode, StoreMode)
     */
    public void
    addCredentials(Collection<CredentialsSpec> credentials) {

        for (CredentialsSpec newCs : credentials) {

            // Avoid adding duplicate credentials specs.
            if (!this.credentials.contains(newCs)) this.credentials.add(newCs);
        }
    }

    @Override @Nullable protected PasswordAuthentication
    getPasswordAuthentication() {

        // Search for the first applicable "<credentials>" subelement.

        String userName = null;

        for (CredentialsSpec cs : this.credentials) {

            if (this.matches(cs)) {
                
                if (cs.deny) return null;
                
                userName = cs.userName;
                if (userName != null) {
                    char[] password = cs.password;
                    if (password != null) {

                        // The matching <credentials> subelement declares BOTH user name AND password;
                        // return that tuple without any user interaction.
                        return new PasswordAuthentication(userName, Arrays.copyOf(password,  password.length));
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

        // Compose the dialog message.
        String message;
        {
            List<Object> args = new ArrayList<>();
            args.add(key);                                                // {0}: key
            CustomAuthenticator.add2(this.getRequestingHost(),     args); // {1}: is... {2} host
            CustomAuthenticator.add2(this.getRequestingSite(),     args); // {3}: is... {4} site
            CustomAuthenticator.add2(this.getRequestingPort(),     args); // {5}: is... {6} port
            CustomAuthenticator.add2(this.getRequestingProtocol(), args); // {7}: is... {8} protocol
            CustomAuthenticator.add2(this.getRequestingPrompt(),   args); // {9}: is... {10} prompt
            CustomAuthenticator.add2(this.getRequestingScheme(),   args); // {11}: is... {12} scheme
            CustomAuthenticator.add2(this.getRequestingURL(),      args); // {13}: is... {14} URL
            CustomAuthenticator.add2(this.getRequestorType(),      args); // {15}: is... {16} type
            
            message = this.dialogLabelMf.format(args.toArray());
        }

        // Compose the dialog title.
        String title = (
            this.getRequestingProtocol().toUpperCase()
            + (this.getRequestorType() == RequestorType.PROXY ? " Proxy Authentication" : " Authentication")
        );

        // Now create the user name and password fields, and initialize them with cached values (if any).
        JTextField     userNameField;
        JPasswordField passwordField;

        // Check the authentication cache.
        boolean hasPassword;
        {
            char[] password = null;
            try {
                
                switch (this.cacheMode) {
        
                case NONE:
                    ;
                    break;
        
                case USER_NAMES:
                    // Prefer the CACHED user name over the CONFIGURED user name.
                    userName = this.userNameCache.get(key);
                    break;
        
                case USER_NAMES_AND_PASSWORDS:
                    // Prefer the CACHED user name and password over the CONFIGURED user name and password.
                    userName = this.userNameCache.get(key);
                    char[] tmp = this.passwordCache.get(key);
                    if (tmp != null) password = tmp.clone();
                    break;
                }
        
                // Check the authentication store.
                switch (this.storeMode) {
        
                case NONE:
                    ;
                    break;
        
                case USER_NAMES:
                    if (userName == null) userName = this.getPasswordStore().getUserName(key);
                    break;
        
                case USER_NAMES_AND_PASSWORDS:
                    if (userName == null) {
                        userName = this.getPasswordStore().getUserName(key);
                        if (userName != null) {
                            if (password != null) Arrays.fill(password, '\0');
                            password = this.getPasswordStore().getPassword(key, userName);
                        }
                    } else
                    if (password == null && userName.equals(this.getPasswordStore().getUserName(key))) {
                        password = this.getPasswordStore().getPassword(key, userName);
                    }
                    break;
                }
    
                // Now prompt the user for user name and passwords, and present the values found so far as proposals.
                userNameField = new JTextField();
                if (userName != null) userNameField.setText(userName);
    
                passwordField = new JPasswordField();
                if (password != null) JPasswordFields.setPassword(passwordField, password);
                
                hasPassword = password != null;
            } finally {
                if (password != null) Arrays.fill(password, '\0');
            }
        }

        // Set the initial focuse to either the user name or the password field.
        CustomAuthenticator.focussify(userName != null && !hasPassword ? passwordField : userNameField);

        if (JOptionPane.showOptionDialog(
            null,                         // parentComponent
            new Object[] {                // message
                new JLabel(message),
                new JLabel("User name:"),
                userNameField,
                new JLabel("Password:"),
                passwordField
            },
            title,                        // title
            JOptionPane.OK_CANCEL_OPTION, // optionType
            JOptionPane.PLAIN_MESSAGE,    // messageType
            null,                         // icon
            null,                         // options
            null                          // initialValue
        ) != JOptionPane.OK_OPTION) return null;

        userName = userNameField.getText();

        char[] password = passwordField.getPassword();
        try {
    
            // Both "userName" and "password" are non-null at this point.
    
            switch (this.cacheMode) {
    
            case NONE:
                ;
                break;
    
            case USER_NAMES:
                this.userNameCache.put(key, userName);
                break;
    
            case USER_NAMES_AND_PASSWORDS:
                this.userNameCache.put(key, userName);
                char[] prev = this.passwordCache.put(key, password.clone());
                if (prev != null) Arrays.fill(prev, '\0');
                break;
            }
    
            try {
    
                switch (this.storeMode) {
    
                case NONE:
                    this.getPasswordStore().remove(key);
                    break;
    
                case USER_NAMES:
                    this.getPasswordStore().put(key, userName);
                    break;
    
                case USER_NAMES_AND_PASSWORDS:
                    this.getPasswordStore().put(key, userName, password.clone()); // <= "put()" clears the char[]
                    break;
                }
            } catch (IOException ioe) {
                throw ExceptionUtil.wrap("Saving password store", ioe, IllegalStateException.class);
            }
    
            return new PasswordAuthentication(userName, password); // <= Constructor clones the password char[]
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * @return Whether this {@link Authenticator} matches the <var>credentialsSpec</var>
     * @see    #matches(Regex, Object)
     */
    private boolean
    matches(CredentialsSpec credentialsSpec) {
        
        return (
            true // SUPPRESS CHECKSTYLE SimplifyBooleanExpression
            && CustomAuthenticator.matches(credentialsSpec.requestingHost,     this.getRequestingHost())
            && CustomAuthenticator.matches(credentialsSpec.requestingSite,     this.getRequestingSite())
            && CustomAuthenticator.matches(credentialsSpec.requestingPort,     this.getRequestingPort())
            && CustomAuthenticator.matches(credentialsSpec.requestingProtocol, this.getRequestingProtocol())
            && CustomAuthenticator.matches(credentialsSpec.requestingPrompt,   this.getRequestingPrompt())
            && CustomAuthenticator.matches(credentialsSpec.requestingScheme,   this.getRequestingScheme())
            && CustomAuthenticator.matches(credentialsSpec.requestingUrl,      this.getRequestingURL())
            && CustomAuthenticator.matches(credentialsSpec.requestorType,      this.getRequestorType())
        );
    }

    /**
     * Adds two elements to the <var>args</var>; either <code>{ 1, arg.toString().trim() }</code> iff <var>arg</var> is
     * neither {@code null}, nor {@code arg.toString().trim().isEmpty()}, nor <var>arg</var> equals -1; otherwise
     * <code>{ 0, "" }</code>.
     */
    private static void
    add2(@Nullable Object arg, List<Object> args) {
        
        if (
            arg != null
            && !Integer.valueOf(-1).equals(arg)
        ) {
            String s = arg.toString().trim();
            if (!s.isEmpty()) {
                args.add(1);
                args.add(s);
                return;
            }
        }
        args.add(0);
        args.add("");
    }

    private PasswordAuthenticationStore
    getPasswordStore() {

        PasswordAuthenticationStore result = this.passwordStore;
        if (result != null) return result;

        try {

            // Get a key for password encryption.
            SecretKey secretKey = SecretKeys.adHocSecretKey(
                CustomAuthenticator.KEY_STORE_FILE,         // keyStoreFile
                CustomAuthenticator.KEY_STORE_PASSWORD,     // keyStorePassword
                CustomAuthenticator.KEY_ALIAS,              // keyAlias
                "Authentication store",                     // dialogTitle
                (                                           // messageCreateKey
                    "Do you want to create an authentication store for user names and passwords?"
                ),
                (                                           // messageUseExistingKey
                    "Do you want to use the existing authentication store for user names and passwords?"
                )
            );
            if (secretKey == null) {
                result = PasswordAuthenticationStore.NOP;
            } else {

                // Set up an (unencrypted) username/password store.
                PasswordAuthenticationStore pas = PasswordAuthenticationStores.propertiesPasswordAuthenticationStore(
                    PasswordAuthenticationStores.propertiesFileDestroyableProperties(
                        CustomAuthenticator.CREDENTIALS_STORE_FILE,
                        CustomAuthenticator.CREDENTIALS_STORE_COMMENTS
                    )
                );

                // Wrap the username/password store for password encryption.
                result = PasswordAuthenticationStores.encryptPasswords(secretKey, pas);
            }
        } catch (Exception e) {
            result = PasswordAuthenticationStore.NOP;
        }

        return (this.passwordStore = result);
    }

    private static void
    focussify(JComponent component) {

        // This is tricky... see
        //    http://tips4java.wordpress.com/2010/03/14/dialog-focus/
        component.addAncestorListener(new AncestorListener() {

            @Override public void
            ancestorAdded(@Nullable AncestorEvent event) {
                assert event != null;
                JComponent component = event.getComponent();
                component.requestFocusInWindow();

                component.removeAncestorListener(this);
            }

            @Override public void ancestorRemoved(@Nullable AncestorEvent event) {}
            @Override public void ancestorMoved(@Nullable AncestorEvent event)   {}
        });
    }

    private static boolean
    matches(@Nullable Regex regex, @Nullable Object subject) {
        return regex == null || (subject != null && regex.pattern.matcher(subject.toString()).matches());
    }
}
