
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2011, Arno Unkrig
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

import java.io.File;
import java.util.logging.Level;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.expression.Parser;
import de.unkrig.commons.util.logging.SimpleLogging;
import de.unkrig.commons.util.logging.formatter.PrintfFormatter;
import de.unkrig.commons.util.logging.handler.StdoutHandler;

// CHECKSTYLE JavadocMethod:OFF

/**
 * An <a href="http://ant.apache.org">ANT</a> task which configures the {@link SimpleLogging} facility.
 */
public
class SimpleLoggingTask extends Task {

    /** Type for the {@code debug="..."} attribute. */
    public enum Debug { FINE, FINER, FINEST }

    @Nullable private File    out;
    private boolean           stdout;
    @Nullable private Level   level;
    @Nullable private Boolean noWarn, quiet, normal, verbose;
    @Nullable private Debug   debug;
    @Nullable private String  spec;
    @Nullable private String  formatter;

    // BEGIN CONFIGURATION SETTERS

    /**
     * Messages of levels INFO (inclusive) through WARNING (exclusive) will be written to the given file.
     */
    public void setOut(File outputFile) { this.out = outputFile; }

    /**
     * Messages of levels INFO (inclusive) through WARNING (exclusive) will be written to STDOUT.
     */
    public void setStdout(boolean value) { this.stdout = value; }

    /**
     * Configures the amount of logging; more precisely: The level of the "root logger".
     * <table border="1" cellpadding="3" cellspacing="0">
     *   <tr>
     *     <th>level</th>
     *     <th>Levels logged to STDERR</th>
     *     <th>Levels logged to STDOUT</th>
     *   </tr>
     *   <tr><td>SEVERE</td><td>SEVERE</td><td>-</td></tr>
     *   <tr><td>WARNING</td><td>SEVERE, WARNING</td><td>-</td></tr>
     *   <tr><td>INFO</td><td>SEVERE, WARNING</td><td>INFO</td></tr>
     *   <tr><td>CONFIG</td><td>SEVERE, WARNING</td><td>INFO, CONFIG</td></tr>
     *   <tr>
     *     <td>FINE</td>
     *     <td>SEVERE, WARNING<br>FINE<sup>*</sup></td>
     *     <td>INFO, CONFIG</td>
     *   </tr>
     *   <tr>
     *     <td>FINER</td>
     *     <td>SEVERE, WARNING<br>FINE, FINER<sup>*</sup></td>
     *     <td>INFO, CONFIG</td>
     *   </tr>
     *   <tr>
     *     <td>FINEST</td>
     *     <td>SEVERE, WARNING<br>FINE, FINER, FINEST<sup>*</sup></td>
     *     <td>INFO, CONFIG</td>
     *   </tr>
     * </table>
     * <p>
     *   <sup>*</sup>: FINE, FINER and FINEST log records are printed with class, method, source and line number
     * </p>
     * <p>
     *   The default is determined by a number of sources, e.g. the "{@code logging.properties}" file. However in
     *   many cases, the level of the root logger will initially be {@code INFO}.
     * </p>
     */
    public void setLevel(String level) { this.level = Level.parse(level); }

    /**
     * Shorthand for {@link #setLevel(String) level}{@code ="WARNING"}: Messages of levels INFO and WARNING will be
     * suppressed.
     */
    public void setNoWarn(boolean value) { this.noWarn = value; }

    /**
     * Shorthand for {@link #setLevel(String) level}{@code ="INFO"}: Messages of level INFO ("normal output") will be
     * suppressed.
     */
    public void setQuiet(boolean value) { this.quiet = value; }

    /**
     * Shorthand for {@link #setLevel(String) level}{@code ="INFO"}: Messages of level INFO ("normal output") and above
     * (WARNING and SEVERE) will be logged.
     */
    public void setNormal(boolean value) { this.normal = value; }

    /**
     * Shorthand for {@link #setLevel(String) level}{@code ="CONFIG"}: Messages of level CONFIG ("verbose output") will
     * be logged.
     */
    public void setVerbose(boolean value) { this.verbose = value; }

