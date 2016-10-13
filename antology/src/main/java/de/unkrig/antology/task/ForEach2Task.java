
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

package de.unkrig.antology.task;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.URLProvider;

import de.unkrig.antology.task.BreakTask.BreakException;
import de.unkrig.antology.task.ContinueTask.ContinueException;
import de.unkrig.antology.util.Logging;
import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.commons.util.time.Duration;
import de.unkrig.commons.util.time.PointOfTime;

/***
 * An enhanced version of the <a href="http://ant-contrib.sourceforge.net/tasks/tasks/for.html">&lt;for&gt;</a> task of
 * <a href="http://ant-contrib.sourceforge.net">ant-contrib</a> which supports {@link BreakTask &lt;break>} and {@link
 * ContinueTask &lt;continue>} subtasks.
 * <p>
 *   It iterates over
 * </p>
 * <ul>
 *   <li>a {@linkplain #setList(String) list of strings}, or
 *   <li>a {@linkplain #setCount(int) sequence of integers}, starting with "1", or
 *   <li>the {@linkplain #add(ResourceCollection) elements of a resource collection}, or
 *   <li>the {@linkplain #addConfiguredKeysOf(MapElement) keys of a map}, or
 *   <li>the {@linkplain #addConfiguredValuesOf(MapElement) values of a map}, or
 *   <li>the {@linkplain #add(Iterable) elements of an Iterable}, or
 *   <li>an infinite sequence of integers, starting with "0" (the default if none of the above is configured),
 * </ul>
 * <p>
 *   and executes the configured {@link #createSequential()} for each element. The behavior for the case that the
 *   {@link Sequential} fails is configured by the {@link #setKeepGoing(boolean) keepGoing} attribute.
 * </p>
 * <p>
 *   The dynamic attribute designated by {@link #setParam(String)} reflects the value of the current element of the
 *   iteration.
 * </p>
 * <h3>Usage examples:</h3>
 * <pre>{@code
 *   <forEach2 list="a,b,c" param="s">
 *       <sequential>
 *           <echo message="@{s}" />
 *       </sequential>
 *   </forEach2>
 *
 *   <forEach2 count=3 param="i">
 *       <sequential>
 *           <echo message="@{i}" />
 *       </sequential>
 *   </forEach2>
 * }</pre>
 * <p>
 *   If the {@link #setMessage(String)} attribute is configured, the task reports the throughput of each iteration,
 *   and, if {@link #setShowEta(boolean) showEta="true"}, it computes and reports the estimated remaining time after
 *   each iteration. The messages that are logged before and after each element are the same as for the {@link
 *   ThroughputTask &lt;throughput>} task, however, the {@code (previous|current|remaining)(Quantity|Duration)} are not
 *   configured through properties, but reflect measured values:
 * </p>
 * <table border="1" rules="all">
 *   <tr>
 *     <td>{@code previousQuantity}</td>
 *     <td>
 *       The number of elements processed <em>before</em> the current element, or, for resource collections, the total
 *       of the sizes of the resources processed <em>before</em> the current resource
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code previousDuration}</td>
 *     <td>
 *       The duration it took to process the elements <em>before</em> the current element
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code currentQuantity}</td>
 *     <td>
 *       1, or, for resource collections, the size of the current resource
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code currentDuration}</td>
 *     <td>
 *       The duration it took to process the current element
 *     </td>
 *   </tr>
 * </table>
 */
public
class ForEach2Task extends Task {

    private static final Iterable<?> DEFAULT_ITERABLE = ForEach2Task.infinite();

    private
    interface PrePost {

        /** Invoked BEFORE each element. */
        void pre(Object element);

        /** Invoked AFTER each element. */
        void post(Object element);
    }

    public
    ForEach2Task() {}

    // -------------------------- CONFIGURATION --------------------------

    /**
     * The default value for the {@link #setDelimiter(String)}.
     */
    public static final String DEFAULT_DELIMITER = ",";

