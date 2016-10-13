
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

package de.unkrig.antology.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;

import de.unkrig.antology.type.Subelement;
import de.unkrig.commons.nullanalysis.Nullable;

// CHECKSTYLE TypeName:OFF
// CHECKSTYLE JavadocVariable:OFF
// CHECKSTYLE JavadocMethod:OFF

/**
 * Configures the JDK logging {@code java.util.logging}, also known as "JUL".
 */
public
class LoggingTask extends Task {

    private final List<LoggerElement> loggerElements = new ArrayList<LoggingTask.LoggerElement>();

    // HELPER CLASSES FOR SUBELEMENTS

    /**
     * An instance is obtained by either
     * <ul>
     *   <li>Creating one with the zero-arg constructor</li>
     *   <li>Creating one with a one-string-arg constructor (see {@code argument=...})</li>
     *   <li>Creating one with a two-or-more-string-args constructor (see {@code <argument>})</li>
     * </ul>
     */
    public static
    class InstantiableElement extends ProjectComponent {
        @Nullable String          className;
        final List<String>        arguments  = new ArrayList<String>();
        final Map<String, String> attributes = new HashMap<String, String>();

        /**
         * The qualified name of the Java class to instantiate (mandatory).
         */
        public void
        setClassName(String className) { this.className = className; }

        /**
         * The (single) string argument for the constructor.
         */
        public void
        setArgument(String value) { this.arguments.add(value); }

        /**
         * A string argument for the constructor.
         */
        public void
        addConfiguredArgument(Subelement.Value element) {
            if (element.value == null) {
                throw new IllegalArgumentException("'value' attribute of element '<argument>' is missing");
            }
            this.arguments.add(element.value);
        }

        /**
         * Attributes to set on the object right after construction.
         */
        public void
        addConfiguredAttribute(Subelement.Name_Value element) {
            if (element.name == null) {
                throw new IllegalArgumentException("'name' attribute of element '<attribute>' is missing");
            }
            if (element.value == null) {
                throw new IllegalArgumentException("'value' attribute of element '<attribute>' is missing");
            }
            this.attributes.put(element.name, element.value);
        }

        <T> T
        instantiate(Class<T> superclass) throws Exception {
            if (this.className == null) throw new IllegalArgumentException("'classname' attribute is missing");
            Class<?> clasS = Class.forName(this.className);

            if (!superclass.isAssignableFrom(clasS)) {
                throw new IllegalArgumentException(
                    "'"
                    + clasS.getName()
                    + "' is not a subclass of '"
                    + superclass.getName()
                    + "'"
                );
            }

            // Find the applicable constructor.
            Constructor<?> constructor;
            CONSTRUCTORS:
            {
                for (Constructor<?> c : clasS.getConstructors()) {
                    Class<?>[] parameterTypes = c.getParameterTypes();
                    if (parameterTypes.length == this.arguments.size()) {
                        constructor = c;
                        break CONSTRUCTORS;
                    }
                }
                throw new NoSuchMethodException(this.className + this.arguments);
            }

            // Instantiate the object.
            @SuppressWarnings("unchecked") T
            object = (T) constructor.newInstance(LoggingTask.convert(this.arguments, constructor.getParameterTypes()));

            // Invoke setters for the given attributes.
            for (Entry<String, String> entry : this.attributes.entrySet()) {
                String name  = entry.getKey();
                String value = entry.getValue();

                String methodName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);

                // Find the applicable setter method.
                Method   method;
                Class<?> parameterType;
                METHODS:
                {
                    for (Method m : clasS.getMethods()) {
                        if (!m.getName().equals(methodName)) continue;
                        Class<?>[] parameterTypes = m.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            method        = m;
                            parameterType = parameterTypes[0];
                            break METHODS;
                        }
                    }
                    throw new NoSuchMethodException(this.className + "::" + methodName);
                }

                // Invoke the setter.
                method.invoke(object, LoggingTask.convert(value, parameterType));
            }

