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

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Properties;
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
    private final static String A_PROPERTIES = "L"
        + Properties.class.getName().replace('.', '/') + ";";
    private final static String A_ASPECT_SERVICE = "L"
        + AspectService.class.getName().replace('.', '/') + ";";

    private Reporter m_reporter;
    private String m_className;
    private String[] m_interfaces;
    private boolean m_isField;
    private String m_field;
    private String m_method;
    private String m_descriptor;
    private Set<String> m_methods = new HashSet<String>();
    private List<Info> m_infos = new ArrayList<Info>(); // Last elem is either Service or AspectService
    private MetaType m_metaType;
    private String m_startMethod;
    private String m_stopMethod;
    private String m_initMethod;
    private String m_destroyMethod;
    private String m_compositionMethod;

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
        AspectService,
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
    public AnnotationCollector(Reporter reporter, MetaType metaType)
    {
        m_reporter = reporter;
        m_metaType = metaType;
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
        
        if (annotation.getName().equals(A_SERVICE))
        {
            parseServiceAnnotation(annotation);
        } 
        else if (annotation.getName().equals(A_ASPECT_SERVICE))
        {
            parseAspectService(annotation);
        }
        else if (annotation.getName().equals(A_INIT))
        {
            checkMethod(m_voidMethodPattern);
            m_initMethod = m_method;
        }
        else if (annotation.getName().equals(A_START))
        {
            checkMethod(m_voidMethodPattern);
            m_startMethod = m_method;
        }
        else if (annotation.getName().equals(A_STOP))
        {
            checkMethod(m_voidMethodPattern);
            m_stopMethod = m_method;
        }
        else if (annotation.getName().equals(A_DESTROY))
        {
            checkMethod(m_voidMethodPattern);
            m_destroyMethod = m_method;
        }
        else if (annotation.getName().equals(A_COMPOSITION))
        {
            checkMethod(m_methodCompoPattern);
            m_compositionMethod = m_method;
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
        else if (annotation.getName().equals(A_PROPERTIES)) 
        {
            parsePropertiesMetaData(annotation);
        }
    }

    /**
     * Parses a Service annotation.
     * @param annotation The Service annotation.
     */
    private void parseServiceAnnotation(Annotation annotation)
    {
        Info info = new Info(EntryTypes.Service);
        m_infos.add(info);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addInitStartStopDestroyCompositionParams(info);
        
        // impl attribute
        info.addParam(Params.impl, m_className);

        // properties attribute
        parseParameters(annotation, Params.properties, info);

        // provide attribute
        info.addClassParam(annotation, Params.provide, m_interfaces);

        // factory attribute
        info.addClassParam(annotation, Params.factory, null);

        // factoryMethod attribute
        info.addParam(annotation, Params.factoryMethod, null);
    }

    private void addInitStartStopDestroyCompositionParams(Info info)
    {
        if (m_initMethod != null) {
            info.addParam(Params.init, m_initMethod);
        }
        
        if (m_startMethod != null) {
            info.addParam(Params.start, m_startMethod);
        }
        
        if (m_stopMethod != null) {
            info.addParam(Params.stop, m_stopMethod);
        }
        
        if (m_destroyMethod != null) {
            info.addParam(Params.destroy, m_destroyMethod);
        }
        
        // Register Composition method
        if (m_compositionMethod != null) {
            info.addParam(Params.composition, m_compositionMethod);
        }        
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
     * Parses a Properties annotation which declares Config Admin Properties meta data.
     * @param properties the Properties annotation to be parsed.
     */
    private void parsePropertiesMetaData(Annotation properties)
    {
        String propertiesPid = get(properties, "pid", m_className);
        String propertiesHeading = properties.get("heading");
        String propertiesDesc = properties.get("description");

        MetaType.OCD ocd = new MetaType.OCD(propertiesPid, propertiesHeading, propertiesDesc);
        for (Object p : (Object[]) properties.get("properties"))
        {
            Annotation property = (Annotation) p;
            String heading = property.get("heading");
            String id = property.get("id");
            String type = (String) property.get("type");
            type = (type != null) ? parseClass(type, m_classPattern, 1) : null;
            Object[] defaults = (Object[]) property.get("defaults");
            String description = property.get("description");
            Integer cardinality = property.get("cardinality");
            Boolean required = property.get("required");

            MetaType.AD ad = new MetaType.AD(id, type, defaults, heading, description, cardinality, required);
            Object[] options = property.get("options");
            if (options != null) {
                for (Object o : (Object[]) property.get("options"))
                {
                    Annotation option = (Annotation) o;
                    ad.add(new MetaType.Option((String) option.get("name"), (String) option.get("value")));
                }
            }
            ocd.add(ad);
        }

        m_metaType.add(ocd);
        MetaType.Designate designate = new MetaType.Designate(propertiesPid);
        m_metaType.add(designate);
        m_reporter.warning("Parsed MetaType Properties from class " + m_className);
    }

    /**
     * Parses an AspectService annotation.
     * @param annotation
     */
    private void parseAspectService(Annotation annotation)
    {
        Info info = new Info(EntryTypes.AspectService);
        m_infos.add(info);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addInitStartStopDestroyCompositionParams(info);
        
        // Parse Service interface.
        Object service = annotation.get(Params.service.toString());
        if (service == null) {
            if (m_interfaces == null)
            {
                throw new IllegalStateException("Invalid AspectService annotation: " +
                    "the service attribute has not been set and the class " + m_className + " does not implement any interfaces");
            }
            if (m_interfaces.length != 1) 
            {
                throw new IllegalStateException("Invalid AspectService annotation: " +
                    "the service attribute has not been set and the class " + m_className + " implements more than one interface");
            }
            
            service = m_interfaces[0];
        }
        info.addClassParam(annotation, Params.service, service.toString());
        
        // Parse service filter
        String filter = annotation.get(Params.filter.toString());
        Verifier.verifyFilter(filter, 0);
        info.addParam(Params.filter, filter);
                
        // Generate Aspect Implementation
        info.addParam(Params.impl, m_className);
        
        // Parse Aspect properties.
        parseParameters(annotation, Params.properties, info);
    }

    /**
     * Parses a Param annotation (which represents a list of key-value pari).
     * @param annotation the annotation where the Param annotation is defined
     * @param attribute the attribute name which is of Param type
     * @param info the Info object where the parsed attributes are written
     */
    private void parseParameters(Annotation annotation, Params attribute, Info info) {
        Object[] parameters = annotation.get(attribute.toString());
        if (parameters != null)
        {
            for (Object p : parameters)
            {
                Annotation a = (Annotation) p; 
                String prop = a.get("name") + ":" + a.get("value");
                info.addParam(attribute, prop);
            }
        }
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
     * Checks if the class is annotated with some given annotations. Notice that the Service
     * is always parsed at end of parsing, so, we have to check the last element of our m_infos
     * List.
     * @return true if one of the provided annotations has been found from the parsed class.
     */
    private void checkServiceDeclared(EntryTypes ... types) {
        boolean ok = false;
        if (m_infos.size() > 0)
        {
            for (EntryTypes type : types)
            {
                if (m_infos.get(m_infos.size() - 1).m_entry == type)
                {
                    ok = true;
                    break;
                }
            }
        }

        if (!ok)
        {
            throw new IllegalStateException(
                ": the class must be annotated with either one of the following types: "
                    + Arrays.toString(types));
        }
    }

    /**
     * Get an annotation attribute, and return a default value if its not present.
     * @param <T> the type of the variable which is assigned to the return value of this method.
     * @param properties The annotation we are parsing
     * @param name the attribute name to get from the annotation
     * @param defaultValue the default value to return if the attribute is not found in the annotation
     * @return the annotation attribute value, or the defaultValue if not found
     */
    private <T> T get(Annotation properties, String name, T defaultValue)
    {
        T value = (T) properties.get(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Finishes up the class parsing. This method must be called once the parseClassFileWithCollector method has returned.
     * @return true if some annotations have been parsed, false if not.
     */
    public boolean finish()
    {
        if (m_infos.size() == 0)
        {
            return false;
        }

        // We must have at least a Service or an AspectService annotation.
        checkServiceDeclared(EntryTypes.Service, EntryTypes.AspectService);

        StringBuilder sb = new StringBuilder();
        sb.append("Parsed annotation for class ");
        sb.append(m_className);
        for (int i = m_infos.size() - 1; i >= 0; i--)
        {
            sb.append("\n\t").append(m_infos.get(i).toString());
        }
        m_reporter.warning(sb.toString());
        return true;
    }

    /**
     * Writes the generated component descriptor in the given print writer.
     * The first line must be the service (@Service or AspectService).
     * @param pw the writer where the component descriptor will be written.
     */
    public void writeTo(PrintWriter pw)
    {
        // The last element our our m_infos list contains either the Service, or the AspectService descriptor.
        for (int i = m_infos.size() - 1; i >= 0; i--)
        {
            pw.println(m_infos.get(i).toString());
        }
    }
}
