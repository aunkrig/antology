
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

package de.unkrig.antology.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

import de.unkrig.commons.nullanalysis.NotNullByDefault;

/**
 * Utility methods related to ANT logging.
 */
public final
class Logging {

    private Logging() {}

    @NotNullByDefault(false) private static
    class FilterBuildListener implements BuildListener {

        private final BuildListener delegate;

        FilterBuildListener(BuildListener delegate) { this.delegate = delegate; }

        @Override public void buildStarted(BuildEvent event)   { this.delegate.buildStarted(event);   }
        @Override public void buildFinished(BuildEvent event)  { this.delegate.buildFinished(event);  }
        @Override public void targetStarted(BuildEvent event)  { this.delegate.targetStarted(event);  }
        @Override public void targetFinished(BuildEvent event) { this.delegate.targetFinished(event); }
        @Override public void taskStarted(BuildEvent event)    { this.delegate.taskStarted(event);    }
        @Override public void taskFinished(BuildEvent event)   { this.delegate.taskFinished(event);   }
        @Override public void messageLogged(BuildEvent event)  { this.delegate.messageLogged(event);  }
    }

    private static final ThreadLocal<Map<Project, String>>
    PREFIX = new ThreadLocal<Map<Project, String>>() {
        @Override protected Map<Project, String> initialValue() { return new HashMap<Project, String>(); }
    };

    @NotNullByDefault(false) private static
    class IndentingBuildListener extends FilterBuildListener {

        IndentingBuildListener(BuildListener delegate) { super(delegate); }

        @Override public void
        messageLogged(BuildEvent event) {

            String prefix = Logging.PREFIX.get().get(event.getProject());

            if (prefix != null) {
                event.setMessage(prefix + event.getMessage(), event.getPriority());
            }

            super.messageLogged(event);
        }
    }

    /**
     * Wraps all build listeners of a given <var>project</var> such that log messages appear with the given
     * <var>prefix</var>.
     * <p>
     *   The typical usage pattern is:
     * </p>
     * <pre>
     *     public
     *     class MyTask extends Task {
     *
     *         public void execute() {
     *
     *             String originalMessagePrefix = Logging.getLogMessagePrefix(this.getProject());
     *             try {
     *                 Logging.setLogMessagePrefix(this.getProject(), originalMessagePrefix + "  ");
     *
     *                 // Do the 'real' work...
     *
     *             } finally {
     *                 Logging.setLogMessagePrefix(this.getProject(), originalMessagePrefix);
     *             }
     *         }
     *     }
     * </pre>
     */
    public static void
    setLogMessagePrefix(Project project, String prefix) {

        @SuppressWarnings("unchecked") List<BuildListener> orig = project.getBuildListeners();

        // Verify that all build listeners of the project are wrapped in IndentingBuildListeners.
        for (BuildListener buildListener : orig) {

            if (!(buildListener instanceof IndentingBuildListener)) {

                // At least ONE build listener is NOT wrapped in an IndentingBuildListener.

                // Remove ALL build listeners from the project.
                for (BuildListener bl : orig) project.removeBuildListener(bl);

                // Add them to the project again, but each wrapped with an IndentingBuildListener.
                for (BuildListener bl : orig) {
                    if (!(bl instanceof IndentingBuildListener)) {
                        bl = new IndentingBuildListener(bl);
                    }
                    project.addBuildListener(bl);
                }
                break;
            }
        }

        Logging.PREFIX.get().put(project, prefix);
    }

    /**
     * @see #setLogMessagePrefix(Project, String)
     */
    public static String
    getLogMessagePrefix(Project project) {

        String prefix = Logging.PREFIX.get().get(project);

        return prefix == null ? "" : prefix;
    }
}
