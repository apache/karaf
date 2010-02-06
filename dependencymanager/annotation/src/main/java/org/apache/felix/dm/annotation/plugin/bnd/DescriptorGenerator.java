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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Service;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.EmbeddedResource;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.Resource;
import aQute.lib.osgi.Clazz.QUERY;

/**
 * This helper parses all classes which contain DM annotations, and generates the corresponding component descriptors.
 */
public class DescriptorGenerator extends Processor
{
    /**
     * This is the bnd analyzer used to lookup classes containing DM annotations.
     */
    private Analyzer m_analyzer;

    /**
     * This is the generated Dependency Manager descriptors. The hashtable key is the path
     * to a descriptor. The value is a bnd Resource object which contains the content of a
     * descriptor. 
     */
    Map<String, Resource> m_resources = new HashMap<String, Resource>();

    /**
     * This is the generated MetaType XML descriptor, if any Properties/Property annotations have been found.
     */
    private Resource m_metaTypeResource;

    /**
     * Creates a new descriptor generator.
     * @param analyzer The bnd analyzer used to lookup classes containing DM annotations.
     */
    public DescriptorGenerator(Analyzer analyzer)
    {
        super(analyzer);
        m_analyzer = analyzer;
    }

    /**
     * Starts the scanning.
     * @return true if some annotations were successfully parsed, false if not. corresponding generated 
     * descriptors can then be retrieved by invoking the getDescriptors/getDescriptorPaths methods.
     */
    public boolean execute()
    {
        boolean annotationsFound = false;
        Clazz clazz = null;
        try
        {
            // Try to locate any classes in the wildcarded universe
            // that are annotated with the DependencyManager "Service" annotations.
            Collection<Clazz> expanded = m_analyzer.getClasses("",
                // Then limit the ones with component annotations.
                QUERY.ANNOTATION.toString(), Service.class.getName(),
                // Parse everything
                QUERY.NAMED.toString(), "*");

            // Create the object which will collect Config Admin MetaTypes.
            MetaType metaType = new MetaType();
            
            for (Clazz c : expanded)
            {
                clazz = c;
                // Let's parse all annotations from that class !
                AnnotationCollector reader = new AnnotationCollector(this, metaType);
                c.parseClassFileWithCollector(reader);
                reader.finish();
                // And store the generated component descriptors in our resource list.
                String name = c.getFQN();
                Resource resource = createComponentResource(reader);
                m_resources.put("OSGI-INF/" + name + ".dm", resource);
                annotationsFound = true;
            }

            // If some Meta Types have been parsed, then creates the corresponding resource file.
            if (metaType.getSize() > 0) {
                m_metaTypeResource = createMetaTypeResource(metaType);
            }
            return annotationsFound;
        }
        catch (Throwable err)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Error while scanning annotations");
            if (clazz != null)
            {
                sb.append(" from class " + clazz);
            }
            sb.append(": ");
            sb.append(parse(err));
            error(sb.toString(), err.getCause());
            return false;
        }

        finally
        {
            // Collect all logs (warns/errors) from our processor and store them into the analyze.
            // Bnd will log them, if necessary.
            m_analyzer.getInfo(this, "DependencyManager: ");
            close();
        }
    }

    /**
     * Returns the path of the descriptor.
     * @return the path of the generated descriptors.
     */
    public String getDescriptorPaths()
    {
        StringBuilder descriptorPaths = new StringBuilder();
        String del = "";
        for (Map.Entry<String, Resource> entry : m_resources.entrySet())
        {
            descriptorPaths.append(del);
            descriptorPaths.append(entry.getKey());
            del = ",";
        }
        return descriptorPaths.toString();
    }

    /**
     * Returns the list of the generated descriptors.
     * @return the list of the generated descriptors.
     */
    public Map<String, Resource> getDescriptors()
    {
        return m_resources;
    }

    /**
     * Returns the MetaType resource.
     */
    public Resource getMetaTypeResource() {
        return m_metaTypeResource;
    }
    
    /**
     * Creates a bnd resource that contains the generated dm descriptor.
     * @param collector 
     * @return
     * @throws IOException
     */
    private Resource createComponentResource(AnnotationCollector collector) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        collector.writeTo(pw);
        pw.close();
        byte[] data = out.toByteArray();
        out.close();
        return new EmbeddedResource(data, 0);
    }
    
    /**
     * Creates a bnd resource that contains the generated metatype descriptor.
     * @param metaType the Object that has collected all meta type informations.
     * @return the meta type resource
     * @throws IOException on any errors
     */
    private Resource createMetaTypeResource(MetaType metaType) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        metaType.writeTo(pw);
        pw.close();
        byte[] data = out.toByteArray();
        out.close();
        return new EmbeddedResource(data, 0);    
    }
    
    /**
     * Parse an exception into a string.
     * @param e The exception to parse
     * @return the parsed exception
     */
    private static String parse(Throwable e) {
      StringWriter buffer = new StringWriter();
      PrintWriter  pw = new PrintWriter(buffer);
      e.printStackTrace(pw);
      return (buffer.toString());
    } 
}
