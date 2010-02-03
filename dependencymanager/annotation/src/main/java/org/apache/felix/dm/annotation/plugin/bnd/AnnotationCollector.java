/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.annotation.plugin.bnd;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.TemporalServiceDependency;

import aQute.lib.osgi.Annotation;
import aQute.lib.osgi.ClassDataCollector;
import aQute.lib.osgi.Verifier;
import aQute.libg.reporter.Reporter;

/**
 * This is the scanner which does all the annotation parsing on a given class.
 * To start the parsing, just invoke the parseClassFileWithCollector and finish methods.
 * Once parsed, the corresponding component descriptors can be built using the "writeTo" method.
 */
public class AnnotationCollector extends ClassDataCollector
{
    private final static String A_INIT = "L" + Init.class.getName().replace('.', '/') + ";";
    private final static String A_START = "L" + Start.class.getName().replace('.', '/') + ";";
    private final static String A_STOP = "L" + Stop.class.getName().replace('.', '/') + ";";
    private final static String A_DESTROY = "L" + Destroy.class.getName().replace('.', '/') + ";";
    private final static String A_COMPOSITION = "L" + Composition.class.getName().replace('.', '/')
        + ";";
    private final static String A_SERVICE = "L" + Service.class.getName().replace('.', '/') + ";";
    private final static String A_SERVICE_DEP = "L"
        + ServiceDependency.class.getName().replace('.', '/') + ";";
    private final static String A_CONFIGURATION_DEPENDENCY = "L"
        + ConfigurationDependency.class.getName().replace('.', '/') + ";";
    private final static String A_TEMPORAL_SERVICE_DEPENDENCY = "L"
        + TemporalServiceDependency.class.getName().replace('.', '/') + ";";

    private Reporter m_reporter;
    private String m_className;
    private String[] m_interfaces;
    private boolean m_isField;
    private String m_field;
    private String m_method;
    private String m_descriptor;
    private Set<String> m_methods = new HashSet<String>();
    private List<Info> m_infos = new ArrayList<Info>();

    // Pattern used to parse the class parameter from the bind methods ("bind(Type)" or "bind(Map, Type)")
    private final static Pattern m_bindClassPattern = Pattern.compile("\\((Ljava/util/Map;)?L([^;]+);\\)V");

    // Pattern used to parse classes from class descriptors;
    private final static Pattern m_classPattern = Pattern.compile("L([^;]+);");

    // Pattern used to check if a method is void and does not take any params
    private final static Pattern m_voidMethodPattern = Pattern.compile("\\(\\)V");

    // Pattern used to check if a method returns an array of Objects
    private final static Pattern m_methodCompoPattern = Pattern.compile("\\(\\)\\[Ljava/lang/Object;");

    // List of component descriptor entry types
    enum EntryTypes
    {
        Service, 
        ServiceDependency, 
        TemporalServiceDependency, 
        ConfigurationDependency,
    };

    // List of component descriptor parameters
    enum Params
    {
        init, 
        start, 
        stop, 
        destroy, 
        impl, 
        provide, 
        properties, 
        factory, 
        factoryMethod, 
        composition, 
        service, 
        filter, 
        defaultImpl, 
        required, 
        added, 
        changed,
        removed,
        autoConfig, 
        pid, 
        propagate, 
        updated, 
        timeout
    };

    /**
     * This class represents a parsed DependencyManager component descriptor entry.
     * (either a Service, a ServiceDependency, or a ConfigurationDependency descriptor entry).
     */
    private class Info
    {
        /** The component descriptor entry type: either Service, (Temporal)ServiceDependency, or ConfigurationDependency */
        EntryTypes m_entry;

        /** The component descriptor entry parameters (init/start/stop ...) */
        Map<Params, Object> m_params = new HashMap<Params, Object>();

        /**
         * Makes a new component descriptor entry.
         * @param entry the component descriptor entry type (either Service, ServiceDependency, or ConfigurationDependency)
         */
        Info(EntryTypes entry)
        {
            m_entry = entry;
        }

        /**
         * Returns a string representation for the given component descriptor line entry.
         */
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(m_entry);
            sb.append(":").append(" ");
            for (Map.Entry<Params, Object> e : m_params.entrySet())
            {
                sb.append(e.getKey());
                sb.append("=");
                sb.append(e.getValue());
                sb.append("; ");
            }
            return sb.toString();
        }

        /**
         * Adds a parameter to this component descriptor entry.
         * @param param the param name
         * @param value the param value
         */
        void addParam(Params param, String value)
        {
            String old = (String) m_params.get(param);
            if (old != null)
            {
                value = old + "," + value;
            }
            m_params.put(param, value);
        }