    @Nullable private String      param;
    private String                delimiter = ForEach2Task.DEFAULT_DELIMITER;
    private Iterable<?>           iterable  = ForEach2Task.DEFAULT_ITERABLE;
    @Nullable private MacroDef    macroDef;
    private boolean               keepGoing;
    @Nullable private String      message;
    @Nullable private String      quantityUnit;
    private boolean               showEta;
    @Nullable private PointOfTime currentBeginning;
    @Nullable private Duration    currentDuration;
    @Nullable private Long        currentQuantity;
    @Nullable private PointOfTime remainingBeginning;

    /**
     * The elements to iterate are the given list, split at the configured {@link #setDelimiter(String) delimiter}.
     */
    public void
    setList(final String values) {
        this.setIterable(new AbstractCollection<String>() {

            @SuppressWarnings({ "unchecked", "rawtypes" }) @Override public Iterator<String>
            iterator() { return (Iterator) this.getDelegate().iterator(); }

            @Override public int
            size() { return this.getDelegate().size(); }

            private List<Object>
            getDelegate() {
                List<Object> result = this.delegate;
                if (result == null) {
                    this.delegate = (
                        result = Collections.list(new StringTokenizer(values, ForEach2Task.this.delimiter))
                    );
                }
                return result;
            }
            @Nullable List<Object> delegate;
        });
    }

    /**
     * Separates the elements within the {@link #setList(String) list}.
     *
     * @ant.defaultValue {@value #DEFAULT_DELIMITER}
     */
    public void
    setDelimiter(String delimiterCharacters) { this.delimiter = delimiterCharacters; }

