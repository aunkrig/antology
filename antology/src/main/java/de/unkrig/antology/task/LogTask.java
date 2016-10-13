
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import de.unkrig.commons.nullanalysis.Nullable;

// CHECKSTYLE MethodCheck:OFF    Method JAVADOC

/**
 * Logs a message through {@code java.util.logging}.
 */
public
class LogTask extends Task {

    private static final Logger LOGGER      = Logger.getLogger(LogTask.class.getName());
    private static final Logger ROOT_LOGGER = Logger.getLogger("");

    private Logger           logger = LogTask.ROOT_LOGGER;
    private Level            level  = Level.INFO;
    @Nullable private String message;

    // BEGIN CONFIGURATION SETTERS

    /**
     * The name of the logger on which to log. The default is to use the "root logger".
     *
     * @see Logger
     */
    public void
    setLogger(String loggerName) { this.logger = Logger.getLogger(loggerName); }

    /**
     * The "log level", which, in combination with the logger, determines how the message is logged.
     *
     * @ant.valueExplanation OFF|SEVERE|WARNING|<u>INFO</u>|CONFIG|FINE|FINER|FINEST|ALL|<var>integer-value</var>
     * @see Logger#log(Level, String)
     */
    public void
    setLevel(String nameOrNumber) { this.level = Level.parse(nameOrNumber); }

    /**
     * The text of the message that is logged. The default is to log <em>no</em> message (which makes hardly any
     * sense).
     *
     * @see Logger#log(Level, String)
     */
    public void
    setMessage(String text) { this.message = text; }

    // END CONFIGURATION SETTERS

    /**
     * The ANT task "execute" method.
     *
     * @see Task#execute()
     */
    @Override public void
    execute() throws BuildException {
        try {
            if (this.message == null) throw new IllegalArgumentException("'message' attribute is missing");
            this.logger.log(this.level, this.message);
        } catch (Exception e) {
            LogTask.LOGGER.log(Level.SEVERE, e.toString(), e);
            throw new BuildException(e);
        }
    }
}
