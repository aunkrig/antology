
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.property.LocalProperties;

import de.unkrig.antology.util.Logging;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.time.Duration;
import de.unkrig.commons.util.time.PointOfTime;
import de.unkrig.commons.util.time.TimeInterval;

/**
 * Executes the tasks configured as subelements and reports about the elapsed time, and, optionally, the "current
 * throughput" (i.e processing rate), and, optionally, percentages of completion, and, optionally, an estimate about
 * how long the precessing of the "remaining" elements will take.
 * <p>
 *   More formally: Prints a "{@link #setMessage(String) before message}", executes the nested tasks sequentially, and
 *   prints an "{@link #setMessage(String) after message}".
 * </p>
 * <p>
 *   <var>point-of-time</var> can have the format <var>yyyy</var>{@code
 *   -}<var>MM</var>{@code -}<var>dd</var>{@code T}<var>HH</var>{@code :}<var>mm</var>{@code :}<var>ss</var>, or the
 *   format <var>yyyy</var>{@code -}<var>MM</var>{@code -}<var>dd</var>, or any format understood by {@link
 *   DateFormat#parse(String)}.
 * </p>
 * <var>duration</var> can have one of the formats
 * <ul>
 *   <li><var>s</var>{@code .}<var>s</var> {@code s}</li>
 *   <li><var>s</var>{@code .}<var>s</var> {@code sec}</li>
 *   <li><var>s</var>{@code .}<var>s</var> {@code secs}</li>
 *   <li><var>m</var>{@code :}<var>s</var>{@code .}<var>s</var></li>
 *   <li><var>h</var>{@code :}<var>m</var>{@code :}<var>s</var>{@code .}<var>s</var></li>
 *   <li><var>d</var> {@code d} (days)</li>
 *   <li><var>d</var> {@code d} <var>h</var>{@code :}<var>m</var></li>
 *   <li><var>d</var> {@code d} <var>h</var>{@code :}<var>m</var>{@code :}<var>s</var>{@code .}<var>s</var></li>
 *   <li><var>w</var> {@code w} (weeks)</li>
 *   <li><var>w</var> {@code w} <var>d</var> {@code d}</li>
 *   <li><var>w</var> {@code w} <var>d</var> {@code d} <var>h</var>{@code :}<var>m</var></li>
 *   <li>
 *     <var>w</var> {@code w} <var>d</var> {@code d} <var>h</var>{@code :}<var>m</var>{@code :}<var>s</var>{@code
 *     .}<var>s</var>
 *   </li>
 * </ul>
 * <p>
 *   Whitespace and seconds' fractions ("{@code .}<var>s</var>") are optional.
 * </p>
 *
 * <a name="Before_message" />
 * <h3>Before message</h3>
 *
 * <p>
 *   Before the first nested task is executed, a message is composed from "fragments" as follows and logged:
 * </p>
 * <table border="1" rules="all">
 *   <tr>
 *     <th>Fragment #</th>
 *     <th>{@link #setPreviousQuantity(double) pQ}</th>
 *     <th>{@link #setPreviousDuration(Duration) pD}</th>
 *     <th>{@link #setCurrentQuantity(double) cQ}</th>
 *     <th>{@link #setRemainingQuantity(double) rQ}</th>
 *     <th>{@link #setShowEta(boolean) showEta}</th>
 *     <th>Text fragment</th>
 *   </tr>
 *   <tr>
 *     <td>1</td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *     <td>{@link #setMessage(String) message}</td>
 *   </tr>
 *   <tr>
 *     <td>
 *       2a
 *       <br />
 *       2b
 *     </td>
 *     <td>
 *       <br />
 *       Y
 *     </td>
 *     <td></td>
 *     <td>
 *       Y
 *       <br />
 *       Y
 *     </td>
 *     <td>
 *       <br />
 *       Y
 *     </td>
 *     <td></td>
 *     <td>
 *       <code>&nbsp;(</code><var>cQ</var> <var>qU</var>
 *       <br />
 *       <code>&nbsp;(</code><var>cQ</var> {@code of} <var>pQ+cQ+rQ</var> <var>qU</var> =
 *       <var>cQ/(pQ+cQ+rQ</var>){@code %}
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>3</td>
 *     <td>&gt;0</td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td></td>
 *     <td><code>&nbsp;= approx. </code><var>cQ*pD/pQ</var> {@code sec}</td>
 *   </tr>
 *   <tr>
 *     <td>4</td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td>true</td>
 *     <td><code>&nbsp;= ETA </code><var>cB+cQ*pD/pQ</var></td>
 *   </tr>
 *   <tr>
 *     <td>5</td>
 *     <td></td>
 *     <td></td>
 *     <td>Y</td>
 *     <td></td>
 *     <td></td>
 *     <td>{@code )}</td>
 *   </tr>
 * </table>
 *
 * <p>
 *   <b>Example:</b>
 * </p>
 * <p>
 *   Parameters are specified as follows:
 * </p>
 * <p>
 *   {@code quantityUnit="KB" previousQuantity="2000" previousDuration="60s" currentQuantity="2000"
 *   remainingQuantity="2000" showEta="true"}
 * </p>
 * <p>
 *   According to the table above, the "before" message is composed of fragments 1, 2b, 3, 4 and 5:
 * </p>
 * <pre>
 *    Starting... (2,000KB of 6,000KB = 33.3% = approx. 60.0 sec = ETA 2014-03-05 00:01:00)
 * </pre>
 *
 * <a name="After_message" />
 * <h3>After message</h3>
 *
 * <p>
 *   When the execution of the last of the nested tasks has completed, another message is composed as follows and
 *   logged:
 * </p>
 * <table border="1" rules="all">
 *   <tr>
 *     <th>Fragment #</th>
 *     <th>{@link #setPreviousQuantity(double) pQ}</th>
 *     <th>{@link #setPreviousDuration(Duration) pD}</th>
 *     <th>{@link #setCurrentQuantity(double) cQ}</th>
 *     <th>{@link #setCurrentDuration(Duration) cD}</th>
 *     <th>{@link #setRemainingQuantity(double) rQ}</th>
 *     <th>{@link #setShowEta(boolean) showEta}</th>
 *     <th>Text</th>
 *   </tr>
 *   <tr>
 *     <td>1</td>
 *     <td></td>
 *     <td></td>
 *     <td></td>
 *     <td>Y</td>
 *     <td></td>
 *     <td></td>
 *     <td>{@code ... done! Took} <var>cD</var></td>
 *   </tr>
 *   <tr>
 *     <td>2</td>
 *     <td></td>
 *     <td></td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td></td>
 *     <td><code>&nbsp;</code>{@code (}<var>cQ</var> <var>qU</var></td>
 *   </tr>
 *   <tr>
 *     <td>3</td>
 *     <td></td>
 *     <td></td>
 *     <td>Y</td>
 *     <td>&gt;0</td>
 *     <td></td>
 *     <td></td>
 *     <td><code>&nbsp;</code>{@code @}&nbsp;<var>cQ/cD</var> <var>qU</var>&nbsp;{@code s}</td>
 *   </tr>
 *   <tr>
 *     <td>4a<br />4b</td>
 *     <td>&gt;0<br />Y</td>
 *     <td></td>
 *     <td>Y<br />Y</td>
 *     <td>Y<br />Y</td>
 *     <td><br />Y</td>
 *     <td></td>
 *     <td>{@code ;} <var>pQ+cQ</var></td>
 *   </tr>
 *   <tr>
 *     <td>5</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td><code>&nbsp;</code>{@code of}&nbsp; <var>pQ+cQ+rQ</var></code>
 *   </tr>
 *   <tr>
 *     <td>6a<br />6b</td>
 *     <td>&gt;0<br />Y</td>
 *     <td></td>
 *     <td>Y<br />Y</td>
 *     <td>Y<br />Y</td>
 *     <td>&nbsp;<br />Y</td>
 *     <td></td>
 *     <td><code>&nbsp;</code><var>qU</var>&nbsp;{@code complete}</code>
 *   </tr>
 *   <tr>
 *     <td>7</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td><code>&nbsp;</code>{@code =}&nbsp;<var>(pQ+cQ)/(pQ+cQ+rQ)</var>{@code %}</td>
 *   </tr>
 *   <tr>
 *     <td>8a<br />8b</td>
 *     <td>&gt;0<br />Y</td>
 *     <td>Y<br />Y</td>
 *     <td>Y<br />Y</td>
 *     <td>Y<br />Y</td>
 *     <td>&nbsp;<br />Y</td>
 *     <td></td>
 *     <td><code>&nbsp;</code>{@code @}&nbsp;<var>(pQ+cQ)/(pD+cD)</var>&nbsp;<var>qU</var>{@code /s}</td>
 *   </tr>
 *   <tr>
 *     <td>9</td>
 *     <td></td>
 *     <td></td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td>&gt;0</td>
 *     <td></td>
 *     <td>&nbsp;{@code ;}&nbsp;<var>rQ</var>&nbsp;<var>qU</var>&nbsp;{@code remaining}</td>
 *   </tr>
 *   <tr>
 *     <td>10a<br />10b</td>
 *     <td>Y<br />&nbsp;</td>
 *     <td>Y<br />&nbsp;</td>
 *     <td>Y<br />&gt;0</td>
 *     <td>Y<br />Y</td>
 *     <td>&gt;0<br />&gt;0</td>
 *     <td>*</td>
 *     <td>
 *       <code>&nbsp;</code>{@code = approx.}&nbsp;<var>rQ*(pD+cD)/(pQ+cQ)</var>
 *       <br />
 *       <code>&nbsp;</code>{@code = approx.}&nbsp;<var>rQ*cD/cQ</var></td>
 *   </tr>
 *   <tr>
 *     <td>11a<br />11b</td>
 *     <td>Y<br />&nbsp;</td>
 *     <td>Y<br />&nbsp;</td>
 *     <td>Y<br />&gt;0</td>
 *     <td>Y<br />Y</td>
 *     <td>&gt;0<br />&gt;0</td>
 *     <td>true<br />true</td>
 *     <td>
 *       <code>&nbsp;</code>{@code = ETA}&nbsp;<var>rB+rQ*(pD+cD)/(pQ+cQ)</var>
 *       <br />
 *       <code>&nbsp;</code>{@code = ETA}&nbsp;<var>rB+rQ*cD/cQ</var></td>
 *   </tr>
 *   <tr>
 *     <td>12</td>
 *     <td></td>
 *     <td></td>
 *     <td>Y</td>
 *     <td>Y</td>
 *     <td></td>
 *     <td></td>
 *     <td><code>)</code>
 *   </tr>
 * </table>
 * <p>
 *   Example 'after' message:
 * </p>
 * <pre>
 *  ... done! Took 1.000s (100 bytes @ 100 bytes/s; 200 of 300 bytes complete = 66.7% @ 100
 *  bytes/s; 100 bytes remaining = approx. 1.000s = ETA 2014-02-24 00:56:00)
 * </pre>
 */
