
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.unkrig.commons.junit4.AssertString;

// SUPPRESS CHECKSTYLE Javadoc:9999

/**
 * Tests for the {@link de.unkrig.antology.task.UrlConnectionTask}.
 */
public
class UrlConnectionTest {

    @Rule public BuildFileRule
    rule = new BuildFileRule();

    @Before public void
    setUp() {
        this.rule.configureProject("target/test-classes/test_urlConnection.ant");
    }

    @Ignore
    @Test public void
    test1() {
        this.rule.executeTarget("test1");
        AssertString.assertContains("Checking for Tomcat: dal-i.intra.swm.de", this.rule.getOutput());
    }

    @Ignore
    @Test public void
    test2() {
        this.rule.executeTarget("test2");
        AssertString.assertContains("Checking for Tomcat: dal-i.intra.swm.de", this.rule.getOutput());
    }
}
