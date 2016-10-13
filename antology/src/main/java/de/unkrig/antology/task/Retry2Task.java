
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

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.property.LocalProperties;

import de.unkrig.antology.util.FlowControlException;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.time.Duration;
import de.unkrig.commons.util.time.PointOfTime;

/***
 * An enhanced version of the <a href="http://ant.apache.org/manual/Tasks/retry.html">{@code <retry>}</a> task.
 * Executes its subtasks sequentially, and if one of these fails, and some other conditions hold true, then the
 * subtasks are re-executed.
 */
public
class Retry2Task extends Task implements TaskContainer {

    public
    Retry2Task() {}

    /** (Must be public for ANTDOC.) */
    public static final double DEFAULT_RETRY_DELAY_EXPONENT = 1.0;

    // -------------------------- CONFIGURATION --------------------------

    @Nullable private Integer     status;
    @Nullable private Integer     retryCount;
    @Nullable private Duration    retryDelay;
    private double                retryDelayExponent = Retry2Task.DEFAULT_RETRY_DELAY_EXPONENT;
    @Nullable private PointOfTime delayIntervalBegin;
    private final List<Task>      tasks              = new ArrayList<Task>();

    /**
     * If set, then the tasks are only re-executed iff the exit status of the failed task equals <var>n</var>, i.e.
     * <code>&lt;fail&nbsp;status="<var>n</var>"&nbsp;/&gt;</code> was executed.
     */
    public void
    setStatus(int n) { this.status = n; }

    /**
     * If set, then this task fails when the <var>{@code N}</var>th retry fails.
     * <p>
     * Value zero means to execute the operation only once (and not retry).<br>
     * Values greater than zero mean to
     * try the operation at most <var>{@code N}</var>{@code +1} times.
     */
    public void setRetryCount(int n) { this.retryCount = n; }

    /**
     * If set, then this task sleeps for the given duration before retrying.
     */
    public void setRetryDelay(Duration duration) { this.retryDelay = duration; }

    /**
     * The retry delay is multiplied with this factor for each retry, so the retry delay will grow exponentially.
     * A value of "2" doubles the retry delay for each retry, a value of "1" (the default) yields a constant retry
     * delay.
     *
     * @ant.valueExplanation <i>float</i>
     * @ant.defaultValue     {@value #DEFAULT_RETRY_DELAY_EXPONENT}
     */
    public void setRetryDelayExponent(double value) { this.retryDelayExponent = value; }

    /**
     * Use the given point-of-time as the beginning of the delay interval, instead of the current time.
     *
     * @deprecated For testing only
     */
    @Deprecated public void setDelayIntervalBegin(PointOfTime pointOfTime) { this.delayIntervalBegin = pointOfTime; }

    /**
     * Adds another subtask.
     */
    @Override public void
    addTask(@Nullable Task task) { assert task != null; this.tasks.add(task); }

    /**
     * Checks whether the operation should be retried, waits for the configured delay (if any), and logs
     * "Retrying...".
     *
     * @param n 0 for the first retry, 1 for the second retry, etc.
     * @return  Whether the preceding operation should be retried
     */
    public boolean
    retry(int n) {
        Integer retryCount = this.retryCount;
        if (retryCount != null && n >= retryCount) return false;

        Duration retryDelay = this.retryDelay;
        if (retryDelay != null) {
            retryDelay = retryDelay.multiply(Math.pow(this.retryDelayExponent, n));
            PointOfTime delayIntervalBegin = this.delayIntervalBegin;
            if (delayIntervalBegin == null) delayIntervalBegin = new PointOfTime();
            this.log((
                "Will retry in "
                + retryDelay
                + " (on "
                + delayIntervalBegin.add(retryDelay)
                + ")..."
            ), Project.MSG_INFO);

            try {
                Thread.sleep(retryDelay.milliseconds());
            } catch (InterruptedException ie) {
                throw new BuildException(ie);
            }
        }
        this.log("Retrying...", Project.MSG_INFO);

        return true;
    }

    // -------------------------- END OF CONFIGURATION --------------------------

    @Override public void
    execute() {

        for (int n = 0;; n++) {
            try {
                LocalProperties localProperties = LocalProperties.get(this.getProject());

                localProperties.enterScope();
                try {
                    for (Task task : this.tasks) task.perform();
                } finally {
                    localProperties.exitScope();
                }

                return;
            } catch (RuntimeException re) {

                // Check for and honor wrapped FlowControlException.
                if (FlowControlException.isWrappedBy(re)) throw re;

                // Check for and honor wrapped ExitStatusException.
                {
                    Integer status = this.status;
                    if (status != null) {
                        for (Throwable t = re; t != null; t = t.getCause()) {
                            if (t instanceof ExitStatusException) {
                                ExitStatusException ese = (ExitStatusException) t;

                                if (ese.getStatus() != status) throw re;
                            }
                        }
                    }
                }

                // Print exception chain.
                for (Throwable t = re; t != null; t = t.getCause()) this.log(t.toString(), Project.MSG_INFO);

                if (!this.retry(n)) {
                    throw new BuildException("Giving up after " + n + " retries: " + re.getMessage(), re);
                }
            }
        }
    }
}