public
class ThroughputTask extends Task implements TaskContainer {

    private static final Locale LOCALE = Locale.US;

    private String           message     = "Starting...";
    private final List<Task> nestedTasks = new ArrayList<Task>();

    @Nullable private Double previousQuantity;
    @Nullable private Double currentQuantity;
    @Nullable private Double remainingQuantity;
    @Nullable private Double totalQuantity;

    private final TimeInterval previousInterval  = new TimeInterval();
    private final TimeInterval currentInterval   = new TimeInterval();
    private final TimeInterval remainingInterval = new TimeInterval();

    @Nullable private String quantityUnit;
    private boolean          showEta;

    // -----------------------------------------

    // CHECKSTYLE JavadocMethod:OFF

    /**
     * The prefix of the message to log before execution of the nested tasks begins (see before message).
     *
     * @ant.defaultValue {@code "Starting..."}
     */
    public void setMessage(String text) { this.message = text; }

    /**
     * Defines the quantity processed in the "previous interval".
     *
     * @ant.valueExplanation <i>float</i>
     * @ant.defaultValue     0.0
     */
    public void setPreviousQuantity(double number) { this.previousQuantity = number; this.checkConsistency(); }

    /**
     * Defines the quantity processed in the "current interval".
     *
     * @ant.valueExplanation <i>float</i>
     * @ant.defaultValue     0.0
     */
    public void setCurrentQuantity(double number) { this.currentQuantity = number; this.checkConsistency(); }

