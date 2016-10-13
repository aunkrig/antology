
/*
 * de.unkrig.ant-contrib - Some contributions to APACHE ANT
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

package test;

import org.apache.tools.ant.BuildFileTest;
import org.junit.Test;

import junit.framework.TestCase;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.UrlConnectionTask}.
 */
public
class UrlConnectionTest extends BuildFileTest {

    @Override public void
    setUp() { this.configureProject("urlConnection_test.ant"); }

    @Test public void
    test1() {
        this.executeTarget("test1");
        this.assertOutputContaining(
            "Statusabfrage des Tomcat f" + (char) ('ü' | 0xff00) + "r dal-i.intra.swm.de auf Server svdali03"
        );
    }

    @Test public void
    test2() {
        this.executeTarget("test2");
        TestCase.assertEquals(
            "<h2>Statusabfrage des Tomcat f\uffc3\uffbcr dal-i.intra.swm.de auf Server svdali03 </h2>\n",
            this.getOutput()
        );
    }
}
