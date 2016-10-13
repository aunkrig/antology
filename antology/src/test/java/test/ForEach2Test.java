
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

import java.util.regex.Pattern;

import org.apache.tools.ant.BuildFileTest;
import org.junit.ComparisonFailure;
import org.junit.Test;

import junit.framework.TestCase;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.ForEach2Task}.
 */
public
class ForEach2Test extends BuildFileTest {

    @Override public void
    setUp() { this.configureProject("target/test-classes/test_forEach2.ant"); }

    @Test public void
    test1() {
        this.executeTarget("test1");
        this.assertLogContaining("123");
    }

    @Test public void
    test2() {
        this.executeTarget("test2");
        this.assertLogMatches(Pattern.compile("foo.betafoo.alpha|foo.alphafoo.beta"));
    }

    @Test public void
    test3() {
        this.executeTarget("test3");
        TestCase.assertEquals("12", this.getLog());
    }

    @Test public void
    test4() {
        this.executeTarget("test4");
        TestCase.assertEquals("13", this.getLog());
    }

    @Test public void
    test5() {
        this.executeTarget("test5");
        TestCase.assertEquals((
            ""
            + "Processing '1' (1 of 3 elements = 33.3%)"
            + "... done! Took 500ms (1 element @ 2 elements/s"
            + "; 1 of 3 elements complete = 33.3% @ 2 elements/s"
            + "; 2 elements remaining = approx. 1s"
            + ")"
            + ""
            + "Processing '2' (1 of 3 elements = 33.3% = approx. 500ms)"
            + "... done! Took 500ms (1 element @ 2 elements/s"
            + "; 2 of 3 elements complete = 66.7% @ 2 elements/s"
            + "; 1 element remaining = approx. 500ms"
            + ")"
            + ""
            + "Processing '3' (1 of 3 elements = 33.3% = approx. 500ms)"
            + "... done! Took 500ms (1 element @ 2 elements/s"
            + "; 3 of 3 elements complete = 100.0% @ 2 elements/s"
            + ")"
        ), this.getLog());
    }

    @Test public void
    test6() {
        this.executeTarget("test6");
        TestCase.assertEquals((
            ""
            + "Processing 'A' (10 of 60 bytes = 16.7%)"
            + "... done! Took 5s (10 bytes @ 2 bytes/s"
            + "; 10 of 60 bytes complete = 16.7% @ 2 bytes/s"
            + "; 50 bytes remaining = approx. 25s = ETA 2014-01-01 00:00:25.000"
            + ")"
            + ""
            + "Processing 'B' (20 of 60 bytes = 33.3% = approx. 10s = ETA 2013-01-01 00:00:10.000)"
            + "... done! Took 5s (20 bytes @ 4 bytes/s"
            + "; 30 of 60 bytes complete = 50.0% @ 3 bytes/s"
            + "; 30 bytes remaining = approx. 10s = ETA 2014-01-01 00:00:10.000"
            + ")"
            + ""
            + "Processing 'C' (30 of 60 bytes = 50.0% = approx. 10s = ETA 2013-01-01 00:00:10.000)"
            + "... done! Took 5s (30 bytes @ 6 bytes/s"
            + "; 60 of 60 bytes complete = 100.0% @ 4 bytes/s"
            + ")"
        ), this.getLog());
    }

    @Test public void
    test7() {
        this.executeTarget("test7");
        TestCase.assertEquals((
            ""
            + "Processing 'A' (10 of 60 bytes = 16.7%)"
            + "... done! Took 500ms (10 bytes @ 20 bytes/s"
            + "; 10 of 60 bytes complete = 16.7% @ 20 bytes/s"
            + "; 50 bytes remaining = approx. 2.5s"
            + ")"
            + ""
            + "Processing 'B' (20 of 60 bytes = 33.3% = approx. 1s)"
            + "... done! Took 500ms (20 bytes @ 40 bytes/s"
            + "; 30 of 60 bytes complete = 50.0% @ 30 bytes/s"
            + "; 30 bytes remaining = approx. 1s"
            + ")"
            + ""
            + "Processing 'C' (30 of 60 bytes = 50.0% = approx. 1s)"
            + "... done! Took 500ms (30 bytes @ 60 bytes/s"
            + "; 60 of 60 bytes complete = 100.0% @ 40 bytes/s"
            + ")"
        ), this.getLog());
    }

    @Test public void
    test8() {
        this.executeTarget("test8");
        TestCase.assertEquals((
            ""
            + "Processing 'target/test-classes/test_forEach2/ALPHA.txt' (5 of 17 bytes = 29.4%)"
            + "| Content of file 'target/test-classes/test_forEach2/ALPHA.txt':"
            + "| One"
            + "... done! Took 500ms (5 bytes @ 10 bytes/s"
            + "; 5 of 17 bytes complete = 29.4% @ 10 bytes/s"
            + "; 12 bytes remaining = approx. 1.2s)"
            + ""
            + "Processing 'target/test-classes/test_forEach2/BETA.txt' (5 of 17 bytes = 29.4% = approx. 500ms)"
            + "| Content of file 'target/test-classes/test_forEach2/BETA.txt':"
            + "| Two"
            + "... done! Took 500ms (5 bytes @ 10 bytes/s"
            + "; 10 of 17 bytes complete = 58.8% @ 10 bytes/s"
            + "; 7 bytes remaining = approx. 700ms)"
            + ""
            + "Processing 'target/test-classes/test_forEach2/GAMMA.txt' (7 of 17 bytes = 41.2% = approx. 700ms)"
            + "| Content of file 'target/test-classes/test_forEach2/GAMMA.txt':"
            + "| Three"
            + "... done! Took 500ms (7 bytes @ 14 bytes/s"
            + "; 17 of 17 bytes complete = 100.0% @ 11 bytes/s)"
        ), this.getLog());
    }

    @Test public void
    test9() {
        this.executeTarget("test9");

        TestCase.assertEquals(
            "message_defaultmessage_errormessage_warningmessage_info",
            this.getLog()
        );
        TestCase.assertEquals(
            "",
            this.getError()
        );
        this.assertContains(
            "Build sequence for target(s) `test9' is",
            this.getFullLog()
        );
        this.assertContains(
            "message_defaultmessage_errormessage_warningmessage_infomessage_verbosemessage_debug",
            this.getFullLog()
        );
        TestCase.assertEquals("",  this.getOutput());
    }

    public void
    assertContains(String expectedSubstring, String actual) {
        if (!actual.contains(expectedSubstring)) {
            throw new ComparisonFailure(null, expectedSubstring, actual);
        }
    }

    public void
    assertLogMatches(Pattern regex) {
        String log = this.getLog();
        if (!regex.matcher(log).find()) {
            TestCase.fail("expecting log to contain '" + regex + "' log was '" + log + "'");
        }
    }
}
