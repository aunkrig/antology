
/*
 * de.unkrig.ant-contrib - Some contributions to APACHE ANT
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

package test;

import org.apache.tools.ant.BuildFileTest;
import org.junit.Test;

import junit.framework.TestCase;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.ThroughputTask}.
 */
public
class ThroughputTest extends BuildFileTest {

    @Override public void
    setUp() { this.configureProject("target/test-classes/test_throughput.ant"); }

    @Test public void
    test1() {

        this.executeTarget("test1");

        TestCase.assertEquals((
            ""
            + "Starting... (100 KB)"
            + "... done! Took 1s (100 KB @ 100 KB/s)"
        ), this.getLog());
    }

    @Test public void
    test2() {

        this.executeTarget("test2");

        TestCase.assertEquals((
            ""
            + "Starting..."
            + " (2,000"
            + " of 6,000 KB = 33.3%"
            + " = approx. 0:01:00 = ETA 2014-03-05 00:01:00.000"
            + ")"
            + "... done! Took 0:01:00"
            + " (2,000 KB @ 33 KB/s"
            + "; 3,000 of 6,000 KB complete = 50.0% @ 33 KB/s"
            + "; 3,000 KB remaining = approx. 0:01:30"
            + " = ETA 2014-01-01 00:01:30.000"
            + ")"
        ), this.getLog());
    }
}
