
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2015, Arno Unkrig
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
import org.junit.Rule;
import org.junit.Test;

import de.unkrig.commons.junit4.AssertString;

// SUPPRESS CHECKSTYLE Javadoc:9999

/**
 * Tests for the {@link de.unkrig.antology.task.UrlConnectionTask}.
 */
public
class JsonPropertyTest {

    @Rule public BuildFileRule
    rule = new BuildFileRule();

    @Before public void
    setUp() {
        this.rule.configureProject("target/test-classes/test_jsonProperty.ant");
    }

    @Test public void
    testJsonProperty() {
        this.rule.executeTarget("testJsonProperty");
        AssertString.assertContains((
            ""
            + "foo.a=3\r\n"
            + "foo.b=false\r\n"
            + "foo.c=null\r\n"
            + "foo.d.0=1\r\n"
            + "foo.d.1=2\r\n"
            + "foo.d.2=3\r\n"
            + "foo.e.f=7.3"
        ), this.rule.getLog());
    }
}


