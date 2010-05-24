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

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.adapter.AdapterService;
import org.apache.felix.dm.annotation.api.adapter.BundleAdapterService;
import org.apache.felix.dm.annotation.api.adapter.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.adapter.ResourceAdapterService;
import org.apache.felix.dm.annotation.api.dependency.BundleDependency;
import org.apache.felix.dm.annotation.api.dependency.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.dependency.ResourceDependency;
import org.apache.felix.dm.annotation.api.dependency.ServiceDependency;
import org.osgi.framework.Bundle;

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
    private final static String A_BUNDLE_DEPENDENCY = "L"
        + BundleDependency.class.getName().replace('.', '/') + ";";
    private final static String A_RESOURCE_DEPENDENCY = "L"
        + ResourceDependency.class.getName().replace('.', '/') + ";";
    private final static String A_ASPECT_SERVICE = "L"
        + AspectService.class.getName().replace('.', '/') + ";";
    private final static String A_ADAPTER_SERVICE = "L"
        + AdapterService.class.getName().replace('.', '/') + ";";
    private final static String A_BUNDLE_ADAPTER_SERVICE = "L"
        + BundleAdapterService.class.getName().replace('.', '/') + ";";
    private final static String A_RESOURCE_ADAPTER_SERVICE = "L"
        + ResourceAdapterService.class.getName().replace('.', '/') + ";";
    private final static String A_FACTORYCONFIG_ADAPTER_SERVICE = "L"
        + FactoryConfigurationAdapterService.class.getName().replace('.', '/') + ";";

    private Reporter m_reporter;
    private String m_className;
    private String[] m_interfaces;
    private boolean m_isField;
    private String m_field;
    private String m_method;
    private String m_descriptor;
    private Set<String> m_methods = new HashSet<String>();
    private List<EntryWriter> m_writers = new ArrayList<EntryWriter>(); // Last elem is either Service or AspectService
    private MetaType m_metaType;
    private String m_startMethod;
    private String m_stopMethod;
    private String m_initMethod;
    private String m_destroyMethod;
    private String m_compositionMethod;

    /**
     * This class represents a DependencyManager component descriptor entry.
     * (Service, a ServiceDependency ... see EntryType enum).
     */

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
        else if (annotation.getName().equals(A_ADAPTER_SERVICE))
        {
            parseAdapterService(annotation);
        }
        else if (annotation.getName().equals(A_BUNDLE_ADAPTER_SERVICE))
        {
            parseBundleAdapterService(annotation);
        }
        else if (annotation.getName().equals(A_RESOURCE_ADAPTER_SERVICE))
        {
            parseResourceAdapterService(annotation);
        }
        else if (annotation.getName().equals(A_FACTORYCONFIG_ADAPTER_SERVICE))
        {
            parseFactoryConfigurationAdapterService(annotation);
        }
        else if (annotation.getName().equals(A_INIT))
        {
            //Patterns.parseMethod(m_method, m_descriptor, Patterns.VOID);
            // TODO check if method takes optional params like Service, DependencyManager, etc ...
            m_initMethod = m_method;
        }
        else if (annotation.getName().equals(A_START))
        {
            //Patterns.parseMethod(m_method, m_descriptor, Patterns.VOID);
            // TODO check if method takes optional params like Service, DependencyManager, etc ...
            m_startMethod = m_method;
        }
        else if (annotation.getName().equals(A_STOP))
        {
            //Patterns.parseMethod(m_method, m_descriptor, Patterns.VOID);
            // TODO check if method takes optional params like Service, DependencyManager, etc ...
            m_stopMethod = m_method;
        }
        else if (annotation.getName().equals(A_DESTROY))
        {
            //Patterns.parseMethod(m_method, m_descriptor, Patterns.VOID);
            // TODO check if method takes optional params like Service, DependencyManager, etc ...
            m_destroyMethod = m_method;
        }
        else if (annotation.getName().equals(A_COMPOSITION))
        {
            Patterns.parseMethod(m_method, m_descriptor, Patterns.COMPOSITION);
            m_compositionMethod = m_method;
        }
        else if (annotation.getName().equals(A_SERVICE_DEP))
        {
            parseServiceDependencyAnnotation(annotation);
        }
        else if (annotation.getName().equals(A_CONFIGURATION_DEPENDENCY))
        {
            parseConfigurationDependencyAnnotation(annotation);
        }
        else if (annotation.getName().equals(A_BUNDLE_DEPENDENCY))
        {
            parseBundleDependencyAnnotation(annotation);
        }
        else if (annotation.getName().equals(A_RESOURCE_DEPENDENCY))
        {
            parseRersourceDependencyAnnotation(annotation);
        }
    }

    /**
     * Parses a Service annotation.
     * @param annotation The Service annotation.
     */
    private void parseServiceAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.Service);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // impl attribute
        writer.put(EntryParam.impl, m_className);

        // properties attribute
        parseProperties(annotation, EntryParam.properties, writer);

        // provide attribute
        writer.putClassArray(annotation, EntryParam.provide, m_interfaces);

        // factory attribute
        writer.putString(annotation, EntryParam.factory, null);

        // factoryPropertiesCallback attribute
        writer.putString(annotation, EntryParam.factoryConfigure, null);
    }

    private void addCommonServiceParams(EntryWriter writer)
    {
        if (m_initMethod != null)
        {
            writer.put(EntryParam.init, m_initMethod);
        }

        if (m_startMethod != null)
        {
            writer.put(EntryParam.start, m_startMethod);
        }

        if (m_stopMethod != null)
        {
            writer.put(EntryParam.stop, m_stopMethod);
        }

        if (m_destroyMethod != null)
        {
            writer.put(EntryParam.destroy, m_destroyMethod);
        }

        // Register Composition method
        if (m_compositionMethod != null)
        {
            writer.put(EntryParam.composition, m_compositionMethod);
        }
    }

    /**
     * Parses a ServiceDependency Annotation.
     * @param annotation the ServiceDependency Annotation.
     */
    private void parseServiceDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ServiceDependency);
        m_writers.add(writer);

        // service attribute
        String service = annotation.get(EntryParam.service.toString());
        if (service != null)
        {
            service = Patterns.parseClass(service, Patterns.CLASS, 1);
        }
        else
        {
            if (m_isField)
            {
                service = Patterns.parseClass(m_descriptor, Patterns.CLASS, 1);
            }
            else
            {
                service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS, 2);
            }
        }
        writer.put(EntryParam.service, service);

        // autoConfig attribute
        if (m_isField)
        {
            writer.put(EntryParam.autoConfig, m_field);
        }

        // filter attribute
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // defaultImpl attribute
        writer.putClass(annotation, EntryParam.defaultImpl, null);

        // added callback
        writer.putString(annotation, EntryParam.added, (!m_isField) ? m_method : null);

        // timeout parameter
        writer.putString(annotation, EntryParam.timeout, null);
        Long t = (Long) annotation.get(EntryParam.timeout.toString());
        if (t != null && t.longValue() < -1)
        {
            throw new IllegalArgumentException("Invalid timeout value " + t + " in ServiceDependency annotation in class " + m_className);
        }
        
        // required attribute (not valid if parsing a temporal service dependency)
        writer.putString(annotation, EntryParam.required, null);

        // changed callback
        writer.putString(annotation, EntryParam.changed, null);

        // removed callback
        writer.putString(annotation, EntryParam.removed, null);       
    }

    /**
     * Parses a ConfigurationDependency annotation.
     * @param annotation the ConfigurationDependency annotation.
     */
    private void parseConfigurationDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ConfigurationDependency);
        m_writers.add(writer);

        // pid attribute
        writer.putString(annotation, EntryParam.pid, m_className);

        // propagate attribute
        writer.putString(annotation, EntryParam.propagate, null);

        // Property Meta Types
        String pid = get(annotation, EntryParam.pid.toString(), m_className);
        parseMetaTypes(annotation, pid, false);
    }

    /**
     * Parses an AspectService annotation.
     * @param annotation
     */
    private void parseAspectService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.AspectService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Parse service filter
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // Parse service aspect ranking
        Integer ranking = annotation.get(EntryParam.ranking.toString());
        writer.put(EntryParam.ranking, ranking.toString());

        // Generate Aspect Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse Aspect properties.
        parseProperties(annotation, EntryParam.properties, writer);
        
        // Parse aspect impl field where to inject the original service.
        writer.putString(annotation, EntryParam.field, null);

        // Parse service interface this aspect is applying to
        Object service = annotation.get(EntryParam.service.toString());
        if (service == null)
        {
            if (m_interfaces == null)
            {
                throw new IllegalStateException("Invalid AspectService annotation: " +
                    "the service attribute has not been set and the class " + m_className
                    + " does not implement any interfaces");
            }
            if (m_interfaces.length != 1)
            {
                throw new IllegalStateException("Invalid AspectService annotation: " +
                    "the service attribute has not been set and the class " + m_className
                    + " implements more than one interface");
            }

            writer.put(EntryParam.service, m_interfaces[0]);
        }
        else
        {
            writer.putClassArray(annotation, EntryParam.service, null);
        }
    }

    /**
     * Parses an AspectService annotation.
     * @param annotation
     */
    private void parseAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.AdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse adaptee filter
        String adapteeFilter = annotation.get(EntryParam.adapteeFilter.toString());
        if (adapteeFilter != null)
        {
            Verifier.verifyFilter(adapteeFilter, 0);
            writer.put(EntryParam.adapteeFilter, adapteeFilter);
        }

        // Parse the mandatory adapted service interface.
        writer.putClass(annotation, EntryParam.adapteeService, null);

        // Parse Adapter properties.
        parseProperties(annotation, EntryParam.adapterProperties, writer);

        // Parse the optional adapter service (use directly implemented interface by default).
        writer.putClassArray(annotation, EntryParam.adapterService, m_interfaces);
    }

    /**
     * Parses a BundleAdapterService annotation.
     * @param annotation
     */
    private void parseBundleAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.BundleAdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse bundle filter
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // Parse stateMask attribute
        writer.putString(annotation, EntryParam.stateMask, Integer.valueOf(
            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE).toString());

        // Parse Adapter properties.
        parseProperties(annotation, EntryParam.properties, writer);

        // Parse the optional adapter service (use directly implemented interface by default).
        writer.putClassArray(annotation, EntryParam.service, m_interfaces);

        // Parse propagate attribute
        writer.putString(annotation, EntryParam.propagate, Boolean.FALSE.toString());
    }

    /**
     * Parses a BundleAdapterService annotation.
     * @param annotation
     */
    private void parseResourceAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ResourceAdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse resource filter
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // Parse Adapter properties.
        parseProperties(annotation, EntryParam.properties, writer);

        // Parse the optional adapter service (use directly implemented interface by default).
        writer.putClassArray(annotation, EntryParam.service, m_interfaces);

        // Parse propagate attribute
        writer.putString(annotation, EntryParam.propagate, Boolean.FALSE.toString());
    }

    /**
     * Parses a Factory Configuration Adapter annotation.
     * @param annotation
     */
    private void parseFactoryConfigurationAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.FactoryConfigurationAdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse factory Pid
        writer.putString(annotation, EntryParam.factoryPid, m_className);

        // Parse updated callback
        writer.putString(annotation, EntryParam.updated, "updated");

        // propagate attribute
        writer.putString(annotation, EntryParam.propagate, Boolean.FALSE.toString());

        // Parse the optional adapter service (use directly implemented interface by default).
        writer.putClassArray(annotation, EntryParam.service, m_interfaces);

        // Parse Adapter properties.
        parseProperties(annotation, EntryParam.properties, writer);

        // Parse optional meta types for configuration description.
        String factoryPid = get(annotation, EntryParam.factoryPid.toString(), m_className);
        parseMetaTypes(annotation, factoryPid, true);
    }

    private void parseBundleDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.BundleDependency);
        m_writers.add(writer);

        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        writer.putString(annotation, EntryParam.added, m_method);
        writer.putString(annotation, EntryParam.changed, null);
        writer.putString(annotation, EntryParam.removed, null);
        writer.putString(annotation, EntryParam.required, null);
        writer.putString(annotation, EntryParam.stateMask, null);
        writer.putString(annotation, EntryParam.propagate, null);
    }

    private void parseRersourceDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ResourceDependency);
        m_writers.add(writer);

        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        writer.putString(annotation, EntryParam.added, m_method);
        writer.putString(annotation, EntryParam.changed, null);
        writer.putString(annotation, EntryParam.removed, null);
        writer.putString(annotation, EntryParam.required, null);
        writer.putString(annotation, EntryParam.propagate, null);
    }

    /**
     * Parse optional meta types annotation attributes
     * @param annotation
     */
    @SuppressWarnings("null")
    private void parseMetaTypes(Annotation annotation, String pid, boolean factory)
    {
        if (annotation.get("metadata") != null)
        {
            String propertiesHeading = annotation.get("heading");
            String propertiesDesc = annotation.get("description");

            MetaType.OCD ocd = new MetaType.OCD(pid, propertiesHeading, propertiesDesc);
            for (Object p: (Object[]) annotation.get("metadata"))
            {
                Annotation property = (Annotation) p;
                String heading = property.get("heading");
                String id = property.get("id");
                String type = (String) property.get("type");
                type = (type != null) ? Patterns.parseClass(type, Patterns.CLASS, 1) : null;
                Object[] defaults = (Object[]) property.get("defaults");
                String description = property.get("description");
                Integer cardinality = property.get("cardinality");
                Boolean required = property.get("required");

                MetaType.AD ad = new MetaType.AD(id, type, defaults, heading, description,
                    cardinality, required);

                Object[] optionLabels = property.get("optionLabels");
                Object[] optionValues = property.get("optionValues");

                if (optionLabels == null
                    && optionValues != null
                    ||
                    optionLabels != null
                    && optionValues == null
                    ||
                    (optionLabels != null && optionValues != null && optionLabels.length != optionValues.length))
                {
                    throw new IllegalArgumentException("invalid option labels/values specified for property "
                        + id +
                        " in PropertyMetadata annotation from class " + m_className);
                }

                if (optionValues != null)
                {
                    for (int i = 0; i < optionValues.length; i++)
                    {
                        ad.add(new MetaType.Option(optionValues[i].toString(), optionLabels[i].toString()));
                    }
                }

                ocd.add(ad);
            }

            m_metaType.add(ocd);
            MetaType.Designate designate = new MetaType.Designate(pid, factory);
            m_metaType.add(designate);
            m_reporter.warning("Parsed MetaType Properties from class " + m_className);
        }
    }

    /**
     * Parses a Property annotation (which represents a list of key-value pair).
     * @param annotation the annotation where the Param annotation is defined
     * @param attribute the attribute name which is of Param type
     * @param writer the object where the parsed attributes are written
     */
    private void parseProperties(Annotation annotation, EntryParam attribute, EntryWriter writer)
    {
        Object[] parameters = annotation.get(attribute.toString());
        Map<String, Object> properties = new HashMap<String, Object>();
        if (parameters != null)
        {
            for (Object p: parameters)
            {
                Annotation a = (Annotation) p;
                String name = (String) a.get("name");
                String value = (String) a.get("value");
                if (value != null)
                {
                    properties.put(name, value);
                }
                else
                {
                    Object[] values = a.get("values");
                    if (values != null)
                    {
                        // the values is an Object array of actual strings, and we must convert it into a String array.
                        properties.put(name, Arrays.asList(values).toArray(new String[values.length]));
                    }
                    else
                    {
                        throw new IllegalArgumentException("Invalid Property attribyte \"" + attribute
                            + " from annotation " + annotation + " in class " + m_className);
                    }
                }
            }
            writer.putProperties(attribute, properties);
        }
    }

    /**
     * Checks if the class is annotated with some given annotations. Notice that the Service
     * is always parsed at end of parsing, so, we have to check the last element of our m_writers
     * List.
     * @return true if one of the provided annotations has been found from the parsed class.
     */
    private void checkServiceDeclared(EntryType... types)
    {
        boolean ok = false;
        if (m_writers.size() > 0)
        {
            for (EntryType type: types)
            {
                if (m_writers.get(m_writers.size() - 1).getEntryType() == type)
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
    @SuppressWarnings("unchecked")
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
        if (m_writers.size() == 0)
        {
            return false;
        }

        // We must have at least a Service or an AspectService annotation.
        checkServiceDeclared(EntryType.Service, EntryType.AspectService, EntryType.AdapterService,
            EntryType.BundleAdapterService,
            EntryType.ResourceAdapterService, EntryType.FactoryConfigurationAdapterService);

        StringBuilder sb = new StringBuilder();
        sb.append("Parsed annotation for class ");
        sb.append(m_className);
        for (int i = m_writers.size() - 1; i >= 0; i--)
        {
            sb.append("\n\t").append(m_writers.get(i).toString());
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
        // The last element our our m_writers list contains either the Service, or the AspectService descriptor.
        for (int i = m_writers.size() - 1; i >= 0; i--)
        {
            pw.println(m_writers.get(i).toString());
        }
    }
}
