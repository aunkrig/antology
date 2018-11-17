
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2018, Arno Unkrig
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

package de.unkrig.antology.filter;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.ChainableReader;

import de.unkrig.commons.lang.protocol.FunctionWhichThrows;
import de.unkrig.commons.lang.protocol.Functions;
import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.text.parser.ParseException;
import de.unkrig.commons.text.pattern.ExpressionMatchReplacer;
import de.unkrig.commons.text.pattern.PatternUtil;

/**
 * A filter reader that replaces all matches of a regular expression with a replacement string.
 * <p>
 *   The pattern search is stream-oriented, not line-oriented, i.e. matches are found even across line boundaries.
 *   Thus the pattern typically enables
 *   {@link Pattern#MULTILINE &#40;?m)} (multi-line mode)
 *   and/or
 *   {@link Pattern#DOTALL &#40;?s)} (dotall mode).
 * </p>
 * <p>
 *   This filter reader is semantically almost equivalent with
 * </p>
 * <pre>
 *   &lt;tokenfilter>
 *     &lt;filetokenizer />
 *     &lt;replaceregex
 *       pattern="<var>regex</var>"
 *       flags="s"
 *       replace="<var>replacement-string</var>"
 *     />
 *   &lt;/tokenfilter>
 * </pre>
 * <p>
 *   , but is much more efficient in many cases, because the {@code <filetokenizer />} always reads the entire content
 *   into memory, while {@code <replaceAll />} processes the content in a "sliding" manner which requires only very
 *   little memory if the regex matches relative short sequences.
 * </p>
 */
@NotNullByDefault(false) public
class ReplaceAllFilter extends ProjectComponent implements ChainableReader {

	public static
	class PatternElement {

		public void
		setPattern(String pattern) {
			if (this.pattern != null) {
				throw new BuildException("\"pattern=...\" and \"<pattern>...</pattern>\" are mutually exclusive");
			}
			this.pattern = Pattern.compile(pattern);
		}
		@Nullable private Pattern pattern;

	    /**
	     * The "replacement string" to use for the substitution of each match.
	     */
		public void
		setReplacementString(String replacementString) {
		    if (this.matchReplacer != null) throw new BuildException("More than one replacement");
		    this.matchReplacer = PatternUtil.<IOException>replacementStringMatchReplacer(replacementString);
	    }

	    /**
	     * A "replacement expression" to use for the substitution of each match.
	     * <p>
	     *   Usage example:
	     * </p>
	     * <p>
	     *   {@code replacementExpression="m.group.toUpperCase()"}
	     * </p>
	     */
	    public void setReplacementExpression(String replacementExpression) {
	        if (this.matchReplacer != null) throw new BuildException("More than one replacement");
	        try {
                this.matchReplacer = Functions.asFunctionWhichThrows(
                    ExpressionMatchReplacer.parse(replacementExpression)
                );
            } catch (ParseException pe) {
                throw new BuildException(pe);
            }
	    }
	    @Nullable private FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends IOException>
	    matchReplacer;

		public void
		addText(String text) {
			text = text.trim();
			if (text.isEmpty()) return;
			this.setPattern(text);
		}
	}

    // ---------------- Implementation of the weird ANT FilterReader pattern ----------------

    @Override public Reader
    chain(Reader reader) {

    	{
	    	Pattern
	    	pattern = this.pattern;

	    	FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends IOException>
	    	matchReplacer = this.matchReplacer;

	    	if (pattern != null || matchReplacer != null) {
	    		reader = ReplaceAllFilter.replaceAll(reader, pattern, matchReplacer);
	    	}
    	}

    	for (PatternElement pe : this.patterns) {
			reader = ReplaceAllFilter.replaceAll(reader, pe.pattern, pe.matchReplacer);
		}

    	return reader;
    }

	private static Reader
	replaceAll(
	    Reader                                                                                        reader,
	    @Nullable Pattern                                                                             pattern,
	    @Nullable FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends IOException> matchReplacer
    ) {

		if (pattern       == null) throw new BuildException("Pattern missing");
		if (matchReplacer == null) throw new BuildException("Replacement string and expression missing");

		return PatternUtil.replaceAllFilterReader(
			reader,
			pattern,
			matchReplacer
		);
	}

    // ---------------- ANT attribute setters. ----------------

	/**
	 * The pattern to search for in the content.
	 */
    public void setPattern(String pattern) { this.pattern = Pattern.compile(pattern); }
    @Nullable private Pattern pattern;

    /**
     * The "replacement string" to use for the substitution of each match.
     */
    public void setReplacementString(String replacementString) {
        if (this.matchReplacer != null) throw new BuildException("More than one replacement");
        this.matchReplacer = PatternUtil.<IOException>replacementStringMatchReplacer(replacementString);
    }
    private FunctionWhichThrows<? super Matcher, ? extends CharSequence, ? extends IOException> matchReplacer;

    /**
     * A "replacement expression" to use for the substitution of each match.
     * <p>
     *   Usage example:
     * </p>
     * <p>
     *   {@code replacementExpression="m.group.toUpperCase()"}
     * </p>
     */
    public void setReplacementExpression(String replacementExpression) {
        if (this.matchReplacer != null) throw new BuildException("More than one replacement");
        try {
            this.matchReplacer = Functions.asFunctionWhichThrows(ExpressionMatchReplacer.parse(replacementExpression));
        } catch (ParseException pe) {
            throw new BuildException(pe);
        }
    }

    /**
     * Another replacement specification (this filter reader can execute multiple replacements in a row).
     */
    public void
    addConfiguredPattern(PatternElement element) {

    	if (element.pattern       == null) throw new BuildException("Pattern missing");
    	if (element.matchReplacer == null) throw new BuildException("Replacement string and expression missing");

    	this.patterns.add(element);
	}
    private List<PatternElement> patterns = new ArrayList<PatternElement>();
}
