
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

package test;

import java.util.regex.Pattern;

import org.apache.tools.ant.BuildFileTest;
import org.junit.Test;

import de.unkrig.commons.nullanalysis.Nullable;
import junit.framework.TestCase;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.FollowTask}.
 */
public
class NslookupTest extends BuildFileTest {

    @Override public void
    setUp() { this.configureProject("target/test-classes/test_nslookup.ant"); }

    @Test public void
    test1() {
        this.executeTarget("test1");
        this.assertPropertyEquals("addresses",         "127.0.0.1,0:0:0:0:0:0:0:1");
        this.assertPropertyEquals("address",           "127.0.0.1");
        this.assertPropertyEquals("canonicalHostName", "127.0.0.1");
        this.assertPropertyEquals("hostName",          "localhost");
    }

    @Test public void
    test2() {
        this.executeTarget("test2");

        final String ipV4Address     = "(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})";
        final String ipV6Address     = "(?:\\p{XDigit}{1,4}(?::\\p{XDigit}{1,4}){7})";
        final String ipV4OrV6Address = "(?:" + ipV4Address + "|" + ipV6Address + ")";

        this.assertPropertyMatches("addresses",         ipV4OrV6Address + "(?:," + ipV4OrV6Address + ")*");
        this.assertPropertyMatches("address",           ipV4OrV6Address);
        this.assertPropertyMatches("canonicalHostName", "[\\w\\-.]+");
        this.assertPropertyEquals("hostName",           "www.google.com");
    }

    private void
    assertPropertyMatches(String propertyName, String regex) {
        NslookupTest.assertMatches(regex, this.getProject().getProperty(propertyName));
    }

    private static void
    assertMatches(String regex, @Nullable String actual) {
        if (actual == null || !Pattern.matches(regex, actual)) {
            TestCase.fail("expected:<" + regex + "> but was <" + actual + ">");
        }
    }
}
