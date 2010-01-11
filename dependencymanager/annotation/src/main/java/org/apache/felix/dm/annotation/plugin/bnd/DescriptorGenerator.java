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

            for (Clazz c : expanded)
            {
                clazz = c;
                // Let's parse all annotations from that class !
                AnnotationCollector reader = new AnnotationCollector(this);
                c.parseClassFileWithCollector(reader);
                reader.finish();
                // And store the generated component descriptors in our resource list.
                String name = c.getFQN();
                Resource resource = createComponentResource(reader);
                m_resources.put("OSGI-INF/" + name + ".dm", resource);
                annotationsFound = true;
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
            sb.append(err.toString());
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
}
