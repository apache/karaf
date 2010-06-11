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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * This class parses files generated in OSGI-INF/*.dm by the DependencyManager bnd plugin.
 * Each descriptor contains a JSON definition of a Service, along with its corresponding  
 * dependencies.
 */
public class DescriptorParser
{
    private Map<String, ServiceComponentBuilder> m_builders = new HashMap<String, ServiceComponentBuilder>();

    public void addBuilder(ServiceComponentBuilder sb)
    {
        m_builders.put(sb.getType(), sb);
    }

    public void parse(BufferedReader reader, Bundle b, DependencyManager dm) throws Exception
    {
        String line;

        // The first line is a Service Component (a Service, an Aspect Service, etc ...)
        line = reader.readLine();
        Log.instance().log(LogService.LOG_DEBUG, "DescriptorParser: parsing service %s", line);
        JSONObject json = new JSONObject(line);
        JSONMetaData serviceMetaData = new JSONMetaData(json);

        String type = (String) json.get("type");
        ServiceComponentBuilder builder = m_builders.get(type);
        if (builder == null)
        {
            throw new IllegalArgumentException("Invalid descriptor"
                + ": no \"type\" parameter found in first line");
        }

        // Parse the rest of the lines (dependencies)
        List<MetaData> serviceDependencies = new ArrayList<MetaData>();
        while ((line = reader.readLine()) != null)
        {
            Log.instance().log(LogService.LOG_DEBUG, "Parsing dependency Ms", line);
            JSONObject dep = new JSONObject(line);
            serviceDependencies.add(new JSONMetaData(dep));
        }

        // and Invoke the builder
        builder.buildService(serviceMetaData, serviceDependencies, b, dm);
    }
}
