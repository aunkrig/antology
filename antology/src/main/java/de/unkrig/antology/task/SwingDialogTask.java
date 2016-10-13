
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
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.antology.task;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit.HTMLFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import de.unkrig.antology.util.SwingUtil;
import de.unkrig.commons.lang.ExceptionUtil;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Pops up a highly configurable dialog and invites the user to fill in form fields.
 *
 * <a name="html_label" />
 * <h4>HTML label texts</h4>
 * <p>
 *   Some dialog components (see the components' descriptions, below) allow for (limited) HTML markup.
 * </p>
 * <p>
 *   <b>The following HTML tags appear to work as expected:</b>
 * </p>
 * <dl>
 *   <dd>{@code <a href="http://www.google.de">A link</a>} (underlined, but not clickable)</dd>
 *   <dd>{@code <a name="myanchor" />} (not useful)</dd>
 *   <dd>{@code <address>An address</address>}</dd>
 *   <dd>{@code <b>Bold text</b>}</dd>
 *   <dd>{@code <big>Bigger text</big>}</dd>
 *   <dd>{@code <blockquote>A block quote</blockquote>}</dd>
 *   <dd>{@code <br />}</dd>
 *   <dd>{@code <center>Centered block</center>}</dd>
 *   <dd>{@code <cite>A citation, italics</cite>}</dd>
 *   <dd>{@code <code>Monospaced code</code>}</dd>
 *   <dd>{@code <dfn>A definition, italics</dfn>}</dd>
 *   <dd>{@code <dir><li>foo.java</li><li>bar.java</li></dir>}</dd>
 *   <dd>{@code <div>A block</div>}</dd>
 *   <dd>{@code <dl><dt>Definition term</dt><dd>Definition description</dd></dl>}</dd>
 *   <dd>{@code <em>Emphasized text</em>}</dd>
 *   <dd>{@code <font color="red" size="17">Alternate font</font>}</dd>
 *   <dd>{@code <form>Input form</form>} (not submittable)</dd>
 *   <dd>{@code <h1>Heading 1</h1>}</dd>
 *   <dd>{@code <h2>Heading 2</h2>}</dd>
 *   <dd>{@code <h3>Heading 3</h3>}</dd>
 *   <dd>{@code <h4>Heading 4</h4>}</dd>
 *   <dd>{@code <h5>Heading 5</h5>}</dd>
 *   <dd>{@code <h6>Heading 6</h6>}</dd>
 *   <dd>{@code <head><base href="xyz" /></head>} (has no effect)</dd>
 *   <dd>{@code <head><basefont color="red" /></head>} (has no effect)</dd>
 *   <dd>{@code <head><meta name="author" content="me" /></head>} (prints as text)</dd>
 *   <dd>{@code <head><noscript>NOSCRIPT</noscript></head>} (prints as text)</dd>
 *   <dd>{@code <head><style>h1 { color:red; }</style></head>} (must be the first tag after "<html>")</dd>
 *   <dd>{@code <hr>Horizontal ruler</hr>}</dd>
 *   <dd>{@code <i>Italic text</i>}</dd>
 *   <dd>{@code <img src="icon.png" />}</dd>
 *   <dd>{@code <input type="text" />}</dd>
 *   <dd>{@code <input type="checkbox" />}</dd>
 *   <dd>{@code <input type="radio" />}</dd>
 *   <dd>{@code <input type="reset" />} (not functional)</dd>
 *   <dd>{@code <kbd>Keyboard input</kbd>}</dd>
 *   <dd>{@code <map><area /></map>} (not useful)</dd>
 *   <dd>{@code <menu><menuitem label="foo" /></menu>} (ignored)</dd>
 *   <dd>{@code <ol><li>Ordered list item</li></ol>}</dd>
 *   <dd>{@code <p>Paragraph</p>}</dd>
 *   <dd>{@code <pre>Preformatted text, monospaced</pre>}</dd>
 *   <dd>{@code <samp>Sample output, monospaced</samp>}</dd>
 *   <dd>{@code <select><option>Selection option</option></select>}</dd>
 *   <dd>{@code <small>Smaller text</small>}</dd>
 *   <dd>{@code <span style="color:red">Grouped inline elements</span>}</dd>
 *   <dd>{@code <strike>Crossed-out text</strike>}</dd>
 *   <dd>{@code <s>Text that is no longer correkt (strikethrough)</s>}</dd>
 *   <dd>{@code <strong>Strong text, bold</strong>}</dd>
 *   <dd>{@code <sub>Subscript text</sub>}</dd>
 *   <dd>{@code <sup>Superscript text</sup>}</dd>
 *   <dd>{@code <table border=1><caption>A caption</caption><tr><th>Heading</th><td>Cell</td></tr></table>}</dd>
 *   <dd>{@code <textarea rows="4">A multi-line text area</textarea>}</dd>
 *   <dd>{@code <tt>Teletype text</tt>}</dd>
 *   <dd>{@code <u>Underlined text</u>}</dd>
 *   <dd>{@code <ul><li>li</li></ul>}</dd>
 *   <dd>{@code <var>A variable, italics</var>}</dd>
 * </p>
 * <p>
 *   <b>The following HTML tags throw exceptions and are therefore not useful:</b>
 * </p>
 * <dl>
 *   <dt>{@code <applet>}</dt>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <frame>}</dt>
 *   <dd>java.lang.RuntimeException: Can't build aframeset, BranchElement(frameset) 226,227</dd>
 *   <dt>{@code <frameset>}</dt>
 *   <dd>java.lang.RuntimeException: Can't build aframeset, BranchElement(frameset) 226,227</dd>
 *   <dt>{@code <head><link rel="stylesheet" type="text/css" href="theme.css" /></head>}</dd>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <head><script>alert('Hi there!');</script></head>}</dd>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <head><title>TITLE</title></head>}</dd>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <input type="submit" />}</dt>
 *   <dd>Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException</dd>
 *   <dt>{@code <link>}</dt>
 *   <dd>javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <noframes>}</dt>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <script>}</dt>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 *   <dt>{@code <title>}</dt>
 *   <dd>java.lang.ClassCastException: javax.swing.JLabel cannot be cast to javax.swing.text.JTextComponent</dd>
 * </dl>
 * <p>
 *   <b>The following HTML tags create unexpected results and are therefore not useful:</b>
 * </p>
 * <dl>
 *   <dt>{@code <body>body</body>}</dt>
 *   <dd>Terminates the document</dd>
 *   <dt>{@code <html>html</html>}</dt>
 *   <dd>Terminates the document</dd>
 *   <dt>{@code <isindex>isindex</isindex>}</dt>
 *   <dd>Breaks the layout</dd>
 *   <dt>{@code <object><param name="x" value="y" /></object>}</dt>
 *   <dd>Displays "??"</dd>
 * </dl>
 *
 * @see HTML#getTag(String)
 * @see HTMLFactory
 */
