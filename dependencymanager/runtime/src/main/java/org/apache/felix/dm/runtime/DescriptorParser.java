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
package org.apache.felix.dm.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.log.LogService;

/**
 * This class parses files generated in OSGI-INF/*.dm by the DependencyManager bnd plugin.
 * Each descriptor contains the definition of a Service, along with its corresponding service dependency or configuration dependencies.
 * Here is an example of a typical descriptor syntax:
 *   
 *    Service: start=start; stop=stop; impl=org.apache.felix.dm.test.annotation.ServiceConsumer; 
 *    ServiceDependency: service=org.apache.felix.dm.test.annotation.ServiceInterface; autoConfig=m_service; 
 *    ServiceDependency: added=bind; service=org.apache.felix.dm.test.annotation.ServiceInterface; 
 * 
 * Notice that the descriptor must start with a "Service" definition. (Dependencies must be declared after the "Service" entry).
 * <p>
 * 
 * Now, here is the formal BNF definition of the descriptor syntax:
 *
 *    line := <type> ':' <params>
 *    
 *    type := service|serviceDependency|configurationDependency
 *    service := 'Service'
 *    serviceDependency := 'ServiceDependency'
 *    configurationDependency := 'ConfigurationDependency'
 *    
 *    params := paramName '=' paramValue ( ';' paramName '=' paramValue )*
 *    
 *    paramName := init | start | stop | destroy | impl | provide | properties | factory | factoryMethod | composition | service | filter | 
 *                 defaultImpl | required | added | changed | removed | autoConfig | pid | propagate | updated
 *    init := 'init'
 *    start := 'start'
 *    stop := 'stop'
 *    destroy := 'destroy'
 *    impl := 'impl'
 *    provide := 'provide'
 *    properties := 'properties'
 *    factory := 'factory'
 *    factoryMethod := 'factoryMethod'
 *    composition := 'composition'
 *    service := 'service'
 *    filter := 'filter'
 *    defaultImpl := 'defaultImpl'
 *    required := 'required'
 *    added := 'added'
 *    changed := 'changed'
 *    removed := 'removed'
 *    autoConfig := 'autoConfig'
 *    pid := 'pid'
 *    propagate := 'propagate'
 *    updated := 'updated'
 *    
 *    paramValue := strings | attributes
 *    strings := string ( ',' string )*
 *    attributes := string ':' string ( ',' string : string )*
 *    string := [alphanum string]
 */
public class DescriptorParser
{
    private LogService m_logService;
    private Map<DescriptorParam, Object> m_params = new HashMap<DescriptorParam, Object>();

    private final static Pattern linePattern = Pattern.compile("(\\w+):\\s*(.*)", Pattern.COMMENTS);
    private final static Pattern paramPattern = Pattern.compile("([^=]+)=([^;]+);?");
    private final static Pattern stringsPattern = Pattern.compile("([^,]+)");
    private final static Pattern attributesPattern = Pattern.compile("([^:]+):([^,]+),?");

    public DescriptorParser(LogService service)
    {
        m_logService = service;
    }

    /*
     * Parses a DependencyManager component descriptor entry (either a Service, a ServiceDependency, or a ConfigurationDependency entry).
     * @return DescriptorEntry.Service, DescriptorEntry.ServiceDependency, or DescriptorEntry.ConfigurationDependency 
     */
    public DescriptorEntry parse(String line)
    {
        m_params.clear();
        Matcher lineMatcher = linePattern.matcher(line);
        if (lineMatcher.matches())
        {
            DescriptorEntry entry = DescriptorEntry.valueOf(lineMatcher.group(1).trim());
            Matcher paramMatcher = paramPattern.matcher(lineMatcher.group(2).trim());
            while (paramMatcher.find())
            {
                DescriptorParam paramName = DescriptorParam.valueOf(paramMatcher.group(1).trim());
                String paramValue = paramMatcher.group(2).trim();
                Matcher attributesMatcher = attributesPattern.matcher(paramValue);
                boolean matched = false;

                Hashtable<String, String> attributes = new Hashtable<String, String>();
                while (attributesMatcher.find())
                {
                    matched = true;
                    attributes.put(attributesMatcher.group(1).trim(),
                        attributesMatcher.group(2).trim());
                }
                m_params.put(paramName, attributes);

                if (!matched)
                {
                    Matcher stringsMatcher = stringsPattern.matcher(paramValue);
                    if (stringsMatcher.groupCount() > 0)
                    {
                        List<String> strings = new ArrayList<String>();
                        while (stringsMatcher.find())
                        {
                            strings.add(stringsMatcher.group(1).trim());
                        }
                        m_params.put(paramName, strings.toArray(new String[strings.size()]));
                    }
                }
            }

            m_logService.log(LogService.LOG_DEBUG, "Parsed " + entry + ": " + toString());
            return entry;
        }
        else
        {
            throw new IllegalArgumentException("Invalid descriptor entry: " + line);
        }
    }

    /**
     * Once a component descriptor entry line is parsed, you can retrieve entry attributes using this method.
     * @param param
     * @return
     */
    public String getString(DescriptorParam param)
    {
        Object value = m_params.get(param);
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter " + param + " not found");
        }
        if (!(value instanceof String[]))
        {
            throw new IllegalArgumentException("Parameter " + param + " not a String array");
        }
        String[] array = (String[]) value;
        if (array.length < 1)
        {
            throw new IllegalArgumentException("Parameter " + param + " not found");
        }
        return (array[0]);
    }

    /**
     * Once a component descriptor entry line is parsed, you can retrieve entry attributes using this method.
     *
     * @param param
     * @param def
     * @return
     */
    public String getString(DescriptorParam param, String def)
    {
        try
        {
            return getString(param);
        }
        catch (IllegalArgumentException e)
        {
            return def;
        }
    }

    /**
     * Once a component descriptor entry line is parsed, you can retrieve entry attributes using this method.
     * @param param
     * @return
     */
    public String[] getStrings(DescriptorParam param)
    {
        Object value = m_params.get(param);
        if (value == null)
        {
            throw new IllegalArgumentException("Parameter " + param + " not found");
        }
        if (!(value instanceof String[]))
        {
            throw new IllegalArgumentException("Parameter " + param + " not a String array");
        }
        return (String[]) value;
    }

    /**
     * Once a component descriptor entry line is parsed, you can retrieve entry attributes using this method.
     * @param param
     * @return
     */
    public String[] getStrings(DescriptorParam param, String[] def)
    {
        try
        {
            return getStrings(param);
        }
        catch (IllegalArgumentException e)
        {
            return def;
        }
    }

    /**
     * Once a component descriptor entry line is parsed, you can retrieve entry attributes using this method.
     * @param param
     * @return
     */
    @SuppressWarnings("unchecked")
    public Dictionary<String, String> getDictionary(DescriptorParam param, Dictionary<String, String> def)
    {
        Object value = m_params.get(param);
        if (value == null)
        {
            return def;
        }
        if (!(value instanceof Dictionary))
        {
            throw new IllegalArgumentException("Parameter " + param + " not Dictionary");
        }
        return (Dictionary<String, String>) value;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<DescriptorParam, Object> entry : m_params.entrySet())
        {
            sb.append(entry.getKey());
            sb.append("=");
            Object val = entry.getValue();
            if (val instanceof String || val instanceof Dictionary<?, ?>)
            {
                sb.append(val.toString());
            }
            else if (val instanceof String[])
            {
                sb.append(Arrays.toString((String[]) val));
            }
            else
            {
                sb.append(val.toString());
            }
            sb.append(";");
        }
        return sb.toString();
    }
}