    /**
     * Alias for {@link #setLevel(String) level}{@code ="FINE|FINER|FINEST"}.
     */
    public void setDebug(Debug value) { this.debug = value; }

    // SUPPRESS CHECKSTYLE LineLength:8
    /**
     * Sets the <var>level</var> of the named loggers, adds the given <var>handler</var> on them and sets the given
     * <var>formatter</var> on the <var>handler</var>.
     * <p>
     *   The <var>spec</var> is parsed as follows:
     * </p>
     * <pre>
     * <var>spec</var> := [ <var>level</var> ] [ ':' [ <var>logger-names</var> ] [ ':' [ <var>handler</var> ] [ ':' [ <var>formatter</var> ] ] ] ]
     * <var>logger-names</var> := <var>logger-name</var> [ ',' <var>logger-name</var> ]...
     * </pre>
     * <p>
     *   The <var>level</var> component must be parsable by {@link Level#parse(String)}, i.e. it must be a decimal
     *   number, or one of {@code SEVERE}, {@code WARNING}, {@code INFO}, {@code CONFIG}, {@code FINE}, {@code FINER},
     *   {@code FINEST} or {@code ALL}.
     * </p>
     * <p>
     *   The <var>handler</var> and <var>formatter</var> components denote {@link Parser expressions}, with the
     *   automatically imported packages "java.util.logging", "de.unkrig.commons.util.logging.handler" and
     *   "de.unkrig.commons.util.logging.formatter".
     * </p>
     * <p>
     *   Example <var>spec</var>:
     * </p>
     * <pre>
     * FINE:de.unkrig:ConsoleHandler:FormatFormatter("%5$tF %5$tT.%5$tL %10$-20s %3$2d %8$s%9$s%n")
     * </pre>
     * <p>
     *   If any of the components of the <var>spec</var> is missing or empty, a reasonable default value is assumed:
     * </p>
     * <dl>
     *   <dt><var>level</var></dt>
     *   <dd>
     *     Logger: Level inherited from parent logger
     *     <br />
     *     Handler: Handler's default log level, typically {@code ALL}
     *   </dd>
     *
     *   <dt><var>loggers</var></dt>
     *   <dd>
     *     The root logger
     *   </dd>
     *
     *   <dt><var>handler</var></dt>
     *   <dd>
     *     {@link StdoutHandler}
     *   </dd>
     *
     *   <dt><var>formatter</var></dt>
     *   <dd>
     *     {@link PrintfFormatter#MESSAGE_AND_EXCEPTION MESSAGE_AND_EXCEPTION}
     *   </dd>
     * </dl>
     */
    public void setSpec(String spec) { this.spec = spec; }

    /**
     * The formatter to use.
     *
     * @see SimpleLogging#setFormatter(String)
     */
    public void setFormatter(String spec) { this.formatter = spec; }

    // END CONFIGURATION SETTERS

    /**
     * The ANT task "execute" method.
     *
     * @see Task#execute()
     */
    @Override public void
    execute() throws BuildException {
        try {
            if (this.out != null) SimpleLogging.setOut(this.out);

            if (this.stdout) SimpleLogging.setStdout();

            if (this.level != null) SimpleLogging.setLevel(this.level);

            if (this.noWarn  == Boolean.TRUE) SimpleLogging.setNoWarn();
            if (this.quiet   == Boolean.TRUE) SimpleLogging.setQuiet();
            if (this.normal  == Boolean.TRUE) SimpleLogging.setNormal();
            if (this.verbose == Boolean.TRUE) SimpleLogging.setVerbose();
            if (this.debug != null) {
                switch (this.debug) {
                case FINE:   SimpleLogging.setLevel(Level.FINE);   break;
                case FINER:  SimpleLogging.setLevel(Level.FINER);  break;
                case FINEST: SimpleLogging.setLevel(Level.FINEST); break;
                default:     throw new IllegalArgumentException();
                }
            }

            if (this.formatter != null) SimpleLogging.setFormatter(this.formatter);

            if (this.spec != null) SimpleLogging.configureLoggers(this.spec);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
