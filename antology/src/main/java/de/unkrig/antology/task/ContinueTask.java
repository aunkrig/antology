
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;

import de.unkrig.antology.util.FlowControlException;
import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Causes the enclosing {@link ForEach2Task} to continue with the next iteration element.
 */
public
class ContinueTask extends Task {

    @Nullable private Condition condition;

    public
    ContinueTask() {}

    /**
     * An optional condition; the {@link ContinueTask} will executed iff the condition holds true.
     */
    public void
    add(Condition condition) {
        if (this.condition != null) throw new BuildException("Only one condition subelement must be configured");
        this.condition = condition;
    }

    @Override public void
    execute() {
        Condition condition = this.condition;
        if (condition == null || condition.eval()) throw new ContinueException();
    }

    /** Does not indicate a failure, but the execution of a {@code <continue>} task. */
    public static final
    class ContinueException extends FlowControlException {

        private static final long serialVersionUID = 1L;

        public
        ContinueException() { super("Uncaught <continue>"); }

        /** @return Whether {@code be} wraps a {@link ContinueException} */
        public static boolean
        isWrappedBy(BuildException be) {
            for (Throwable t = be.getCause(); t != null; t = t.getCause()) {
                if (t instanceof ContinueException) return true;
            }
            return false;
        }
    }
}