        /**
         * Adds an annotation parameter to this component descriptor entry.
         * @param annotation the annotation where the parameter has been parsed
         * @param param the param name
         * @param def the default value to add, if the param is not present in the parsed annotation.
         */
        void addParam(Annotation annotation, Params param, Object def)
        {
            Object value = annotation.get(param.toString());
            if (value == null && def != null)
            {
                value = def;
            }
            if (value != null)
            {
                if (value instanceof Object[])
                {
                    for (Object v : ((Object[]) value))
                    {
                        addParam(param, v.toString());
                    }
                }
                else
                {
                    addParam(param, value.toString());
                }
            }
        }

        /**
         * Adds a annotation parameter of type 'class' to this component descriptor entry.
         * The parsed class parameter has the format "Lfull.package.ClassName;"
         * @param annotation the annotation where the class parameter has been parsed
         * @param param the annotation class param name
         * @param def the default class name to add if the param is not present in the parsed annotation.
         */
        void addClassParam(Annotation annotation, Params param, Object def)
        {
            Pattern pattern = m_classPattern;
            Object value = annotation.get(param.toString());
            if (value == null && def != null)
            {
                value = def;
                pattern = null;
            }
            if (value != null)
            {
                if (value instanceof Object[])
                {
                    for (Object v : ((Object[]) value))
                    {
                        if (pattern != null)
                        {
                            v = parseClass(v.toString(), pattern, 1);
                        }
                        addParam(param, v.toString());
                    }
                }
                else
                {
                    if (pattern != null)
                    {
                        value = parseClass(value.toString(), pattern, 1);
                    }
                    addParam(param, value.toString());
                }
            }

        }
    }

    /**
     * Makes a new Collector for parsing a given class.
     * @param reporter the object used to report logs.
     */
    public AnnotationCollector(Reporter reporter)
    {
        m_reporter = reporter;
        m_infos.add(new Info(EntryTypes.Service));
    }

    /**
     * Returns the log reporter.
     * @return the log reporter.
     */
    public Reporter getReporter()
    {
        return m_reporter;
    }

    /**
     * Parses the name of the class.
     * @param access the class access
     * @param name the class name (package are "/" separated).
     */
    @Override
    public void classBegin(int access, String name)
    {
        m_className = name.replace('/', '.');
        m_reporter.trace("class name: " + m_className);
    }

    /**
     * Parses the implemented interfaces ("/" separated).
     */
    @Override
    public void implementsInterfaces(String[] interfaces)
    {
        m_interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++)
        {
            m_interfaces[i] = interfaces[i].replace('/', '.');
        }
        m_reporter.trace("implements: %s", Arrays.toString(m_interfaces));
    }

    /**
     * Parses a method. Always invoked BEFORE eventual method annotation.
     */
    @Override
    public void method(int access, String name, String descriptor)
    {
        m_reporter.trace("Parsed method %s, descriptor=%s", name, descriptor);
        m_isField = false;
        m_method = name;
        m_descriptor = descriptor;
        m_methods.add(name + descriptor);
    }

    /**
     * Parses a field. Always invoked BEFORE eventual field annotation
     */
    @Override
    public void field(int access, String name, String descriptor)
    {
        m_reporter.trace("Parsed field %s, descriptor=%s", name, descriptor);
        m_isField = true;
        m_field = name;
        m_descriptor = descriptor;
    }

    /** 
     * An annotation has been parsed. Always invoked AFTER the "method"/"field"/"classBegin" callbacks. 
     */
    @Override
    public void annotation(Annotation annotation)
    {
        m_reporter.trace("Parsed annotation: %s", annotation);
        if (annotation.getName().equals(A_INIT))
        {
            checkMethod(m_voidMethodPattern);
            m_infos.get(0).m_params.put(Params.init, m_method);
        }
        else if (annotation.getName().equals(A_START))
        {
            checkMethod(m_voidMethodPattern);
            m_infos.get(0).m_params.put(Params.start, m_method);
        }
        else if (annotation.getName().equals(A_STOP))
        {
            checkMethod(m_voidMethodPattern);
            m_infos.get(0).m_params.put(Params.stop, m_method);
        }
        else if (annotation.getName().equals(A_DESTROY))
        {
            checkMethod(m_voidMethodPattern);
            m_infos.get(0).m_params.put(Params.destroy, m_method);
        }
        else if (annotation.getName().equals(A_COMPOSITION))
        {
            checkMethod(m_methodCompoPattern);
            m_infos.get(0).m_params.put(Params.composition, m_method);
        }
        else if (annotation.getName().equals(A_SERVICE))
        {
            parseServiceAnnotation(annotation);
        }
        else if (annotation.getName().equals(A_SERVICE_DEP))
        {
            parseServiceDependencyAnnotation(annotation, false);
        }
        else if (annotation.getName().equals(A_CONFIGURATION_DEPENDENCY))
        {
            parseConfigurationDependencyAnnotation(annotation);
        }
        else if (annotation.getName().equals(A_TEMPORAL_SERVICE_DEPENDENCY))
        {
            parseServiceDependencyAnnotation(annotation, true);
        }
    }

    /**
     * Parses a Service annotation.
     * @param annotation The Service annotation.
     */
    private void parseServiceAnnotation(Annotation annotation)
    {
        Info info = m_infos.get(0);
        // impl attribute
        info.addParam(Params.impl, m_className);

        // properties attribute
        Object[] properties = annotation.get(Params.properties.toString());
        if (properties != null)
        {
            for (Object p : properties)
            {
                Annotation a = (Annotation) p; 
                String prop = a.get("name") + ":" + a.get("value");
                info.addParam(Params.properties, prop);
            }
        }

        // provide attribute
        info.addClassParam(annotation, Params.provide, m_interfaces);

        // factory attribute
        info.addClassParam(annotation, Params.factory, null);

        // factoryMethod attribute
        info.addParam(annotation, Params.factoryMethod, null);
    }

    /**
     * Parses a ServiceDependency or a TemporalServiceDependency Annotation.
     * @param annotation the ServiceDependency Annotation.
     */
    private void parseServiceDependencyAnnotation(Annotation annotation, boolean temporal)
    {
        Info info = new Info(temporal ? EntryTypes.TemporalServiceDependency
            : EntryTypes.ServiceDependency);
        m_infos.add(info);

        // service attribute
        String service = annotation.get(Params.service.toString());
        if (service != null)
        {
            service = parseClass(service, m_classPattern, 1);
        }
        else
        {
            if (m_isField)
            {
                service = parseClass(m_descriptor, m_classPattern, 1);
            }
            else
            {
                service = parseClass(m_descriptor, m_bindClassPattern, 2);
            }
        }
        info.addParam(Params.service, service);

        // autoConfig attribute
        if (m_isField)
        {
            info.addParam(Params.autoConfig, m_field);
        }

        // filter attribute
        String filter = annotation.get(Params.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            info.addParam(Params.filter, filter);
        }

        // defaultImpl attribute
        info.addClassParam(annotation, Params.defaultImpl, null);

        // added callback
        info.addParam(annotation, Params.added, (!m_isField) ? m_method : null);

        if (temporal)
        {
            // timeout attribute (only valid if parsing a temporal service dependency)
            info.addParam(annotation, Params.timeout, null);
        }
        else
        {
            // required attribute (not valid if parsing a temporal service dependency)
            info.addParam(annotation, Params.required, null);

            // changed callback
            info.addParam(annotation, Params.changed, null);

            // removed callback
            info.addParam(annotation, Params.removed, null);
        }
    }

    /**
     * Parses a ConfigurationDependency annotation.
     * @param annotation the ConfigurationDependency annotation.
     */
    private void parseConfigurationDependencyAnnotation(Annotation annotation)
    {
        Info info = new Info(EntryTypes.ConfigurationDependency);
        m_infos.add(info);

        // pid attribute
        info.addParam(annotation, Params.pid, m_className);

        // propagate attribute
        info.addParam(annotation, Params.propagate, null);
    }

    /**
     * Parses a class.
     * @param clazz the class to be parsed (the package is "/" separated).
     * @param pattern the pattern used to match the class.
     * @param group the pattern group index where the class can be retrieved.
     * @return the parsed class.
     */
    private String parseClass(String clazz, Pattern pattern, int group)
    {
        Matcher matcher = pattern.matcher(clazz);
        if (matcher.matches())
        {
            return matcher.group(group).replace("/", ".");
        }
        else
        {
            m_reporter.warning("Invalid class descriptor: %s", clazz);
            throw new IllegalArgumentException("Invalid class descriptor: " + clazz);
        }
    }

    /**
     * Checks if a method descriptor matches a given pattern. 
     * @param pattern the pattern used to check the method signature descriptor
     * @throws IllegalArgumentException if the method signature descriptor does not match the given pattern.
     */
    private void checkMethod(Pattern pattern)
    {
        Matcher matcher = pattern.matcher(m_descriptor);
        if (!matcher.matches())
        {
            m_reporter.warning("Invalid method %s : wrong signature: %s", m_method, m_descriptor);
            throw new IllegalArgumentException("Invalid method " + m_method + ", wrong signature: "
                + m_descriptor);
        }
    }

    /**
     * Finishes up the class parsing. This method must be called once the parseClassFileWithCollector method has returned.
     */
    public void finish()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Parsed annotation for class ");
        sb.append(m_className);
        for (Info info : m_infos)
        {
            sb.append("\n\t").append(info.toString());
        }
        m_reporter.warning(sb.toString());
    }

    /**
     * Writes the generated component descriptor in the given print writer
     * @param pw the writer where the component descriptor will be written.
     */
    public void writeTo(PrintWriter pw)
    {
        for (Info info : m_infos)
        {
            pw.println(info.toString());
        }
    }
}