    /**
     * Defines the quantity processed in the "remaining interval".
     *
     * @ant.valueExplanation <i>float</i>
     * @ant.defaultValue     0.0
     */
    public void setRemainingQuantity(double number) { this.remainingQuantity = number; this.checkConsistency(); }

    /**
     * Defines the total of {@link #setPreviousQuantity(double)}, {@link #setCurrentQuantity(double)} and {@link
     * #setRemainingQuantity(double)}.
     *
     * @ant.valueExplanation <i>float</i>
     * @ant.defaultValue     0.0
     */
    public void setTotalQuantity(double number) { this.totalQuantity = number; this.checkConsistency(); }

    /**
     * Defines the time of the beginning of the "previous interval".
     */
    public void setPreviousBeginning(PointOfTime value) { this.previousInterval.setBeginning(value); }

    /**
     * Defines the duration of the "previous interval".
     */
    public void setPreviousDuration(Duration value) { this.previousInterval.setDuration(value); }

    /**
     * Defines the end of the "previous interval".
     */
    public void setPreviousEnding(PointOfTime value) { this.previousInterval.setEnding(value); }

    /**
     * Defines the time of the beginning of the "current interval".
     */
    public void setCurrentBeginning(PointOfTime value) { this.currentInterval.setBeginning(value); }

