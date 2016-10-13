
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2016, Arno Unkrig
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

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Sets the SWING look-and-feel for this JVM, or retrieves look-and-feel-related information from the JVM.
 * <p>
 *   The look-and-feel, in particular, affects the appearance and the behavior of the dialogs produced by the {@link
 *   SwingDialogTask} task.
 * </p>
 * <p>
 *   My platform currently supports the following look-and-feels:
 * </p>
 * <p>
 *   <img src="doc-files/Metal_dialog.png" />
 * </p>
 * <p>
 *   <img src="doc-files/Motif_dialog.png" />
 * </p>
 * <p>
 *   <img src="doc-files/Nimbus_dialog.png" />
 * </p>
 * <p>
 *   <img src="doc-files/Windows_classic_dialog.png" />
 * </p>
 * <p>
 *   <img src="doc-files/Windows_dialog.png" />
 * </p>
 */
public
class SwingLookAndFeelTask extends Task {

    @Nullable public String lookAndFeelNameProperty, lookAndFeelIdProperty, lookAndFeelClassNameProperty;
    @Nullable public String auxiliaryLookAndFeelNamesProperty, auxiliaryLookAndFeelIdsProperty;
    @Nullable public String installedLookAndFeelNamesProperty, installedLookAndFeelClassNamesProperty;
    @Nullable public String name, id, className;
    public String           separator = ",";

    /**
     * Store the name, ID or class name of the currently loaded look-and-feel in the named property.
     *
     * @see UIManager#getLookAndFeel()
     * @see LookAndFeel#getName()
     */
    public void
    setGetLookAndFeelName(String propertyName) { this.lookAndFeelNameProperty = propertyName; }

    /** @see #setGetLookAndFeelName(String) */
    public void
    setGetLookAndFeelId(String propertyName) { this.lookAndFeelIdProperty = propertyName; }

    /** @see #setGetLookAndFeelName(String) */
    public void
    setGetLookAndFeelClassName(String propertyName) { this.lookAndFeelClassNameProperty = propertyName; }

    /**
     * Store the names or IDs of the auxiliary look-and-feels in the named property.
     *
     * @see UIManager#getAuxiliaryLookAndFeels()
     * @see LookAndFeel#getName()
     * @see LookAndFeel#getID()
     */
    public void
    setGetAuxiliaryLookAndFeelNames(String propertyName) { this.auxiliaryLookAndFeelNamesProperty = propertyName; }

    /** @see #setGetAuxiliaryLookAndFeelNames(String) */
    public void
    setGetAuxiliaryLookAndFeelIds(String propertyName) { this.auxiliaryLookAndFeelIdsProperty = propertyName; }

    /**
     * Store the names or class names of all available look-and-feels in the named property.
     *
     * @see UIManager#getInstalledLookAndFeels()
     * @see LookAndFeelInfo#getName()
     * @see LookAndFeelInfo#getClassName()
     */
    public void
    setGetInstalledLookAndFeelNames(String propertyName) { this.installedLookAndFeelNamesProperty = propertyName; }

    /** @see #setGetInstalledLookAndFeelNames(String) */
    public void
    setGetInstalledLookAndFeelClassNames(String propertyName) {
        this.installedLookAndFeelClassNamesProperty = propertyName;
    }

    /**
     * Load the look-and-feel designated by name or by class name.
     *
     * @see UIManager#setLookAndFeel(String)
     */
    public void
    setName(String name) {

        for (LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
            if (lafi.getName().equals(name)) {
                this.setClassName(lafi.getClassName());
                return;
            }
        }
        throw new BuildException("Unknown look-and-feel name \"" + name + "\"");
    }

    /** @see #setName(String) */
    public void
    setClassName(String className) {
        if (this.className != null) throw new BuildException("At most one look-and-feel can be set");
        this.className = className;
    }

    /**
     * Load the "cross-platform look-and-feel" (a.k.a. "Metal").
     */
    public void
    setCrossPlatform(boolean value) { if (value) this.setClassName(UIManager.getCrossPlatformLookAndFeelClassName()); }