            return object;
        }
    }

    /**
     * An XML element that specifies a {@link Handler} object.
     */
    public static
    class HandlerElement extends InstantiableElement {

        @Nullable Filter    filter;
        @Nullable Formatter formatter;

        /**
         * The filter to set on the handler (at most one).
         * <p>
         *   The default is handler-specific, but typically is "none".
         * </p>
         *
         * @see Handler#setFilter(Filter)
         */
        public void
        addConfiguredFilter(InstantiableElement element) throws Exception {

            if (this.filter != null) throw new BuildException("At most one 'filter' subelement allowed");

            this.filter = element.instantiate(Filter.class);
        }

        /**
         * The formatter to use with the handler (at most one).
         * <p>
         *   The default is handler-specific, but typically is a {@link SimpleFormatter}.
         * </p>
         *
         * @see Formatter
         * @see Handler#setFormatter(Formatter)
         */
        public void
        addConfiguredFormatter(InstantiableElement element) throws Exception {

            if (this.formatter != null) throw new BuildException("At most one 'formatter' subelement allowed");

            this.formatter = element.instantiate(Formatter.class);
        }
    }

    /**
     * An XML element that designates a {@link Logger}.
     */
    public static
    class LoggerElement extends ProjectComponent {
        final List<String>  names = new ArrayList<String>();
        public boolean      clearLevel;
        @Nullable Level     level;
        @Nullable Boolean   useParentHandlers;
        public boolean      clearFilter;
        @Nullable Filter    filter;
        boolean             clearHandlers;
        final List<Handler> handlers = new ArrayList<Handler>();

        /**
         * The name of a logger to be configured; typically, but not necessarily, the qualified name of the Java class
         * that will use the logger.
         * <p>
         *   Iff <em>no</em> logger name is specified, then the "root logger" is configured.
         * </p>
         */
        public void
        setName(String name) { this.names.add(name); }

        /**
         * The name of a logger to be configured; typically, but not necessarily, the qualified name of the Java class
         * that will use the logger.
         * <p>
         *   Iff <em>no</em> logger name is specified, then the "root logger" is configured.
         * </p>
         */
        public void
        addConfiguredName(Subelement.Value element) {
            if (element.value == null) {
                throw new IllegalArgumentException("'value' attribute of element '<name>' is missing");
            }
            this.names.add(element.value);
        }

        /**
         * Whether the logger level should be "cleared", so that the level inherited from the ancestor loggers takes
         * effect.
         *
         * @ant.defaultValue false
         */
        public void
        setClearLevel(boolean value) { this.clearLevel = value; }

        /**
         * The logger level to set.
         *
         * @ant.valueExplanation    OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST|ALL|<var>integer-value</var>
         * @see Level#parse(String)
         */
        public void
        setLevel(String nameOrNumber) { this.level = Level.parse(nameOrNumber); }

        /**
         * Whether to configure the logger(s) such that, after logging an event, the event is also logged by the
         * logger's ancestor loggers.
         *
         * @ant.defaultValue                         true
         * @see Logger#setUseParentHandlers(boolean)
         */
        public void
        setUseParentHandlers(Boolean value) { this.useParentHandlers = value; }

        /**
         * Whether to remove any filter that was previously configured for the logger(s).
         *
         * @ant.defaultValue false
         */
        public void
        setClearFilter(boolean value) { this.clearFilter = value; }

        /**
         * The filter to configure for the logger(s); at most one.
         *
         * @see Filter
         */
        public void
        addConfiguredFilter(InstantiableElement element) throws Exception {

            if (this.filter != null) throw new BuildException("At most one 'filter' subelement allowed");

            this.filter = element.instantiate(Filter.class);
        }

        /**
         * Whether to remove any handlers from the logger(s) before adding new ones.
         *
         * @ant.defaultValue false
         */
        public void
        setClearHandlers(boolean value) { this.clearHandlers = value; }

        /**
         * A handler to add to the logger(s).
         *
         * @see Handler
         */
        public void
        addConfiguredHandler(HandlerElement element) throws Exception {

            Handler handler = element.instantiate(Handler.class);

            if (element.filter != null) handler.setFilter(element.filter);

            if (element.formatter != null)  handler.setFormatter(element.formatter);

            this.handlers.add(handler);
        }
    }

    // BEGIN CONFIGURATION SETTERS

    /**
     * One logger configuration.
     */
    public void
    addConfiguredLogger(LoggerElement element) {
        this.loggerElements.add(element);
    }

    // END CONFIGURATION SETTERS

    /**
     * The ANT task "execute" method.
     *
     * @see Task#execute()
     */
    @Override public void
    execute() throws BuildException {
        try {

            for (LoggerElement loggerElement : this.loggerElements) {
                List<String> loggerNames = loggerElement.names;
                if (loggerNames.isEmpty()) loggerNames = Collections.singletonList("");
                for (String name : loggerNames) {
                    Logger logger = Logger.getLogger(name);

                    if (loggerElement.clearFilter)    logger.setFilter(null);
                    if (loggerElement.filter != null) logger.setFilter(loggerElement.filter);

                    if (loggerElement.clearLevel)    logger.setLevel(null);
                    if (loggerElement.level != null) logger.setLevel(loggerElement.level);

                    if (loggerElement.useParentHandlers != null) {
                        logger.setUseParentHandlers(loggerElement.useParentHandlers);
                    }

                    if (loggerElement.clearHandlers) {
                        for (Handler handler : logger.getHandlers()) logger.removeHandler(handler);
                    }
                    for (Handler handler : loggerElement.handlers) logger.addHandler(handler);
                }
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * @return {@code value}, converted to the {@code targetType} using the {@code targetType}'s one-string-parameter
     *         constructor, or the value of the named constant
     */
    @SuppressWarnings("unchecked") private static <T> T
    convert(String value, Class<T> targetType)
    throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        if (targetType == String.class) return (T) value;

        try {
            return targetType.getConstructor(String.class).newInstance(value);
        } catch (NoSuchMethodException nsme) {
            ;
        }

        try {
            return (T) targetType.getField(value).get(null);
        } catch (NoSuchFieldException nsfe) {
            ;
        }

        throw new NoSuchMethodException(
            targetType
            + " has neither a single-string-arg constructor nor a constant "
            + value
        );
    }

    /**
     * @return An array with the same length as {@code values}, containing the {@code values}, converted to the given
     *         {@code targetTypes} using the {@code targetTypes}' one-string-parameter constructors
     */
    private static Object[]
    convert(List<String> values, Class<?>[] targetTypes)
    throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        Object[] result = new Object[targetTypes.length];

        for (int i = 0; i < result.length; i++) result[i] = LoggingTask.convert(values.get(i), targetTypes[i]);

        return result;
    }
}
