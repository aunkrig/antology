
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

package test;

import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.ExitStatusException;
import org.junit.Test;

import de.unkrig.commons.nullanalysis.Nullable;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.ForEach2Task}.
 */
public
class Retry2Test extends BuildFileTest {

    @Override public void
    setUp() { this.configureProject("target/test-classes/test_retry2.ant"); }

    @Test public void
    test1() {
        long start = System.currentTimeMillis();
        try {
            this.executeTarget("test1");
            TestCase.fail();
        } catch (BuildException be) {
            TestCase.assertEquals("Giving up after 2 retries: Nested task failed", be.getMessage());

            ExitStatusException ese = (ExitStatusException) be.getCause();
            TestCase.assertEquals(77, ese.getStatus());
        }
        long took = System.currentTimeMillis() - start;
        Retry2Test.assertMatches((
            ""
            + "\\S+test_retry2\\.ant:\\d+: "
            + "Nested task failed"
            + "Will retry in 1s \\(on 2012-01-01 00:00:01.000\\)..."
            + "Retrying..."
            + "\\S+test_retry2\\.ant:\\d+: "
            + "Nested task failed"
            + "Will retry in 2s \\(on 2012-01-01 00:00:02.000\\)..."
            + "Retrying..."
            + "\\S+test_retry2\\.ant:\\d+: "
            + "Nested task failed"
        ), this.getLog());
        TestCase.assertTrue(took + "ms", took >= 2900 && took <= 3300);
    }

    private static void
    assertMatches(String regex, @Nullable String actual) {
        Retry2Test.assertMatches(null, regex, actual);
    }

    private static void
    assertMatches(@Nullable String message, String regex, @Nullable String actual) {
        if (actual != null && Pattern.matches(regex, actual)) return;
        String cleanMessage = (message == null) ? "" : message;
        throw new ComparisonFailure(cleanMessage, regex, actual);
    }
}
