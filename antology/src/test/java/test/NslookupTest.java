
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

import org.apache.tools.ant.BuildFileRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.unkrig.commons.junit4.AssertRegex;

// CHECKSTYLE JavadocMethod|JavadocVariable:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.FollowTask}.
 */
public
class NslookupTest {

    @Rule public BuildFileRule
    rule = new BuildFileRule();

    @Before public void
    setUp() {
        this.rule.configureProject("target/test-classes/test_nslookup.ant");
    }

    @Test public void
    test1() {
        this.rule.executeTarget("test1");
        Assert.assertEquals("127.0.0.1,0:0:0:0:0:0:0:1", this.rule.getProject().getProperty("addresses"));
        Assert.assertEquals("127.0.0.1",                 this.rule.getProject().getProperty("address"));
        Assert.assertEquals("127.0.0.1",                 this.rule.getProject().getProperty("canonicalHostName"));
        Assert.assertEquals("localhost",                 this.rule.getProject().getProperty("hostName"));
    }

    @Test public void
    test2() {
        this.rule.executeTarget("test2");

        final String ipV4Address     = "(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})";
        final String ipV6Address     = "(?:\\p{XDigit}{1,4}(?::\\p{XDigit}{1,4}){7})";
        final String ipV4OrV6Address = "(?:" + ipV4Address + "|" + ipV6Address + ")";

        // SUPPRESS CHECKSTYLE LineLength:4
        AssertRegex.assertMatches(ipV4OrV6Address + "(?:," + ipV4OrV6Address + ")*", this.rule.getProject().getProperty("addresses"));
        AssertRegex.assertMatches(ipV4OrV6Address,                                   this.rule.getProject().getProperty("address"));
        AssertRegex.assertMatches("[\\w\\-.]+",                                      this.rule.getProject().getProperty("canonicalHostName"));
        Assert.assertEquals("www.google.com",                                        this.rule.getProject().getProperty("hostName"));
    }
}
