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

import static org.apache.karaf.config.core.impl.MetaServiceCaller.doWithMetaType;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.karaf.config.command.completers.MetaCompleter;
import org.apache.karaf.config.core.impl.MetaServiceCaller.MetaInfo;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.CommandException;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

@Command(scope = "config", name = "meta", description = "Lists meta type information.")
@Service
public class MetaCommand extends ConfigCommandSupport {

    @Argument(name = "pid", description = "The configuration pid", required = true, multiValued = false)
    @Completion(MetaCompleter.class)
    protected String pid;

    @Option(name = "-c", description = "Create respective config from metatype defaults", required = false, multiValued = false)
    protected boolean create;

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
            if (create) {
                doWithMetaType(context, pid, new Create());
            } else {
                doWithMetaType(context, pid, new Print());
            }
            return null;
        } catch (Throwable e) {
            Throwable ncdfe = e;
            while (ncdfe != null && !(ncdfe instanceof NoClassDefFoundError)) {
                ncdfe = ncdfe.getCause();
            }
            if (ncdfe != null && ncdfe.getMessage().equals("org/osgi/service/metatype/MetaTypeService")) {
                throw new CommandException("config:meta disabled because the org.osgi.service.metatype package is not wired", e);
            } else {
                throw e;
            }
        }
    }
        
    abstract class AbstractMeta implements Function<Optional<MetaInfo>, Void> {
        protected String getDefaultValueStr(String[] defaultValues) {
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
      
    }
    
    class Create extends AbstractMeta {

        public Void apply(Optional<MetaInfo> info) {
            if (!info.isPresent()) {                
                System.out.println("No meta type definition found for pid: " + pid);
                return null;
            }
            
            try {
                createDefaultConfig(pid, info.get());
            } catch (IOException e) {
                 throw new RuntimeException(e.getMessage(), e);
            }
            return null;
        }
        
        private void createDefaultConfig(String pid, MetaInfo info) throws IOException {
            AttributeDefinition[] attrs = info.getDefinition().getAttributeDefinitions(ObjectClassDefinition.ALL);
            if (attrs == null) {
                return;
            }
            ConfigurationAdmin configAdmin = configRepository.getConfigAdmin();
            Configuration config;
            if(info.isFactory()) {
                config = configAdmin.createFactoryConfiguration(pid, null);
            }
            else {
                config = configAdmin.getConfiguration(pid, null);
            }            
            Dictionary<String, Object> props = new Hashtable<>();
            for (AttributeDefinition attr : attrs) {
                String valueStr = getDefaultValueStr(attr.getDefaultValue());
                if (valueStr != null) {
                    props.put(attr.getID(), valueStr);
                }
            }
            config.update(props);
        }

    }
    
    class Print extends AbstractMeta {
        public Void apply(Optional<MetaInfo> info) {           
            if (!info.isPresent()) {
                System.out.println("No meta type definition found for pid: " + pid);
                return null;
            }
            if(info.get().isFactory()) {
                System.out.println("Meta type informations for factory pid: " + pid);
            }
            else {
                System.out.println("Meta type informations for pid: " + pid);
            }
            ShellTable table = new ShellTable();
            table.column("key");
            table.column("name");
            table.column("type");
            table.column("default");
            table.column("description").wrap();
            AttributeDefinition[] attrs = info.get().getDefinition().getAttributeDefinitions(ObjectClassDefinition.ALL);
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

    }        
}
