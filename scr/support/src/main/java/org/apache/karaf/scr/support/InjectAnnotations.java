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
package org.apache.karaf.scr.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.component.HeaderReader;
import aQute.bnd.component.TagResource;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.URLResource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.tag.Tag;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Function;
import org.apache.karaf.scr.support.ScrCommandSupport;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.inject.Destroy;
import org.apache.karaf.shell.inject.Init;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;

import static aQute.bnd.osgi.Constants.COMPONENT_ACTIVATE;
import static aQute.bnd.osgi.Constants.COMPONENT_DEACTIVATE;
import static aQute.bnd.osgi.Constants.COMPONENT_PROPERTIES;
import static aQute.bnd.osgi.Constants.COMPONENT_PROVIDE;

/**
 *
 */
public class InjectAnnotations implements AnalyzerPlugin {

    private Analyzer analyzer;

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        try {
            return doAnalyzeJar(analyzer);
        } catch (Exception t) {
            t.printStackTrace(System.out);
            throw t;
        }
    }

    protected boolean doAnalyzeJar(Analyzer analyzer) throws Exception {
        this.analyzer = analyzer;

        System.out.println("\nLooking for @Service annotated classes\n");

        Collection<Clazz> annotatedComponents = analyzer.getClasses("", Clazz.QUERY.ANNOTATED.toString(), Service.class.getName());

        List<String> components = new ArrayList<String>();


        for (Clazz clazz : annotatedComponents) {
            System.out.println("\nFound @Service annotated class: " + clazz);

            if (clazz.is(Clazz.QUERY.ANNOTATED, new Instruction(Command.class.getName()), analyzer)) {
                System.out.println("\tCommand");
                Collector collector = new Collector();
                clazz.parseClassFileWithCollector(collector);

                Map<String, String> info = new LinkedHashMap<String, String>();
                info.put(COMPONENT_ACTIVATE, "activate");
                info.put(COMPONENT_DEACTIVATE, "deactivate");
                for (String key : collector.references.keySet()) {
                    info.put(key, collector.references.get(key));
                }
                info.put(COMPONENT_PROVIDE, Processor.join(Arrays.asList(new String[] {
                        Function.class.getName(),
                        CompletableFunction.class.getName(),
                        CommandWithAction.class.getName(),
//                        AbstractCommand.class.getName()
                })));

                List<String> properties = new ArrayList<String>();
                properties.add(CommandProcessor.COMMAND_SCOPE + "=" + collector.command.get("scope"));
                properties.add(CommandProcessor.COMMAND_FUNCTION + "=" + collector.command.get("name"));
                properties.add("hidden.component=true");
                info.put(COMPONENT_PROPERTIES, Processor.join(properties));

                TagResource resource = createComponentResource(clazz.getFQN(), ScrCommandSupport.class.getName(), info);
                analyzer.getJar().putResource("OSGI-INF/" + clazz.getFQN() + ".xml", resource);
                components.add("OSGI-INF/" + clazz.getFQN() + ".xml");
                resource.write(System.out);
                System.out.println();

            } else {
                System.out.println("\tNot a command");
                Collector collector = new Collector();
                clazz.parseClassFileWithCollector(collector);

                Map<String, String> info = new LinkedHashMap<String, String>();
                if (collector.init != null) {
                    info.put(COMPONENT_ACTIVATE, collector.init);
                }
                if (collector.destroy != null) {
                    info.put(COMPONENT_DEACTIVATE, collector.destroy);
                }
                for (String key : collector.references.keySet()) {
                    info.put(key, collector.references.get(key));
                }
                info.put(COMPONENT_PROVIDE, Processor.join(collector.allClasses));
                List<String> properties = new ArrayList<String>();
                properties.add("hidden.component=true");
                info.put(COMPONENT_PROPERTIES, Processor.join(properties));

                TagResource resource = createComponentResource(clazz.getFQN(), clazz.getFQN(), info);
                analyzer.getJar().putResource("OSGI-INF/" + clazz.getFQN() + ".xml", resource);
                components.add("OSGI-INF/" + clazz.getFQN() + ".xml");
                resource.write(System.out);
                System.out.println();
            }

        }

        String name = ScrCommandSupport.class.getName().replace('.', '/') + ".class";
        analyzer.getJar().putResource(name, new URLResource(ScrCommandSupport.class.getClassLoader().getResource(name)));
        String pkg = ScrCommandSupport.class.getName();
        pkg = pkg.substring( 0, pkg.lastIndexOf( '.' ) );
        Descriptors.PackageRef pkgRef = analyzer.getPackageRef( pkg );
        if ( !analyzer.getContained().containsKey( pkgRef ) ) {
            analyzer.getContained().put(pkgRef);
        }
        String[] imports = new String[] {
                "org.apache.felix.gogo.commands",
                "org.apache.karaf.shell.commands.basic",
                "org.apache.karaf.shell.console",
                "org.apache.karaf.shell.inject",
                "org.osgi.framework",
                "org.osgi.service.component",
                "org.slf4j"
        };
        for (String importPkg : imports) {
            pkgRef = analyzer.getPackageRef( importPkg );
            if ( !analyzer.getReferred().containsKey( pkgRef ) ) {
                analyzer.getReferred().put( pkgRef );
            }
        }

        String prop = analyzer.getProperty(Constants.SERVICE_COMPONENT);
        for (String comp : components) {
            if (prop == null || prop.length() == 0) {
                prop = comp;
            } else {
                prop = prop + "," + comp;
            }
        }
        analyzer.setProperty(Constants.SERVICE_COMPONENT, prop);

        return false;
    }

    TagResource createComponentResource(String name, String impl, Map<String, String> info)
            throws Exception {
        Tag tag = new HeaderReader(analyzer).createComponentTag(name, impl, info);
        return new TagResource(tag);
    }

    class Collector extends ClassDataCollector {
        Descriptors.TypeRef zuper;
        Clazz.MethodDef method;
        Clazz.FieldDef field;

        String init;
        String destroy;
        Annotation command;
        Map<String, String> references = new LinkedHashMap<String, String>();
        List<String> allClasses = new ArrayList<String>();

        @Override
        public void classBegin(int access, Descriptors.TypeRef name) {
            if (!name.getFQN().equals(Object.class.getName())) {
                allClasses.add(name.getFQN());
            }
        }

        public void implementsInterfaces(Descriptors.TypeRef[] interfaces) throws Exception {
            if (interfaces != null) {
                for (Descriptors.TypeRef ref : interfaces) {
                    allClasses.add(ref.getFQN());
                }
            }
        }

        @Override
        public void extendsClass(Descriptors.TypeRef zuper) throws Exception {
            this.zuper = zuper;
        }

        @Override
        public void field(Clazz.FieldDef defined) {
            field = defined;
        }

        @Override
        public void method(Clazz.MethodDef defined) {
            method = defined;
        }

        @Override
        public void annotation(Annotation annotation) {
            String name = annotation.getName().getFQN();
            if (Command.class.getName().equals(name)) {
                System.out.println("\tCommand: " + annotation.get("scope") + ":" + annotation.get("name"));
                command = annotation;
            }
            if (Reference.class.getName().equals(name)) {
                System.out.println("\tReference: field=" + field.getName() + ", type=" + field.getType().getFQN());
                references.put(field.getName(), field.getType().getFQN());
            }
            if (Init.class.getName().equals(name)) {
                if (init == null) {
                    System.out.println("\tInit method: " + method.getName());
                    init = method.getName();
                }
            }
            if (Destroy.class.getName().equals(name)) {
                if (destroy == null) {
                    System.out.println("\tDestroy method: " + method.getName());
                    destroy = method.getName();
                }
            }
        }

        @Override
        public void classEnd() throws Exception {
            if (zuper != null) {
                Clazz clazz = analyzer.findClass(zuper);
                zuper = null;
                if (clazz != null) {
                    clazz.parseClassFileWithCollector(this);
                }
            }
        }
    }

    static boolean isAnnotated(Clazz.Def def, Class annotation) {
        Collection<Descriptors.TypeRef> anns = def.getAnnotations();
        if (anns != null) {
            for (Descriptors.TypeRef ann : anns) {
                if (annotation.getName().equals(ann.getFQN())) {
                    return true;
                }
            }
        }
        return false;
    }

}