    /**
     * Defines the duration of the "current interval".
     */
    public void setCurrentDuration(Duration value) { this.currentInterval.setDuration(value); }

    /**
     * Defines the end of the "current interval".
     */
    public void setCurrentEnding(PointOfTime value) { this.currentInterval.setEnding(value); }

    /**
     * Defines the time of the beginning of the "remaining interval".
     */
    public void setRemainingBeginning(PointOfTime value) { this.remainingInterval.setBeginning(value); }

    /**
     * The unit to be displayed in all counts and rates, e.g. "bytes" or "m". If the related quantity is "1", then the
     * quantity unit is automatically "singularized", which means that a trailing "s" is chopped off.
     */
    public void setQuantityUnit(@Nullable String unitName) { this.quantityUnit = unitName; }

    /**
     * Whether to compute and display the 'estimated time of arrival' (if enough of the other attributes are configured
     * to compute one).
     */
    public void setShowEta(boolean value) { this.showEta = value; }

    /**
     * The tasks to execute sequentially.
     */
    @Override public void
    addTask(@Nullable Task task) { this.nestedTasks.add(task); }

    // CHECKSTYLE JavadocMethod:ON

    // -----------------------------------------

    @Override public void
    execute() {

        final Double previousQuantity  = this.previousQuantity;
        final Double currentQuantity   = this.currentQuantity;
        final Double remainingQuantity = this.remainingQuantity;

        TimeInterval previousInterval  = this.previousInterval;
        TimeInterval currentInterval   = this.currentInterval;

        // previousInterval.ending = currentInterval.beginning = now.
        {
            PointOfTime now = new PointOfTime();
            if (previousInterval.getEnding() == null) {
                previousInterval = new TimeInterval(previousInterval).setEnding(now);
            }
            if (currentInterval.getBeginning() == null) {
                currentInterval = new TimeInterval(currentInterval).setBeginning(now);
            }
        }

        // Compose and log the BEFORE message.
        PointOfTime currentIntervalBeginning = currentInterval.getBeginning();
        assert currentIntervalBeginning != null;
        this.log(ThroughputTask.composeBeforeMessage(
            this.message,                   // message
            previousQuantity,               // previousQuantity
            previousInterval.getDuration(), // previousQuantity
            currentQuantity,                // currentQuantity
            currentIntervalBeginning,       // currentBeginning
            remainingQuantity,              // remainingQuantity
            this.showEta,                   // showEta
            this.quantityUnit               // quantityUnit
        ));

        LocalProperties localProperties = LocalProperties.get(this.getProject());
        localProperties.enterScope();

        String originalMessagePrefix = Logging.getLogMessagePrefix(this.getProject());
        try {
            Logging.setLogMessagePrefix(this.getProject(), originalMessagePrefix + "| ");
            for (Task t : this.nestedTasks) t.perform();
        } catch (ExitStatusException ese) {

            // Handle '<fail status="0" />' like normal completion.
            if (ese.getStatus() != 0) throw ese;
        } finally {
            localProperties.exitScope();
            Logging.setLogMessagePrefix(this.getProject(), originalMessagePrefix);
        }

        // currentInterval.ending = remainingInterval.beginning = now.
        TimeInterval remainingInterval = this.remainingInterval;
        {
            PointOfTime now = new PointOfTime();
            if (currentInterval.getEnding() == null) {
                currentInterval = new TimeInterval(currentInterval).setEnding(now);
            }
            if (remainingInterval.getBeginning() == null) {
                remainingInterval = new TimeInterval(remainingInterval).setBeginning(now);
            }
        }

        Duration currentDuration = currentInterval.getDuration();
        assert currentDuration != null;
        PointOfTime remainingBeginning = remainingInterval.getBeginning();
        assert remainingBeginning != null;
        this.log(ThroughputTask.composeAfterMessage(
            previousQuantity,
            previousInterval.getDuration(),
            currentQuantity,
            currentDuration,
            remainingQuantity,
            remainingBeginning,
            this.showEta,
            this.quantityUnit
        ));
    }