    /**
     * Iterate over the values "1", "2", "3", ... "<var>N</var>".
     */
    public void
    setCount(final int n) {
        this.setIterable(new Iterable<Object>() {

            @Override public Iterator<Object>
            iterator() {
                return new Iterator<Object>() {

                    /** Counts from 1 to 'value'. */
                    int idx = 1;

                    @Override public boolean
                    hasNext() {
                        return this.idx <= n;
                    }

                    @Override public Object
                    next() {
                        if (this.idx > n) throw new NoSuchElementException();
                        return this.idx++;
                    }

                    @Override public void
                    remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }
        });
    }

    /**
     * The name of the dynamic attribute that refers to the current element.
     */
    public void
    setParam(String dynamicAttributeName) { this.param = dynamicAttributeName; }

    /**
     * Iff {@code true}, execution will not fail immediately if one of the subtasks fails. Instead, the iteration will
     * continue with the following elements and execution will fail after the iteration is complete.
     */
    public void
    setKeepGoing(boolean value) { this.keepGoing = value; }

    /**
     * If set, then messages are logged before and after execution of the nested tasks (see the {@link ThroughputTask
     * &lt;throughput>} task).
     * <p>
     *   Iff the iterated elements are resource collections, then the reported quantities are the sizes of the
     *   resources, and the quantity unit is "bytes", otherwise the quantities are the element counts, and the quantity
     *   unit is "elements".
     * </p>
     */
    public void
    setMessage(String text) { this.message = text; }

    /**
     * Iff set, and {@link #setMessage(String)} is also set, then this is the 'unit' that the throughput will be
     * reported in, e.g. "bytes". If the related quantity is "1", then the quantity unit is automatically
     * 'singularized', which means that a trailing 's' is chopped off.
     * <p>
     *   The default is {@code "elements"}; for resources {@code "bytes"}.
     * </p>
     */
    public void
    setQuantityUnit(@Nullable String unitName) { this.quantityUnit = unitName; }

    /**
     * Configures, together with {@link #setMessage(String)}, the messages to be logged before and after execution of
     * the nested tasks.
     *
     * @see ThroughputTask#setShowEta(boolean)
     */
    public void
    setShowEta(boolean value) { this.showEta = value; }

    /**
     * Use the given point-of-time as the "time of the beginning of the current interval" instead of the current time.
     *
     * @deprecated For testing only
     */
    @Deprecated public void
    setCurrentBeginning(PointOfTime pointOfTime) { this.currentBeginning = pointOfTime; }

    /**
     * Use the given duration as the "the duration of the current interval" instead of the <em>real</em> duration.
     *
     * @deprecated For testing only
     */
    @Deprecated public void
    setCurrentDuration(Duration duration) { this.currentDuration = duration; }

    /**
     * Use the given values as the "quantity processed in the current interval" instead of the <em>real</em> quantity.
     *
     * @deprecated For testing only
     */
    @Deprecated public void
    setCurrentQuantity(long n) { this.currentQuantity = n; }

    /**
     * Use the given point-of-time as the "time of the beginning of the remaining interval" instead of the current time.
     *
     * @deprecated For testing only
     */
    @Deprecated public void
    setRemainingBeginning(PointOfTime pointOfTime) { this.remainingBeginning = pointOfTime; }

    /**
     * The elements to iterate are the resources contained in this {@link ResourceCollection resource collection}.
     * <p>
     *   For file resources, the {@link #setParam(String) param} reflects the path of each file;
     *   for URL resources, it reflects the URL;
     *   for other resources (e.g. the elements of a {@link PropertySet &lt;propertyset&gt;}), it reflects the
     *   <em>name</em> of each resource.
     * </p>
     */
    public void
    add(final ResourceCollection resourceCollection) {

        this.setIterable(new AbstractCollection<Resource>() {

            @Override public Iterator<Resource>
            iterator() {
                @SuppressWarnings("unchecked") Iterator<Resource> it = resourceCollection.iterator();
                return it;
            }

            @Override public int
            size() {
                return resourceCollection.size();
            }
        });
    }

    /** The elements to iterate are the <em>keys</em> of a {@link Map java.util.Map}. */
    public void
    addConfiguredKeysOf(MapElement mapSubelement) { this.setIterable(mapSubelement.getMap().keySet()); }

    /** The elements to iterate are the <em>values</em> of a {@link Map java.util.Map}. */
    public void
    addConfiguredValuesOf(MapElement mapSubelement) { this.setIterable(mapSubelement.getMap().values()); }

    /** The elements to iterate are those of this {@link Iterable java.lang.Iterable}. */
    public void
    add(Iterable<Object> iterable) { this.setIterable(iterable); }

    /**
     * Container for the subtasks to execute for each iteration.
     */
    public TaskContainer
    createSequential() {

        if (this.macroDef != null) throw new BuildException("Exactly one <sequential> must be configured");

        MacroDef macroDef = (this.macroDef = new MacroDef());
        macroDef.setProject(this.getProject());
        return macroDef.createSequential();
    }

    public static final
    class MapElement {

        @Nullable private Map<Object, Object> map;

        /** The {@link Map java.util.Map} to use. */
        public void
        add(Map<Object, Object> map) {
            if (this.map != null) throw new BuildException("Must not configure more than one subelement");
            this.map = map;
        }

        private Map<Object, Object>
        getMap() {
            Map<Object, Object> map = this.map;
            if (map == null) throw new BuildException("Map subelement missing");
            return map;
        }
    }

    // -------------------------- END OF CONFIGURATION --------------------------

    @Override public void
    execute() {

        MacroDef macroDef = this.macroDef;
        if (macroDef == null) throw new BuildException("Subelement '<sequential>' is not configured");

        if (macroDef.getAttributes().isEmpty() && this.param != null) {
            MacroDef.Attribute attribute = new MacroDef.Attribute();
            attribute.setName(this.param);
            macroDef.addConfiguredAttribute(attribute);
        }

        final String message = this.message;

        PrePost prePost;
        if (message == null) {
            prePost = new PrePost() {
                @Override public void pre(Object element)  {}
                @Override public void post(Object element) {}
            };
        } else {

            // Prepare throughput reporting.
            final long    totalQuantity; // -1 == unknown
            final boolean elementsAreResources;

            if (this.iterable instanceof Collection) {

                Iterator<?> it = this.iterable.iterator();
                if (it.hasNext() && it.next() instanceof Resource) {
                    long totalSize = 0;
                    for (Object element : this.iterable) {
                        totalSize += ((Resource) element).getSize();
                    }
                    totalQuantity        = totalSize;
                    elementsAreResources = true;
                } else {
                    totalQuantity        = ((Collection<?>) this.iterable).size();
                    elementsAreResources = false;
                }
            } else {
                totalQuantity        = -1;
                elementsAreResources = false;
            }

            final String quantityUnit = ForEach2Task.or(
                this.quantityUnit,
                elementsAreResources ? "bytes" : "elements"
            );

            // Iteration state.
            final long[]     previousQuantity = new long[1];
            final Duration[] previousDuration = { new Duration(0) };

            prePost = new PrePost() {

                @Nullable private PointOfTime currentBeginning;
                @Nullable private String      originalMessagePrefix;

                @Override public void
                pre(Object element) {

                    String message2 = message;
                    if (ForEach2Task.this.param != null) {

                        // 'PropertyResource.toString()' (created by 'PropertySet', a.k.a. '<propertyset>'), and maybe
                        // other 'Resource's return 'this.getValue()', but we want 'this.getName()'.
                        String
                        token = element instanceof Resource ? ((Resource) element).getName() : element.toString();

                        message2 = message.replace("@{" + ForEach2Task.this.param + "}", token);
                    }

                    long currentQuantity = ForEach2Task.this.quantityOfElement(element, elementsAreResources);

                    PointOfTime currentBeginning = (
                        this.currentBeginning = ForEach2Task.or(ForEach2Task.this.currentBeginning, new PointOfTime())
                    );

                    Double remainingQuantity = (
                        totalQuantity == -1
                        ? null
                        : (double) (totalQuantity - previousQuantity[0] - currentQuantity)
                    );

                    ForEach2Task.this.log(ThroughputTask.composeBeforeMessage(
                        message2,                     // message
                        (double) previousQuantity[0], // previousQuantity
                        previousDuration[0],          // previousDuration
                        (double) currentQuantity,     // currentQuantity
                        currentBeginning,             // currentBeginning
                        remainingQuantity,            // remainingQuantity
                        ForEach2Task.this.showEta,    // showEta
                        quantityUnit                  // quantityUnit
                    ));

                    {
                        String currentMessagePrefix = Logging.getLogMessagePrefix(ForEach2Task.this.getProject());
                        this.originalMessagePrefix = currentMessagePrefix;
                        Logging.setLogMessagePrefix(ForEach2Task.this.getProject(), currentMessagePrefix + "| ");
                    }
                }

                @Override public void
                post(Object element) {

                    {
                        String originalMessagePrefix = this.originalMessagePrefix;
                        assert originalMessagePrefix != null;
                        Logging.setLogMessagePrefix(ForEach2Task.this.getProject(), originalMessagePrefix);
                    }

                    PointOfTime remainingBeginning = ForEach2Task.this.remainingBeginning;
                    if (remainingBeginning == null) remainingBeginning = new PointOfTime();

                    long currentQuantity = ForEach2Task.this.quantityOfElement(element, elementsAreResources);

                    PointOfTime currentBeginning = this.currentBeginning;
                    assert currentBeginning != null;

                    Duration currentDuration = ForEach2Task.or(
                        ForEach2Task.this.currentDuration,
                        remainingBeginning.subtract(currentBeginning)
                    );

                    Double remainingQuantity = (
                        totalQuantity == -1
                        ? null
                        : (double) (totalQuantity - previousQuantity[0] - currentQuantity)
                    );

                    ForEach2Task.this.log(ThroughputTask.composeAfterMessage(
                        (double) previousQuantity[0],  // previousQuantity
                        previousDuration[0],           // previousDuration
                        (double) currentQuantity,      // currentQuantity
                        currentDuration,               // currentDuration
                        remainingQuantity,             // remainingQuantity
                        remainingBeginning,            // remainingBeginning
                        ForEach2Task.this.showEta,     // showEta
                        quantityUnit                   // quantityUnit
                    ));

                    previousDuration[0] = previousDuration[0].add(currentDuration);
                    previousQuantity[0] += currentQuantity;
                }
            };
        }

        int errorCount = 0;

        ITERATION:
        for (Object element : this.iterable) {
            assert element != null;

            prePost.pre(element);

            MacroInstance instance = new MacroInstance();
            instance.setProject(this.getProject());
            instance.setOwningTarget(this.getOwningTarget());
            instance.setMacroDef(macroDef);

            {
                String param = this.param;
                if (param != null) {

                    // 'PropertyResource.toString()' (created by 'PropertySet', a.k.a. '<propertyset>'), and maybe other
                    // 'Resource's return 'this.getValue()', but we want 'this.getName()'.
                    String token = (
                        element instanceof FileProvider ? ((FileProvider) element).getFile().getPath() :
                        element instanceof URLProvider  ? ((URLProvider)  element).getURL().toString() :
                        element instanceof Resource     ? ((Resource)     element).getName()           :
                        element.toString()
                    );

                    instance.setDynamicAttribute(param.toLowerCase(), token);
                }
            }

            try {
                instance.execute();
            } catch (BreakException be) {
                break ITERATION;
            } catch (ContinueException ce) {
                continue ITERATION;
            } catch (BuildException be) {
                if (BreakException.isWrappedBy(be)) break ITERATION;
                if (ContinueException.isWrappedBy(be)) continue ITERATION;

                int status = be instanceof ExitStatusException ? ((ExitStatusException) be).getStatus() : -1;
                if (status != 0) {

                    if (!this.keepGoing) throw be;

                    // 'PropertyResource.toString()' (created by 'PropertySet', a.k.a. '<propertyset>'), and maybe other
                    // 'Resource's return 'this.getValue()', but we want 'this.getName()'.
                    String token = element instanceof Resource ? ((Resource) element).getName() : element.toString();

                    this.log(token + ": " + be.getMessage(), Project.MSG_ERR);
                    errorCount++;
                }
            } finally {
                prePost.post(element);
            }
        }

        if (errorCount > 0) {
            throw new BuildException(errorCount + " iterations failed.");
        }
    }

    private void
    setIterable(Iterable<?> iterable) {

        if (this.iterable != ForEach2Task.DEFAULT_ITERABLE) {
            throw new BuildException(
                "At most one of 'list=\"...\"', 'count=\"...\"', resource collection subelement, '<keysOf>', "
                + "'<valuesOf>' and 'Iterable' must be configured"
            );
        }

        this.iterable = iterable;
    }

    private long
    quantityOfElement(Object element, final boolean elementIsResource) {

        Long tmp = ForEach2Task.this.currentQuantity;
        return (
            tmp != null       ? tmp :
            elementIsResource ? ((Resource) element).getSize() :
            1
        );
    }

    private static Iterable<Integer>
    infinite() {
        return new Iterable<Integer>() {

            @Override public Iterator<Integer>
            iterator() {

                return new Iterator<Integer>() {
                    private int idx;
                    @Override public boolean hasNext() { return true; }
                    @Override public Integer next()    { return ++this.idx; }
                    @Override public void    remove()  { throw new UnsupportedOperationException("remove"); }
                };
            }
        };
    }

    private static <T> T
    or(@Nullable T x, T y) {
        return x != null ? x : y;
    }
}
