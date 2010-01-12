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

import java.util.Map;

import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Resource;

/**
 * This class is a BND plugin. It scans the target bundle and look for DependencyManager annotations.
 * It can be directly used when using ant and can be referenced inside the ".bnd" descriptor, using
 * the "-plugin" parameter.
 */
public class AnnotationPlugin implements AnalyzerPlugin
{
    /**
     * This plugin is called after analysis of the JAR but before manifest
     * generation. When some DM annotations are found, the plugin will add the corresponding 
     * DM component descriptors under OSGI-INF/ directory. It will also set the  
     * "DependencyManager-Component" manifest header (which references the descriptor paths).
     * 
     * @param analyzer the object that is used to retrieve classes containing DM annotations.
     * @return true if the classpace has been modified so that the bundle classpath must be reanalyzed
     * @throws Exception on any errors.
     */
    public boolean analyzeJar(Analyzer analyzer) throws Exception
    {
        // We'll do the actual parsing using a DescriptorGenerator object.
        DescriptorGenerator generator = new DescriptorGenerator(analyzer);
        if (generator.execute())
        {
            // We have parsed some annotations: set the OSGi "DependencyManager-Component" header in the target bundle.
            analyzer.setProperty("DependencyManager-Component", generator.getDescriptorPaths());

            // And insert the generated descriptors into the target bundle.
            Map<String, Resource> resources = generator.getDescriptors();
            for (Map.Entry<String, Resource> entry : resources.entrySet())
            {
                analyzer.getJar().putResource(entry.getKey(), entry.getValue());
            }
        }
        return false;
    }
}