    /**
     * Composes and returns a nice 'before message', as explained <a
     * href="http://antology.unkrig.de/antdoc/tasks/throughput.html#Before_message">here</a>.
     *
     * @param message      The 'original' text to which throughput text will be appended
     * @param quantityUnit E.g. 'min' or 'bytes' or {@code null}
     */
    public static String
    composeBeforeMessage(
        String             message,
        @Nullable Double   previousQuantity,
        @Nullable Duration previousDuration,
        @Nullable Double   currentQuantity,
        PointOfTime        currentBeginning,
        @Nullable Double   remainingQuantity,
        boolean            showEta,
        @Nullable String   quantityUnit
    ) {
        if (currentQuantity != null) {
            quantityUnit = quantityUnit == null ? "" : " " + quantityUnit;

            if (previousQuantity != null && remainingQuantity != null) {
                double totalQuantity = previousQuantity + currentQuantity + remainingQuantity;
                message += String.format(
                    ThroughputTask.LOCALE,
                    " (%,.0f of %,.0f%s",
                    currentQuantity,
                    totalQuantity,
                    ThroughputTask.singularize(quantityUnit, totalQuantity)
                );
                if (totalQuantity != 0) {
                    message += String.format(
                        ThroughputTask.LOCALE,
                        " = %.1f%%",
                        100.0 * currentQuantity / totalQuantity
                    );
                }
            } else {
                message += String.format(
                    ThroughputTask.LOCALE,
                    " (%,.0f%s",
                    currentQuantity,
                    ThroughputTask.singularize(quantityUnit, currentQuantity)
                );
            }
            if (previousQuantity != null && previousQuantity != 0 && previousDuration != null) {
                Duration estimatedCurrentDuration = previousDuration.multiply(currentQuantity).divide(previousQuantity);
                message += String.format(ThroughputTask.LOCALE, " = approx. %s", estimatedCurrentDuration);
                if (showEta) {
                    message += String.format(
                        " = ETA %s",
                        currentBeginning.add(estimatedCurrentDuration)
                    );
                }
            }
            message += ")";
        }
        return message;
    }