public
class SwingDialogTask extends Task {

    /**
     * The weird "message" parameter of the {@link JOptionPane#showOptionDialog(java.awt.Component, Object, String,
     * int, int, javax.swing.Icon, Object[], Object) JOptionPane.showOptionDialog()} method.
     *
     * @see JOptionPane The explanation of the <var>message</var> parameter of the {@link
     *                  JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int, javax.swing.Icon,
     *                  Object[], Object)} method
     */
    @Nullable private Object message;

    /**
     * The "title" parameter of the {@link JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int,
     * javax.swing.Icon, Object[], Object) JOptionPane.showOptionDialog()} method.
     */
    private String title = SwingDialogTask.DEFAULT_TITLE;

    public static final String DEFAULT_TITLE = "APACHE ANT"; // SUPPRESS CHECKSTYLE JavadocVariable

    /**
     * The "optionType" parameter of the {@link JOptionPane#showOptionDialog(java.awt.Component, Object, String, int,
     * int, javax.swing.Icon, Object[], Object) JOptionPane.showOptionDialog()} method.
     */
    private OptionType optionType  = OptionType.OK_CANCEL;

    /**
     * The "messageType" parameter of the {@link JOptionPane#showOptionDialog(java.awt.Component, Object, String, int,
     * int, javax.swing.Icon, Object[], Object) JOptionPane.showOptionDialog()} method.
     */
    private MessageType messageType = MessageType.PLAIN;

    @Nullable private String property;

    /**
     * These are run right before {@link JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int,
     * javax.swing.Icon, Object[], Object)} is invoked and typically set default values on the dialog components.
     */
    private final List<Runnable> beforeShow = new ArrayList<Runnable>();

    /**
     * These are run after {@link JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int,
     * javax.swing.Icon, Object[], Object)} has returned and typically set ANT properties from what the user entered
     * in the dialog components.
     */
    private final List<Runnable> retrievers = new ArrayList<Runnable>();

    /**
     * Organizes (radio) button groups; key is the property name.
     */
    private final Map<String, ButtonGroup> buttonGroups = new HashMap<String, ButtonGroup>();

    /**
     * List of tasks that are executed if the user hits the respective button.
     */
    @Nullable private List<Task> ifYesTasks, ifNoTasks, ifCancelTasks, ifOkTasks, ifClosedTasks;

    // ATTRIBUTE SETTERS

    /**
     * The text to be displayed in the title bar of the dialog.
     *
     * @ant.defaultValue {@value #DEFAULT_TITLE}
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Which buttons to display in the dialog. ("{@link OptionType#DEFAULT}" means a single {@link
     * ShowDialogReturnType#OK} button.)
     * <p>
     *   <img src="doc-files/swingDialog_optionType_DEFAULT.png"       />
     *   <img src="doc-files/swingDialog_optionType_YES_NO.png"        />
     *   <img src="doc-files/swingDialog_optionType_YES_NO_CANCEL.png" />
     *   <img src="doc-files/swingDialog_optionType_OK_CANCEL.png"     />
     * </p>
     *
     * @ant.defaultValue OK_CANCEL
     */
    public void setOptionType(OptionType optionType) { this.optionType = optionType; }

    /**
     * If not set to {@link MessageType#PLAIN}, then a respective icon is displayed in the dialog.
     * <p>
     *   <img src="doc-files/swingDialog_messageType_ERROR.png"       />
     *   <img src="doc-files/swingDialog_messageType_INFORMATION.png" />
     *   <img src="doc-files/swingDialog_messageType_WARNING.png"     />
     *   <img src="doc-files/swingDialog_messageType_QUESTION.png"    />
     *   <img src="doc-files/swingDialog_messageType_PLAIN.png"       />
     * </p>
     *
     * @ant.defaultValue PLAIN
     */
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    /**
     * If configured, the the named property will be set to "{@link ShowDialogReturnType#YES}", "{@link
     * ShowDialogReturnType#NO}", "{@link ShowDialogReturnType#CANCEL}", "{@link ShowDialogReturnType#OK}" or "{@link
     * ShowDialogReturnType#CLOSED}" when the task completes.
     */
    public void setProperty(String propertyName) { this.property = propertyName; }

