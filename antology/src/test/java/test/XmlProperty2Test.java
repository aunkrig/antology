
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.unkrig.commons.junit4.AssertString;

//CHECKSTYLE JavadocMethod|JavadocVariable:OFF

/**
 * Tests for the {@link de.unkrig.antcontrib.task.ForEach2Task}.
 */
public
class XmlProperty2Test {

    @Rule public BuildFileRule
    rule = new BuildFileRule();

    @Before public void
    setUp() {
        this.rule.configureProject("target/test-classes/test_xmlProperty2.ant");
    }

    @Test public void
    test1() {

        this.rule.executeTarget("test1");

        AssertString.assertContains("prefix.0.project.$$=",                          this.rule.getLog());
        AssertString.assertContains("prefix.0.project._name=prj1",                   this.rule.getLog());
        AssertString.assertContains("prefix.0.project.0.$=\\n\\t",                   this.rule.getLog());
        AssertString.assertContains("prefix.0.project.1.target.$$=",                 this.rule.getLog());
        AssertString.assertContains("prefix.0.project.1.target._name=trg1",          this.rule.getLog());
        AssertString.assertContains("prefix.0.project.1.target.0.$=\\n\\t\\t",       this.rule.getLog());
        AssertString.assertContains("prefix.0.project.1.target.1.echo.$$=",          this.rule.getLog());
        AssertString.assertContains("prefix.0.project.1.target.1.echo._message=msg", this.rule.getLog());
        AssertString.assertContains("prefix.0.project.1.target.2.$=\\n\\t",          this.rule.getLog());
        AssertString.assertContains("prefix.0.project.2.$=\\n",                      this.rule.getLog());
    }

    @Test public void
    test2() {

        this.rule.executeTarget("test2");

        AssertString.assertContains((
            ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<!--LEADING COMMENT-->"
            + "<project name=\"prj1\">\n"
            + "\t<target name=\"trg1\">\n"
            + "\t\t<echo message=\"msg\"/><echo/>\n"
            + "\t\t<!-- MY COMMENT -->\n"
            + "\t\t<![CDATA[ MY CDATA ]]>\n"
            + "\t\t<?MY PROCESSING  INSTRUCTION  ?>\n"
//            + "\t\t&amp;&lt;\n"
            + "\t</target>\n"
            + "</project>"
            + "<!--TRAILING COMMENT-->"
        ), this.rule.getOutput());
    }

    @Test public void
    test3() {

        this.rule.executeTarget("test3");

        AssertString.assertContains("prefix.0.\\#=\\ MY COMMENT ",                   this.rule.getLog());
        AssertString.assertContains("prefix.1.project.$$=bla bla bla",               this.rule.getLog());
        AssertString.assertContains("prefix.1.project._name=prj1",                   this.rule.getLog());
        AssertString.assertContains("prefix.1.project.0.$=\\n\\tbla ",               this.rule.getLog());
        AssertString.assertContains("prefix.1.project.1.\\!=bla",                    this.rule.getLog());
        AssertString.assertContains("prefix.1.project.2.$=\\ bla\\n\\t",             this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.$$=&<",               this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target._name=trg1",          this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.0.$=\\n\\t\\t",       this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.1.echo.$$=",          this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.1.echo._message=msg", this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.2.$=\\n\\t\\t",       this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.3.$=&",               this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.4.$=<",               this.rule.getLog());
        AssertString.assertContains("prefix.1.project.3.target.5.$=\\n\\t",          this.rule.getLog());
        AssertString.assertContains("prefix.1.project.4.$=\\n",                      this.rule.getLog());
        AssertString.assertContains("prefix.2.\\#=ONE MORE",                         this.rule.getLog());
    }
}
