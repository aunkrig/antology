
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

package de.unkrig.antology.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * Evaluates to {@code true}
 * <ul>
 *   <li>When it is evaluated for the FIRST TIME
 *   <li>When the last evaluation to {@code true} has been more than the configured interval ago
 * </ul>
 */
public
class EveryCondition implements Condition {

    @Nullable private Long interval;
    private long           expiration;

    /** The interval length in milliseconds. */
    public void
    setMilliseconds(long n) {
        if (this.interval != null) {
            throw new BuildException(
                "Exactly one of 'milliseconds=...', 'seconds=...', 'minutes=...' and 'hours=...' must be configured"
            );
        }
        this.interval = n;
    }

    /** The interval length in seconds. */
    public void
    setSeconds(double number) {
        this.setMilliseconds((long) (1000 * number));
    }

    /** The interval length in minutes. */
    public void
    setMinutes(double number) {
        this.setMilliseconds((long) (60 * 1000 * number));
    }

    /** The interval length in hours. */
    public void
    setHours(double number) {
        this.setMilliseconds((long) (60 * 60 * 1000 * number));
    }

    @Override public boolean
    eval() throws BuildException {
        Long interval = this.interval;

        if (interval == null) {
            throw new BuildException("Exactly one of 'seconds=...', 'minutes=...' and 'hours=...' must be configured");
        }

        long now = System.currentTimeMillis();
        synchronized (this) {
            if (now > this.expiration) {
                this.expiration = now + interval;
                return true;
            }
        }
        return false;
    }
}