    /**
     * Each line of the text is mapped to a {@link JLabel}, as if there was a {@link #addConfiguredLabel(Label)}
     * subelement.
     * <p>
     *   Due to the way that ANT handles literal text, all these labels appear <em>above</em> the other GUI elements
     *   configured through subelements, even if the text appears <em>below</em> the subelement. That is,
     * </p>
     * <pre>{@code <swingDialog>ONE<label>TWO</label>THREE</swingDialog>}</pre>
     * <p>
     *   will appear as
     * </p>
     * <p>
     *   <img src="doc-files/swingDialog_tag_text.png" />
     * </p>
     */
    public void
    addText(String text) {

        // Is invoked by ANT to process any character data within the element. "text" is the concatenation of the
        // text between the element's staring tag and the first subelement, plus the texts between subelement, plus
        // the text after the last subelement and the end tag.

        text = text.trim();
        if (text.isEmpty()) return;

        text = this.getProject().replaceProperties(text);

        this.addToMessage(text);
    }

    // SUBELEMENT ADDERS

    /**
     * Adds a {@link JLabel} to the dialog.
     * <p>
     *   <img src="doc-files/swingDialog_label.png" />
     * </p>
     */
    public void addConfiguredLabel(Label label) { label.adddTo(this); }

    /**
     * @deprecated Use {@link #addConfiguredTextField(TextField)} instead.
     */
    @Deprecated public void addConfiguredText(TextField textField) { this.addConfiguredTextField(textField); }

    /**
     * Adds a {@link JTextField} to the dialog.
     * <p>
     *   <img src="doc-files/swingDialog_textField.png" />
     * </p>
     */
    public void addConfiguredTextField(TextField textField) { textField.adddTo(this); }

    /**
     * Adds a {@link JTextArea} to the dialog.
     * <p>
     *   <img src="doc-files/swingDialog_textArea.png" />
     * </p>
     */
    public void addConfiguredTextArea(TextArea textArea) { textArea.adddTo(this); }

    /**
     * Adds a {@link JCheckBox} to the dialog.
     */
    public void addConfiguredCheckbox(Checkbox checkbox) { checkbox.adddTo(this); }

    /**
     * Adds a {@link JRadioButton} to the dialog.
     */
    public void
    addConfiguredRadioButton(RadioButton radioButton) { radioButton.adddTo(this); }

    /**
     * Adds a {@link JList} to the dialog.
     */
    public void addConfiguredList(LisT list) { list.adddTo(this); }

    /**
     * Adds a {@link JSeparator} to the dialog.
     */
    public void addConfiguredSeparator(Separator separator) { separator.adddTo(this); }

    /**
     * Configures the tasks to be executed when the user hits the {@link ShowDialogReturnType#YES} button.
     */
    public void
    addConfiguredIfYes(Element__Tasks subelement) {
        if (this.ifYesTasks != null) throw new BuildException("At most one '<ifYes>' subelement must be added");
        this.ifYesTasks = subelement.tasks;
    }

    /**
     * Configures the tasks to be executed when the user hits the {@link ShowDialogReturnType#NO} button.
     * <p>
     *   If none of {@link #addConfiguredIfYes(Element__Tasks)} and {@link #addConfiguredIfNo(Element__Tasks)} are
     *   configured and the user hits the {@link ShowDialogReturnType#NO} button, then the task fails.
     * </p>
     */
    public void
    addConfiguredIfNo(Element__Tasks subelement) {
        if (this.ifNoTasks != null) throw new BuildException("At most one '<ifNo>' subelement must be added");
        this.ifNoTasks = subelement.tasks;
    }

    /**
     * Configures the tasks to be executed when the user hits the {@link ShowDialogReturnType#CANCEL} button.
     * <p>
     *   If this property is not configured configured and the user hits "CANCEL", then the task fails.
     * </p>
     */
    public void
    addConfiguredIfCancel(Element__Tasks subelement) {
        if (this.ifCancelTasks != null) throw new BuildException("At most one '<ifCancel>' subelement must be added");
        this.ifCancelTasks = subelement.tasks;
    }

    /**
     * Configures the tasks to be executed when the user hits the {@link ShowDialogReturnType#OK} button.
     */
    public void
    addConfiguredIfOk(Element__Tasks subelement) {
        if (this.ifOkTasks != null) throw new BuildException("At most one '<ifOk>' subelement must be added");
        this.ifOkTasks = subelement.tasks;
    }

    /**
     * Configures the tasks to be executed when the user closes the dialog (instead of hitting one of the buttons).
     * <p>
     *   If this property is not configured configured and the user closes the dialog, then the task fails.
     * </p>
     */
    public void
    addConfiguredIfClosed(Element__Tasks subelement) {
        if (this.ifClosedTasks != null) throw new BuildException("At most one '<ifClosed>' subelement must be added");
        this.ifClosedTasks = subelement.tasks;
    }

    // END SUBELEMENT ADDERS

    /**
     * An object that can add itself to a {@link SwingDialogTask}.
     *
     * @see #adddTo(SwingDialogTask)
     */
    public
    interface SwingDialogTaskAddable {