    /**
     * Load the "system look-and-feel". (On MS Windows systems, that is usually the "Windows" look-and-feel.)
     */
    public void
    setSystem(boolean value) { if (value) this.setClassName(UIManager.getSystemLookAndFeelClassName()); }

    /**
     * The separator that is used to concatenate elements for the various {@code get*="..."} attributes
     *
     * @see #setGetInstalledLookAndFeelNames(String)
     * @see #setGetAuxiliaryLookAndFeelIds(String)
     * @see #setGetInstalledLookAndFeelNames(String)
     * @see #setGetInstalledLookAndFeelClassNames(String)
     * @ant.defaultValue ,
     */
    public void
    setSeparator(String separator) { this.separator = separator; }

    @Override public void
    execute() throws BuildException {

        if (this.lookAndFeelNameProperty != null) {
            this.getProject().setProperty(this.lookAndFeelNameProperty, UIManager.getLookAndFeel().getName());
        }

        if (this.lookAndFeelIdProperty != null) {
            this.getProject().setProperty(this.lookAndFeelIdProperty, UIManager.getLookAndFeel().getID());
        }

        if (this.lookAndFeelClassNameProperty != null) {
            this.getProject().setProperty(
                this.lookAndFeelClassNameProperty,
                UIManager.getLookAndFeel().getClass().getName()
            );
        }

        if (this.auxiliaryLookAndFeelNamesProperty != null) {
            StringBuilder result = new StringBuilder();

            LookAndFeel[] auxiliaryLookAndFeels = UIManager.getAuxiliaryLookAndFeels();
            if (auxiliaryLookAndFeels != null) {
                for (int i = 0; i < auxiliaryLookAndFeels.length; i++) {
                    if (i > 0) result.append(this.separator);
                    result.append(auxiliaryLookAndFeels[i].getName());
                }
            }
            this.getProject().setProperty(this.auxiliaryLookAndFeelNamesProperty, result.toString());
        }

        if (this.auxiliaryLookAndFeelIdsProperty != null) {
            StringBuilder result = new StringBuilder();

            LookAndFeel[] auxiliaryLookAndFeels = UIManager.getAuxiliaryLookAndFeels();
            if (auxiliaryLookAndFeels != null) {
                for (int i = 0; i < auxiliaryLookAndFeels.length; i++) {
                    if (i > 0) result.append(this.separator);
                    result.append(auxiliaryLookAndFeels[i].getID());
                }
            }
            this.getProject().setProperty(this.auxiliaryLookAndFeelIdsProperty, result.toString());
        }

        if (this.installedLookAndFeelNamesProperty != null) {
            StringBuilder result = new StringBuilder();

            LookAndFeelInfo[] installedLookAndFeelInfos = UIManager.getInstalledLookAndFeels();
            for (int i = 0; i < installedLookAndFeelInfos.length; i++) {
                if (i > 0) result.append(this.separator);
                result.append(installedLookAndFeelInfos[i].getName());
            }
            this.getProject().setProperty(this.installedLookAndFeelNamesProperty, result.toString());
        }

        if (this.installedLookAndFeelClassNamesProperty != null) {
            StringBuilder result = new StringBuilder();

            LookAndFeelInfo[] installedLookAndFeelInfos = UIManager.getInstalledLookAndFeels();
            for (int i = 0; i < installedLookAndFeelInfos.length; i++) {
                if (i > 0) result.append(this.separator);
                result.append(installedLookAndFeelInfos[i].getClassName());
            }
            this.getProject().setProperty(this.installedLookAndFeelClassNamesProperty, result.toString());
        }

        if (this.className != null) {
            try {
                UIManager.setLookAndFeel(this.className);
            } catch (Exception e) {
                throw new BuildException("Setting look-and-feel \"" + this.className + "\"", e, this.getLocation());
            }
        }
    }
}
