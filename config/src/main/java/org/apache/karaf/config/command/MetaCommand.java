/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.command;

import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.config.command.completers.ConfigurationCompleter;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

@Command(scope = "config", name = "meta", description = "Lists meta type information.")
@Service
public class MetaCommand extends ConfigCommandSupport {
    @Option(name = "-p", aliases = "--pid", description = "The configuration pid", required = true, multiValued = false)
    @Completion(ConfigurationCompleter.class)
    protected String pid;

    @Reference
    BundleContext context;

    private Map<Integer, String> typeMap;

    public MetaCommand() {
        typeMap = new HashMap<>();
        typeMap.put(AttributeDefinition.BOOLEAN, "boolean");
        typeMap.put(AttributeDefinition.BYTE, "byte");
        typeMap.put(AttributeDefinition.CHARACTER, "char");
        typeMap.put(AttributeDefinition.DOUBLE, "double");
        typeMap.put(AttributeDefinition.FLOAT, "float");
        typeMap.put(AttributeDefinition.INTEGER, "int");
        typeMap.put(AttributeDefinition.LONG, "long");
        typeMap.put(AttributeDefinition.PASSWORD, "password");
        typeMap.put(AttributeDefinition.SHORT, "short");
        typeMap.put(AttributeDefinition.STRING, "String");
    }

    @Override
    public Object doExecute() throws Exception {
        try {
            new InnerCommand().doExecute();
        } catch (NoClassDefFoundError e) {
            System.out
                    .println("No MetaTypeService present. You need to install an implementation to use this command.");
        }
        return null;
    }

    class InnerCommand {

        protected Object doExecute() throws Exception {

            ServiceReference<MetaTypeService> ref = context.getServiceReference(MetaTypeService.class);
            if (ref == null) {
                System.out
                        .println("No MetaTypeService present. You need to install an implementation to use this command.");
            }
            MetaTypeService metaTypeService = context.getService(ref);
            ObjectClassDefinition def = getMetatype(metaTypeService, pid);
            context.ungetService(ref);

            if (def == null) {
                System.out.println("No meta type definition found for pid: " + pid);
                return null;
            }
            System.out.println("Meta type informations for pid: " + pid);
            ShellTable table = new ShellTable();
            table.column("key");
            table.column("name");
            table.column("type");
            table.column("default");
            table.column("description");
            AttributeDefinition[] attrs = def.getAttributeDefinitions(ObjectClassDefinition.ALL);
            if (attrs != null) {
                for (AttributeDefinition attr : attrs) {
                    table.addRow().addContent(attr.getID(), attr.getName(), getType(attr.getType()),
                            getDefaultValueStr(attr.getDefaultValue()), attr.getDescription());
                }
            }
            table.print(System.out);
            return null;
        }

        private String getType(int type) {
            return typeMap.get(type);
        }

        private String getDefaultValueStr(String[] defaultValues) {
            if (defaultValues == null) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (String defaultValue : defaultValues) {
                if (first) {
                    first = false;
                } else {
                    result.append(",");
                }
                result.append(defaultValue);
            }
            return result.toString();
        }

        public ObjectClassDefinition getMetatype(MetaTypeService metaTypeService, String pid) {
            for (Bundle bundle : context.getBundles()) {
                MetaTypeInformation info = metaTypeService.getMetaTypeInformation(bundle);
                if (info == null) {
                    continue;
                }
                String[] pids = info.getPids();
                for (String cPid : pids) {
                    if (cPid.equals(pid)) {
                        return info.getObjectClassDefinition(cPid, null);
                    }
                }
            }
            return null;
        }

    }
}
