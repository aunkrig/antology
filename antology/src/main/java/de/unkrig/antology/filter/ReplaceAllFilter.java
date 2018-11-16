
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
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.ChainableReader;

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
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
 */
@NotNullByDefault(false) public
class ReplaceAllFilter extends ProjectComponent implements ChainableReader {

	public static
	class PatternElement {

		@Nullable private Pattern pattern;
		@Nullable private String  replacementString;

		public void
		setPattern(String pattern) {
			if (this.pattern != null) {
				throw new BuildException("\"pattern=...\" and \"<pattern>...</pattern>\" are mutually exclusive");
			}
			this.pattern = Pattern.compile(pattern);
		}

		public void
		setReplacementString(String replacementString) { this.replacementString = replacementString; }

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
	    	Pattern pattern           = this.pattern;
	    	String  replacementString = this.replacementString;

	    	if (pattern != null || replacementString != null) {
	    		reader = ReplaceAllFilter.replaceAll(reader, pattern, replacementString);
	    	}
    	}

    	for (PatternElement pe : this.patterns) {
			reader = ReplaceAllFilter.replaceAll(reader, pe.pattern, pe.replacementString);
		}

    	return reader;
    }

	private static Reader
	replaceAll(Reader reader, @Nullable Pattern pattern, @Nullable String replacementString) {

		if (pattern           == null) throw new BuildException("Pattern missing");
		if (replacementString == null) throw new BuildException("Replacement string missing");

		return PatternUtil.replaceAllFilterReader(
			reader,
			pattern,
			PatternUtil.<IOException>replacementStringMatchReplacer(replacementString)
		);
	}

    // ---------------- ANT attribute setters. ----------------

    public void setPattern(String pattern) { this.pattern = Pattern.compile(pattern); }
    @Nullable private Pattern pattern;

    public void setReplacementString(String replacementString) { this.replacementString = replacementString; }
    @Nullable private String replacementString;

    public void
    addConfiguredPattern(PatternElement element) {

    	if (element.pattern           == null) throw new BuildException("Pattern missing");
    	if (element.replacementString == null) throw new BuildException("Replacement string missing");

    	this.patterns.add(element);
	}
    private List<PatternElement> patterns = new ArrayList<PatternElement>();
}