        /**
         * Adds itself to the given {@link SwingDialogTask}, by adding a {@link JComponent} to the {@link
         * SwingDialogTask#message} and/or runnables to {@link SwingDialogTask#beforeShow} and {@link
         * SwingDialogTask#retrievers}.
         * <p>
         *   Notice: The name of this method hs THREE Ds in order to avoid that the ANT misinterprets it as a property
         *   setter method.
         * </p>
         */
        void adddTo(SwingDialogTask swingDialogTask);
    }

    /***/
    public static
    class Label implements SwingDialogTaskAddable {

        @Nullable private String text;

        /**
         * The text to appear on the label (optional). May contain HTML markup iff it starts with "{@code <html>}";
         * see <a href="#html_label">here</a>.
         *
         * @see #addText(String)
         */
        public void
        setText(String text) { this.text = text; }

        /**
         * The text to appear on the label (optional). May contain HTML markup iff it starts with "{@code <html>}";
         * see <a href="#html_label">here</a>.
         */
        public void
        addText(String text) {
            text = text.trim();
            if (text.isEmpty()) return;

            this.text = text;
        }

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) {
            final JLabel jLabel = new JLabel();

            swingDialogTask.beforeShow.add(new Runnable() {

                @Override public void
                run() { jLabel.setText(swingDialogTask.getProject().replaceProperties(Label.this.text));  }
            });

            swingDialogTask.addToMessage(jLabel);
        }
    }

    /**
     * Abstract base class for {@link TextArea} and {@link TextField}.
     */
    public abstract static
    class TextComponent implements SwingDialogTaskAddable {

        // SUPPRESS CHECKSTYLE JavadocVariable:4
        @Nullable String property;
        @Nullable String defaultValue;
        boolean          focus;
        boolean          enabled = true;

        /**
         * Copies the text to the named property when the dialog is completed (optional).
         */
        public void setProperty(String propertyName) { this.property = propertyName; }

        /**
         * Initializes the component with the <var>text</var> (optional).
         */
        public void setDefaultValue(String text) { this.defaultValue = text; }

        /**
         * Whether this component should initially receive focus when the dialog opens.
         */
        public void setFocus(boolean value) { this.focus = value; }

        /**
         * If {@code false}, then the control is grayed out and is not editable.
         *
         * @ant.defaultValue true
         */
        public void setEnabled(boolean value) { this.enabled = value; }
    }

    /***/
    public static
    class TextField extends TextComponent {

        private boolean          secure;
        @Nullable private String label;
        private int              labelWidth;

        /**
         * Whether the text in the {@link JTextField} is not echoed.
         */
        public void setSecure(boolean value) { this.secure = value; }

        /**
         * The (optional) text to display left from the text field.
         */
        public void setLabel(String text) { this.label = text; }

        /**
         * The (optional) width of the label; useful if you have multiple text fields with labels and want to have
         * them vertically aligned.
         *
         * @see #setLabel(String)
         */
        public void setLabelWidth(int pixels) { this.labelWidth = pixels; }

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) {
            final JTextField jTextField = this.secure ? new JPasswordField() : new JTextField();
            jTextField.setEnabled(this.enabled);
            if (this.focus) SwingUtil.focussify(jTextField);

            JComponent jComponent;
            if (this.label != null) {

                BorderLayout bl = new BorderLayout();
                bl.setHgap(10);
                jComponent = new JPanel(bl);

                {
                    JLabel l = new JLabel(this.label);
                    if (this.labelWidth != 0) {
                        l.setPreferredSize(new Dimension(this.labelWidth, 0));
                    }
                    jComponent.add(l, BorderLayout.WEST);
                }

                jComponent.add(jTextField, BorderLayout.CENTER);
            } else {

                jComponent = jTextField;
            }

            swingDialogTask.beforeShow.add(new Runnable() {

                @Override public void
                run() { jTextField.setText(TextField.this.defaultValue); }
            });

            swingDialogTask.retrievers.add(new Runnable() {

                @Override public void
                run() { swingDialogTask.getProject().setProperty(TextField.this.property, jTextField.getText()); }
            });

            swingDialogTask.addToMessage(jComponent);
        }
    }

    /**
     * <p>
     *   The following attributes are mutually exclusive:
     * </p>
     * <dl>
     *   <dd>{@link #setDefaultValue(String)}</dd>
     *   <dd>{@link #setDefaultFile(File)}</dd>
     * </dl>
     */
    public static
    class TextArea extends TextComponent {

        @Nullable private File    file;
        private boolean           append;
        @Nullable private File    defaultFile;
        private int               rows;
        private int               columns;
        @Nullable private Charset encoding;

        /**
         * Copy the text of the {@link JTextArea} to the designated file when the dialog is completed (optional).
         */
        public void setFile(File file) { this.file = file; }

        /**
         * Whether to append to an existing {@code file=...} (instead of recreating it).
         */
        public void setAppend(boolean value) { this.append = value; }

        /**
         * Initialize the text of the {@link JTextArea} with the content of the <var>default-file</var> (optional).
         */
        public void setDefaultFile(File defaultFile) { this.defaultFile = defaultFile; }

        /**
         * Set the height of the {@link JTextArea} to the designated number of rows.
         */
        public void setRows(int n) { this.rows = n; }

        /**
         * Set the width of the {@link JTextArea} to the designated number of columns.
         */
        public void setColumns(int n) { this.columns = n; }

        /**
         * The encoding (charset) for the {@code file="..."} and the {@code defaultFile="..."}.
         */
        public void setEncoding(String charset) { this.encoding = Charset.forName(charset); }

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) {

            final JTextArea jTextArea = new JTextArea(this.rows, this.columns);

            jTextArea.setEnabled(this.enabled);

            if (this.focus) SwingUtil.focussify(jTextArea);

            JScrollPane jScrollPane = new JScrollPane(jTextArea);
            jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

            if (this.defaultValue != null) {
                if (this.defaultFile != null) {
                    throw new BuildException("'defaultValue=\"...\"' and 'defaultFile=\"...\"' are mutually exclusive");
                }
                swingDialogTask.beforeShow.add(new Runnable() {

                    @Override public void
                    run() { jTextArea.setText(TextArea.this.defaultValue); }
                });
            } else
            if (this.defaultFile != null) {
                swingDialogTask.beforeShow.add(new Runnable() {

                    @Override public void
                    run() {
                        try {
                            InputStream is = new FileInputStream(TextArea.this.defaultFile);
                            try {
                                jTextArea.read(new InputStreamReader(is, TextArea.this.encoding), null);
                                is.close();
                            } finally {
                                try { is.close(); } catch (Exception e) {}
                            }
                        } catch (IOException ioe) {
                            throw ExceptionUtil.wrap(
                                "Reading default file '" + TextArea.this.defaultFile + "'",
                                ioe,
                                BuildException.class
                            );
                        }
                    }
                });
            }
            swingDialogTask.retrievers.add(new Runnable() {

                @Override public void
                run() {

                    if (TextArea.this.property != null) {
                        swingDialogTask.getProject().setProperty(TextArea.this.property, jTextArea.getText());
                    }

                    if (TextArea.this.file != null) {
                        try {
                            OutputStream os = new FileOutputStream(TextArea.this.file, TextArea.this.append);
                            try {
                                jTextArea.write(new OutputStreamWriter(os, TextArea.this.encoding));
                                os.close();
                            } finally {
                                try { os.close(); } catch (Exception e) {}
                            }
                        } catch (IOException ioe) {
                            throw ExceptionUtil.wrap(
                                "Writing  file '" + TextArea.this.file + "'",
                                ioe,
                                BuildException.class
                            );
                        }
                    }
                }
            });
            swingDialogTask.addToMessage(jScrollPane);
        }
    }

    /***/
    public static
    class Checkbox implements SwingDialogTaskAddable {

        @Nullable private String text;
        @Nullable private String property;
        private boolean          preselected;
        private boolean          focus;
        private boolean          enabled = true;

        /**
         * The label text to appear next to the {@link JCheckBox} (optional). May contain HTML markup iff it starts
         * with "{@code <html>}"; see <a href="#html_label">here</a>.
         */
        public void setText(String text) { this.text = text; }

        /**
         * The label text to appear next to the {@link JCheckBox} (optional). May contain HTML markup iff it starts
         * with "{@code <html>}"; see <a href="#html_label">here</a>.
         */
        public void
        addText(String text) {
            text = text.trim();
            if (text.isEmpty()) return;

            this.text = text;
        }

        /**
         * Set the named property to either "{@code true}" or "{@code false}" when the dialog is completed (optional).
         */
        public void setProperty(String propertyName) { this.property = propertyName; }

        /**
         * Whether the {@link JCheckBox} is intially checked or not.
         */
        public void setPreselected(boolean value) { this.preselected = value; }

        /**
         * Whether this {@link JCheckBox} should initially receive focus when the dialog opens.
         */
        public void setFocus(boolean value) { this.focus = value; }

        /**
         * If {@code false}, then the control is grayed out and is not modifiable.
         *
         * @ant.defaultValue true
         */
        public void setEnabled(boolean value) { this.enabled = value; }

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) {

            final JCheckBox jCheckBox = new JCheckBox();

            if (this.focus) SwingUtil.focussify(jCheckBox);

            jCheckBox.setEnabled(this.enabled);

            swingDialogTask.beforeShow.add(new Runnable() {

                @Override public void
                run() {
                    jCheckBox.setText(swingDialogTask.getProject().replaceProperties(Checkbox.this.text));
                    jCheckBox.setSelected(Checkbox.this.preselected); }
            });

            swingDialogTask.retrievers.add(new Runnable() {

                @Override public void
                run() {
                    swingDialogTask.getProject().setProperty(
                        Checkbox.this.property,
                        Boolean.toString(jCheckBox.isSelected())
                    );
                }
            });

            swingDialogTask.addToMessage(jCheckBox);
        }
    }

    /***/
    public static
    class RadioButton implements SwingDialogTaskAddable {

        @Nullable private String text;
        @Nullable private String value;
        @Nullable private String property;
        private boolean          preselected;
        private boolean          focus;
        private boolean          enabled = true;

        /**
         * The label text to appear next to the {@link JRadioButton} (optional). May contain HTML markup iff it starts
         * with "{@code <html>}"; see <a href="#html_label">here</a>.
         */
        public void setText(String text) { this.text = text; }

        /**
         * The label text to appear next to the {@link JRadioButton} (optional). May contain HTML markup iff it starts
         * with "{@code <html>}"; see <a href="#html_label">here</a>.
         */
        public void
        addText(String text) {
            text = text.trim();
            if (text.isEmpty()) return;

            this.text = text;
        }

        /**
         * Set the named property to the given {@code value="..."} or {@code text="..."} when the dialog is completed
         * (mandatory).
         */
        public void setProperty(String propertyName) { this.property = propertyName; }

        /**
         * The string to store in the named property when the dialog is completed.
         * If this attribute is not configured, then the radio button text is used instead.
         */
        public void setValue(String value) { this.value = value; }

        /**
         * Whether this {@link JRadioButton} is intially selected or not.
         */
        public void setPreselected(boolean value) { this.preselected = value; }

        /**
         * Whether this {@link JRadioButton} should initially receive focus when the dialog opens.
         */
        public void setFocus(boolean value) { this.focus = value; }

        /**
         * If {@code false}, then the control is grayed out and neither selectable nor deselectable.
         *
         * @ant.defaultValue true
         */
        public void setEnabled(boolean value) { this.enabled = value; }

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) {

            String property = RadioButton.this.property;
            assert property != null;

            final JRadioButton jRadioButton = new JRadioButton();

            if (this.focus) SwingUtil.focussify(jRadioButton);

            jRadioButton.setEnabled(this.enabled);

            {
                ButtonGroup bg = swingDialogTask.buttonGroups.get(property);
                if (bg == null) swingDialogTask.buttonGroups.put(property, (bg = new ButtonGroup()));

                bg.add(jRadioButton);
            }

            // Preselect the radio button before the dialog is shown.
            swingDialogTask.beforeShow.add(new Runnable() {

                @Override public void
                run() {
                    jRadioButton.setText(swingDialogTask.getProject().replaceProperties(RadioButton.this.text));
                    jRadioButton.setSelected(RadioButton.this.preselected);
                }
            });

            // Set the ANT property after the dialog has been closed.
            swingDialogTask.retrievers.add(new Runnable() {

                @Override public void
                run() {
                    if (jRadioButton.isSelected()) {
                        swingDialogTask.getProject().setProperty(
                            RadioButton.this.property,
                            RadioButton.this.value != null ? RadioButton.this.value : RadioButton.this.text
                        );
                    }
                }
            });

            swingDialogTask.addToMessage(jRadioButton);
        }
    }

    /**
     * <p>
     *   The following attributes are mutually exclusive:
     * </p>
     * <dl>
     *   <dd>{@code defaultValues="..."}</dd>
     *   <dd>{@code defaultIndices="..."}</dd>
     * </dl>
     */
    public static
    class LisT implements SwingDialogTaskAddable {

        @Nullable private SelectionMode selectionMode;
        @Nullable private String        values;
        @Nullable private String        defaultValues;
        @Nullable private String        labels;
        @Nullable private String        defaultIndices;
        private String                  delimiters = ",";
        @Nullable private String        property;
        private boolean                 focus;
        private boolean                 enabled         = true;
        private int                     visibleRowCount = -1;

        /**
         * How entries can be selected by the user:
         * <dl>
         *   <dt>{@link SelectionMode#SINGLE}</dt>
         *   <dd>At most one item can be selected at a time.</dd>
         *   <dt>{@link SelectionMode#SINGLE_INTERVAL}</dt>
         *   <dd>Only a contiguous interval of items can be selected.</dd>
         *   <dt>{@link SelectionMode#MULTIPLE_INTERVAL}</dt>
         *   <dd>Any number of items (including 0) can be selected.</dd>
         * </dl>
         *
         * @ant.defaultValue MULTIPLE_INTERVAL
         * @see          JList#setSelectionMode(int)
         */
        public void setSelectionMode(SelectionMode value) { this.selectionMode = value; }

        /**
         * Set the named property to the delimiter-separated list of selected item values when the dialog is completed
         * (mandatory).
         */
        public void setProperty(String propertyName) { this.property = propertyName; }

        /**
         * The delimiter-separated list of entry values (mandatory).
         */
        public void setValues(String values) { this.values = values; }

        /**
         * If given, then the delimiter-separated <var>labels</var> are displayed in the list instead of the entries'
         * <i>values</i> (optional).
         */
        public void setLabels(String labels) { this.labels = labels; }

        /**
         * The delimiter-separated list of entry values that are initially selected when the dialog opens (optional).
         */
        public void setDefaultValues(String values) { this.defaultValues = values; }

        /**
         * The delimiter-separated list of entry <i>indexes</i> (counting from 0) that are initially selected when the
         * dialog opens (optional).
         */
        public void setDefaultIndices(String indexes) { this.defaultIndices = indexes; }

        /**
         * The delimiter characters for the delimiter-separated lists in the other attributes.
         *
         * @ant.defaultValue ,
         */
        public void setDelimiters(String delimiters) { this.delimiters = delimiters; }

        /**
         * Whether this {@link JList} should initially receive focus when the dialog opens.
         */
        public void setFocus(boolean value) { this.focus = value; }

        /**
         * If {@code false}, then the control is grayed out and its items neither selectable nor deselectable.
         *
         * @ant.defaultValue true
         */
        public void setEnabled(boolean value) { this.enabled = value; }

        /**
         * Fixes the height of the list; if more values are configured, then a vertical scroll bar appears.
         * <p>
         *   The default is to adapt the list height to the number of values and not display a scroll bar.
         * </p>
         * @param n
         */
        public void setVisibleRowCount(int n) { this.visibleRowCount = n; }

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) {

            // values=..., labels=..., delimiters=...
            final String[] values = SwingDialogTask.tokenize(this.values, this.delimiters);
            final String[] labels = SwingDialogTask.tokenize(this.labels, this.delimiters);

            Object[] listData = new Object[values.length];
            for (int i = 0; i < listData.length; i++) {
                listData[i] = i < labels.length ? labels[i] : values[i];
            }
            final JList jList = new JList(listData);
            jList.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));

            // focus=...
            if (this.focus) SwingUtil.focussify(jList);

            // enabled=...
            jList.setEnabled(this.enabled);

            // selectionMode=...
            if (this.selectionMode != null) jList.setSelectionMode(this.selectionMode.intValue());

            final int[] defaultIndices;
            if (LisT.this.defaultValues != null) {
                if (LisT.this.defaultIndices != null) {
                    throw new BuildException("Only one of 'defaultValues=...' and 'defaultIndices=...' must be used");
                }

                // defaultValues=..., delimiters=...
                String[] defaultValues = SwingDialogTask.tokenize(LisT.this.defaultValues, LisT.this.delimiters);

                defaultIndices = new int[defaultValues.length];
                for (int i = 0; i < defaultValues.length; i++) {
                    @Nullable Object defaultValue = defaultValues[i];

                    int idx = SwingDialogTask.indexOf(values, defaultValue);
                    if (idx == -1) {
                        throw new BuildException("Default value '" + defaultValue + "' is not a valid value");
                    }
                    defaultIndices[i] = idx;
                }

                swingDialogTask.beforeShow.add(new Runnable() {
                    @Override public void run() {  LisT.select(jList, defaultIndices); }
                });
            } else
            if (LisT.this.defaultIndices != null) {

                // defaultIndices=..., delimiters=...
                defaultIndices = SwingDialogTask.tokenizeIntegers(LisT.this.defaultIndices, LisT.this.delimiters);
            } else
            {
                defaultIndices = null;
            }

            if (defaultIndices != null) {
                swingDialogTask.beforeShow.add(new Runnable() {
                    @Override public void run() {  LisT.select(jList, defaultIndices); }
                });
            }

            swingDialogTask.retrievers.add(new Runnable() {

                @Override public void
                run() {
                    int[] selectedIndices = jList.getSelectedIndices();

                    String s;
                    if (selectedIndices.length == 0) {
                        s = null;
                    } else {
                        StringBuilder sb = new StringBuilder(values[selectedIndices[0]]);
                        for (int i = 1; i < selectedIndices.length; i++) {
                            sb.append(LisT.this.delimiters.charAt(0)).append(values[selectedIndices[i]]);
                        }
                        s = sb.toString();
                    }
                    swingDialogTask.getProject().setProperty(LisT.this.property, s);
                }
            });

            if (this.visibleRowCount == -1) {
                swingDialogTask.addToMessage(jList);
            } else {
                jList.setVisibleRowCount(this.visibleRowCount);
                swingDialogTask.addToMessage(new JScrollPane(jList));
            }
        }

        private static void
        select(JList jList, int[] indices) {

            for (int index : indices) {
                if (index < 0 || index >= jList.getModel().getSize()) {
                    throw new BuildException("Index '" + index + "' out of range");
                }
            }

            jList.setSelectedIndices(indices);
        }
    }

    /***/
    public static
    class Separator implements SwingDialogTaskAddable {

        @Override public void
        adddTo(final SwingDialogTask swingDialogTask) { swingDialogTask.addToMessage(new JSeparator()); }
    }

    private void
    addToMessage(Object message) {

        if (this.message == null) {
            this.message = message;
            return;
        }

        if (this.message instanceof Object[]) {
            Object[] oa  = (Object[]) this.message;
            Object[] tmp = new Object[oa.length + 1];
            System.arraycopy(oa, 0, tmp, 0, oa.length);
            tmp[oa.length] = message;
            this.message   = tmp;
            return;
        }

        Object[] oa = new Object[2];
        oa[0]        = this.message;
        oa[1]        = message;
        this.message = oa;
    }

    /***/
    public static
    class Element__Tasks { // SUPPRESS CHECKSTYLE TypeName

        private final List<Task> tasks = new ArrayList<Task>();

        /**
         * Another task to execute.
         */
        public void addConfigured(Task task) { this.tasks.add(task); }
    }

    /** Enum wrapper for the 'optionType' parameter. */
    public
    enum OptionType {
        DEFAULT(JOptionPane.DEFAULT_OPTION),             // SUPPRESS CHECKSTYLE Javadoc
        YES_NO(JOptionPane.YES_NO_OPTION),               // SUPPRESS CHECKSTYLE Javadoc
        YES_NO_CANCEL(JOptionPane.YES_NO_CANCEL_OPTION), // SUPPRESS CHECKSTYLE Javadoc
        OK_CANCEL(JOptionPane.OK_CANCEL_OPTION),         // SUPPRESS CHECKSTYLE Javadoc
        ;

        private final int value;

        OptionType(int value) { this.value = value; }

        int intValue() { return this.value; } // SUPPRESS CHECKSTYLE Javadoc
    }

    /** Enum wrapper for the 'showXxxDialog()' return type. */
    public
    enum ShowDialogReturnType {
        YES(JOptionPane.YES_OPTION),       // SUPPRESS CHECKSTYLE Javadoc
        NO(JOptionPane.NO_OPTION),         // SUPPRESS CHECKSTYLE Javadoc
        CANCEL(JOptionPane.CANCEL_OPTION), // SUPPRESS CHECKSTYLE Javadoc
        OK(JOptionPane.OK_OPTION),         // SUPPRESS CHECKSTYLE Javadoc
        CLOSED(JOptionPane.CLOSED_OPTION), // SUPPRESS CHECKSTYLE Javadoc
        ;

        private final int value;

        ShowDialogReturnType(int value) { this.value = value; }

        private static ShowDialogReturnType
        fromInt(int value) {
            for (ShowDialogReturnType sdrt : ShowDialogReturnType.values()) {
                if (sdrt.intValue() == value) return sdrt;
            }
            throw new IllegalArgumentException(Integer.toString(value));
        }
        int intValue() { return this.value; } // SUPPRESS CHECKSTYLE Javadoc
    }

    /** Enum wrapper for the 'selectionMode' parameter. */
    public
    enum SelectionMode {
        SINGLE(ListSelectionModel.SINGLE_SELECTION),                       // SUPPRESS CHECKSTYLE Javadoc
        SINGLE_INTERVAL(ListSelectionModel.SINGLE_INTERVAL_SELECTION),     // SUPPRESS CHECKSTYLE Javadoc
        MULTIPLE_INTERVAL(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION), // SUPPRESS CHECKSTYLE Javadoc
        ;

        private final int value;

        SelectionMode(int value) { this.value = value; }

        int intValue() { return this.value; } // SUPPRESS CHECKSTYLE Javadoc
    }

    /** Enum wrapper for the 'messageType' parameter. */
    public
    enum MessageType {

        ERROR(JOptionPane.ERROR_MESSAGE),             // SUPPRESS CHECKSTYLE Javadoc
        INFORMATION(JOptionPane.INFORMATION_MESSAGE), // SUPPRESS CHECKSTYLE Javadoc
        WARNING(JOptionPane.WARNING_MESSAGE),         // SUPPRESS CHECKSTYLE Javadoc
        QUESTION(JOptionPane.QUESTION_MESSAGE),       // SUPPRESS CHECKSTYLE Javadoc
        PLAIN(JOptionPane.PLAIN_MESSAGE);             // SUPPRESS CHECKSTYLE Javadoc

        private final int value;

        MessageType(int value) { this.value = value; }

        int intValue() { return this.value; } // SUPPRESS CHECKSTYLE Javadoc
    }

    @Override public void
    execute() throws BuildException {

        // Run the "before show" runnables, i.e. set the default values on the dialog components.
        for (Runnable r : this.beforeShow) r.run();

        // Show the dialog.
        ShowDialogReturnType sdrv = ShowDialogReturnType.fromInt(JOptionPane.showOptionDialog(
            null,                        // parentComponent
            this.message,                // message
            this.title,                  // title
            this.optionType.intValue(),  // optionType
            this.messageType.intValue(), // messageType
            null,                        // icon
            null,                        // options
            null                         // initialValue
        ));

        // Hack: 'YES' and 'OK' map to the same integer value, so we need to look at the 'optionType' to
        // distinguish them.
        if (
            sdrv == ShowDialogReturnType.YES
            && (this.optionType == OptionType.DEFAULT || this.optionType == OptionType.OK_CANCEL)
        ) sdrv = ShowDialogReturnType.OK;

        // Store the srdv in the given property (if configured).
        if (this.property != null) this.getProject().setProperty(this.property, sdrv.toString());

        // Run the retrievers, i.e. set properties depending on the UI selection.
        for (final Runnable r : this.retrievers) r.run();

        // Determine the conditional tasks to execute.
        List<Task> tasks;
        switch (sdrv) {

        case YES:
            tasks = this.ifYesTasks;
            break;

        case NO:
            if (this.ifYesTasks == null && this.ifNoTasks == null) throw new BuildException("NO");
            tasks = this.ifNoTasks;
            break;

        case CANCEL:
            if (this.ifCancelTasks == null) throw new BuildException("CANCEL");
            tasks = this.ifCancelTasks;
            break;

        case OK:
            tasks = this.ifOkTasks;
            break;

        case CLOSED:
            if (this.ifClosedTasks == null) throw new BuildException("CLOSED");
            tasks = this.ifClosedTasks;
            break;

        default:
            throw new IllegalStateException(sdrv.toString());
        }

        // Execute the conditional tasks (if configured).
        if (tasks != null) {
            for (Task task : tasks) task.execute();
        }
    }

    private static String[]
    tokenize(@Nullable String s, @Nullable String delimiters) {
        if (s == null) return new String[0];

        List<String> l = new ArrayList<String>();
        for (StringTokenizer st = new StringTokenizer(s, delimiters); st.hasMoreTokens();) {
            l.add(st.nextToken());
        }
        return l.toArray(new String[l.size()]);
    }

    private static int[]
    tokenizeIntegers(@Nullable String s, @Nullable String delimiters) {
        if (s == null) return new int[0];

        List<Integer> l = new ArrayList<Integer>();
        for (StringTokenizer st = new StringTokenizer(s, delimiters); st.hasMoreTokens();) {
            String token = st.nextToken();

            int value;
            try {
                value = Integer.parseInt(token);
            } catch (NumberFormatException nfe) {
                // SUPPRESS CHECKSTYLE AvoidHidingCause
                throw new BuildException("'" + token + "' is not a valid integer");
            }
            l.add(value);
        }

        int[] result = new int[l.size()];
        for (int i = 0; i < result.length; i++) result[i] = l.get(i);
        return result;
    }

    private static int
    indexOf(Object[] array, @Nullable Object element) {
        for (int i = 0; i < array.length; i++) {
            if (SwingDialogTask.equal(array[i], element)) return i;
        }
        return -1;
    }

    private static boolean
    equal(@Nullable Object o1, @Nullable Object o2) { return o1 == null ? o2 == null : o1.equals(o2); }
}