    /**
     * Attempts to derive the singular form from the <var>word</var>. The rules for that transformation are as follows:
     * <ul>
     *   <li>Iff <var>word</var> ends with {@code "ies"}, then that suffix is replaced with "y"</li>
     *   <li>Iff <var>word</var> ends with {@code "s"}, then that suffix is removed</li>
     *   <li>The <var>word</var> is left unchanged</li>
     * </ul>
     * <p>
     *   Notice that this strategy works well for the english language (in most cases), but more or less bad for other
     *   languages.
     * </p>
     */
    private static String
    singularize(String word, Double quantity) {
        return (
            quantity != 1        ? word                                       :
            word.endsWith("ies") ? word.substring(0, word.length() - 3) + "y" :
            word.endsWith("s")   ? word.substring(0, word.length() - 1)       :
            word
        );
    }

    /**
     * Composes and returns a nice 'after message', as explained <a
     * href="http://antology.unkrig.de/antdoc/tasks/throughput.html#After_message">here</a>.
     *
     * @param quantityUnit E.g. 'min' or 'bytes' or {@code null}
     */
    public static String
    composeAfterMessage(
        @Nullable Double   previousQuantity,
        @Nullable Duration previousDuration,
        @Nullable Double   currentQuantity,
        Duration           currentDuration,
        @Nullable Double   remainingQuantity,
        PointOfTime        remainingBeginning,
        boolean            showEta,
        @Nullable String   quantityUnit
    ) {

        // (1) "... done! Took 1.000s"
        String afterMessage = String.format(ThroughputTask.LOCALE, "... done! Took %s", currentDuration);

        if (currentQuantity == null) return afterMessage;

        quantityUnit = quantityUnit == null ? "" : " " + quantityUnit;

        // (2) " (123 bytes"
        afterMessage += String.format(
            ThroughputTask.LOCALE,
            " (%,.0f%s",
            currentQuantity,
            ThroughputTask.singularize(quantityUnit, currentQuantity)
        );

        if (!currentDuration.isZero()) {
            double currentRate = currentQuantity / currentDuration.toSeconds();

            // (3) " @ 17 bytes/s"
            afterMessage += String.format(
                ThroughputTask.LOCALE,
                " @ %,.0f%s/s",
                currentRate,
                ThroughputTask.singularize(quantityUnit, currentRate)
            );
        }

        if (previousQuantity != null && (previousQuantity > 0 || remainingQuantity != null)) {

            // (4) "; 1000"
            afterMessage += String.format(
                ThroughputTask.LOCALE,
                "; %,.0f",
                previousQuantity + currentQuantity
            );

            if (remainingQuantity != null) {

                // (5) " of 10000"
                afterMessage += String.format(
                    ThroughputTask.LOCALE,
                    " of %,.0f",
                    previousQuantity + currentQuantity + remainingQuantity
                );
            }

            // (6) " bytes complete"
            afterMessage += String.format(
                ThroughputTask.LOCALE,
                "%s complete",
                ThroughputTask.singularize(
                    quantityUnit,
                    previousQuantity + currentQuantity + (remainingQuantity == null ? 0 : remainingQuantity)
                )
            );

            if (remainingQuantity != null) {
                double totalQuantity = previousQuantity + currentQuantity + remainingQuantity;
                if (totalQuantity != 0) {

                    // (7) " = 10.0%"
                    afterMessage += String.format(
                        ThroughputTask.LOCALE,
                        " = %.1f%%",
                        100.0 * (previousQuantity + currentQuantity) / totalQuantity
                    );
                }
            }

            if (previousDuration != null) {
                Duration cumulatedDuration = previousDuration.add(currentDuration);
                if (!cumulatedDuration.isZero()) {
                    double cumulatedRate = (previousQuantity + currentQuantity) / cumulatedDuration.toSeconds();

                    // (8) " @ 100 bytes/s"
                    afterMessage += String.format(
                        ThroughputTask.LOCALE,
                        " @ %,.0f%s/s",
                        cumulatedRate,
                        ThroughputTask.singularize(quantityUnit, cumulatedRate)
                    );
                }
            }
        }

        // Can we calculate a forecast?
        if (remainingQuantity == null) return afterMessage + ")";

        // If the remaining quantity is zero, then it does not make sense to calculate a forecast.
        if (remainingQuantity == 0) return afterMessage + ")";

        // (9) "; 9000 bytes remaining"
        afterMessage += String.format(
            ThroughputTask.LOCALE,
            "; %,.0f%s remaining",
            remainingQuantity,
            ThroughputTask.singularize(quantityUnit, remainingQuantity)
        );

        // Calculate the 'seconds per unit' (the inverse of the 'rate') for the following ETR / ETA calculation.
        // If all of '(previous current)(Duration Quantity)' are known, calculate it from these; otherwise,
        // calculate it from 'current(Duration Quantity)'.
        Double secondsPerUnit = (
            previousDuration != null && previousQuantity != null && previousQuantity + currentQuantity > 0
            ? (Double) (previousDuration.add(currentDuration).toSeconds() / (previousQuantity + currentQuantity))
            : currentQuantity > 0
            ? currentDuration.toSeconds() / currentQuantity
            : null
        );
        if (secondsPerUnit != null) {
            Duration remainingDuration = new Duration(remainingQuantity * secondsPerUnit);

            // (10) " = approx. 3.234s"
            afterMessage += String.format(
                ThroughputTask.LOCALE,
                " = approx. %s",
                remainingDuration
            );
            if (showEta) {

                // (11) " = ETA yyyy-MM-dd HH:mm:ss.SSS"
                afterMessage += String.format(
                    ThroughputTask.LOCALE,
                    " = ETA %s",
                    remainingBeginning.add(remainingDuration)
                );
            }
        }

        // (12) ")"
        return afterMessage + ")";
    }

    private void
    checkConsistency() {
        Double previousQuantity  = this.previousQuantity;
        Double currentQuantity   = this.currentQuantity;
        Double remainingQuantity = this.remainingQuantity;
        Double totalQuantity     = this.totalQuantity;

        if (previousQuantity == null) {
            if (currentQuantity != null && remainingQuantity != null && totalQuantity != null) {
                this.previousQuantity = totalQuantity - currentQuantity - remainingQuantity;
            }
        } else
        if (currentQuantity == null) {
            if (remainingQuantity != null && totalQuantity != null) {
                this.currentQuantity = totalQuantity - previousQuantity - remainingQuantity;
            }
        } else
        if (remainingQuantity == null) {
            if (totalQuantity != null) {
                this.remainingQuantity = totalQuantity - previousQuantity - currentQuantity;
            }
        } else
        if (totalQuantity == null) {
            this.totalQuantity = previousQuantity + currentQuantity + remainingQuantity;
        } else
        if (previousQuantity + currentQuantity + remainingQuantity != totalQuantity) {
            throw new BuildException(
                "'previousQuantity=...', 'currentQuantity=...', 'remainingQuantity=...' and 'totalQuantity=...' are "
                + "inconsistently configured"
            );
        }
    }
}
