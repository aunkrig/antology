
/*
 * antology - Some contributions to APACHE ANT
 *
 * Copyright (c) 2014, Arno Unkrig
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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.ChainableReader;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Prints dots (".") to STDERR as data is read through it. This implements a kind of "progress monitoring" for slow
 * tasks that read some data.
 */
@NotNullByDefault(false) public
class ProgressFilter extends ProjectComponent implements ChainableReader {

    // ---------------- Implementation of the weird ANT FilterReader pattern ----------------

    @Override public Reader
    chain(Reader reader) {

        return new FilterReader(reader) {

            @Override public int
            read() throws IOException {
                int c = this.in.read();
                if (c != -1) ProgressFilter.this.reportBytes(1);
                return c;
            }

            @Override public int
            read(char[] cbuf, int off, int len) throws IOException {
                int n = this.in.read(cbuf, off, len);
                if (n != -1) ProgressFilter.this.reportBytes(n);
                return n;
            }

            @Override public long
            skip(long n) throws IOException {
                long skipped = this.in.skip(n);
                ProgressFilter.this.reportBytes(skipped);
                return skipped;
            }

            @Override public void
            close() throws IOException {
                ProgressFilter.this.reportEndOfProgress();
                this.in.close();
            }
        };
    }

    // ---------------- ANT attribute setters. ----------------

    /**
     * If {@code true}, then the initial dot printing frequency will degrade so that no more than 80 dots will ever be
     * printed.
     *
     * @ant.defaultValue true
     */
    public void setExponential(boolean value) { this.exponential = value; }
    private boolean exponential = true;

    /**
     * That many bytes must be processed before another dot is printed.
     *
     * @ant.defaultValue 1024
     */
    public void setBytesPerTick(int n) { this.bytesPerTick = n; }
    private int bytesPerTick = 1024;

    // ------------------- IMPLEMENTATION ----------------------

    /**
     * @param n The (positive) number of bytes that were just processed
     */
    private synchronized void
    reportBytes(long n) {

        this.totalBytes += n;

        // Convert the bytes total into ticks.
        int ticks = (int) (this.totalBytes / this.bytesPerTick);

        this.setProgress(ticks);
    }
    private long totalBytes;

    // -----------------------------

    /**
     * Prints a line break, so the next output will appear on a new line. Avoids printing multiple line breaks in a row.
     */
    private void
    reportEndOfProgress() {
        if (this.dotsPending) {
            this.dotsPending = false;
            System.err.println();
            System.err.flush();
        }
    }
    private boolean dotsPending;

    /**
     * Prints N dots to STDERR iff {@code ticks} is N larger than at the previous invocation (but also honors the
     * 'exponential' attribute).
     */
    private synchronized void
    setProgress(int ticks) {

        int logicalTicks1 = this.previousLogicalTicks;
        int logicalTicks2 = (this.previousLogicalTicks = this.logicalizeTicks(ticks));

        if (logicalTicks2 > logicalTicks1) {

            for (int i = logicalTicks1; i < logicalTicks2; i++) System.err.print('.');
            System.err.flush();

            this.dotsPending = true;
        }
    }
    private int previousLogicalTicks;

    /**
     * Returns {@code physicalTicks} iff not {@link #setExponential(boolean) exponential}.
     * <p>
     * Otherwise, for small {@code physicalTicks}, a value slightly smaller than {@code physicalTicks} is returned;
     * for larger {@code physicalTicks} a value no greated than {@value #ASYMPTOTE}.
     */
    private int
    logicalizeTicks(int physicalTicks) {
        return this.exponential ? (
            ProgressFilter.ASYMPTOTE
            - (int) (ProgressFilter.ASYMPTOTE * Math.exp(physicalTicks * (-1.0 / ProgressFilter.ASYMPTOTE)))
        ) : physicalTicks;
    }
    private static final int ASYMPTOTE = 80;
}
