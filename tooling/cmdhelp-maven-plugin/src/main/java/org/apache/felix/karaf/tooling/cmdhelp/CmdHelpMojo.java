/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.tooling.cmdhelp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.karaf.shell.console.commands.BlueprintCommand;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;
import org.osgi.service.command.CommandSession;

/**
 * Generates docbook for Karaf commands
 *
 * @version $Revision: 1.1 $
 * @goal cmdhelp
 * @phase generate-resources
 * @execute phase="generate-resources"
 * @requiresDependencyResolution runtime
 * @inheritByDefault false
 * @description Generates help for Karaf commands
 */
public class CmdHelpMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * The output folder
     *
     * @parameter default-value="${project.build.directory}/docbkx/sources"
     */
    protected File targetFolder;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            
            List<URL> urls = new ArrayList<URL>();
            for (Object object : project.getCompileClasspathElements()) {
                String path = (String) object;
                urls.add(new File(path).toURI().toURL());
            }

            ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
            ClassFinder finder = new ClassFinder(loader, urls);
            List<Class> classes = finder.findAnnotatedClasses(Command.class);
            if (classes.isEmpty()) {
                throw new MojoFailureException("No command found");
            }

            Map<String, Set<String>> commands = new TreeMap<String, Set<String>>();

            for (Class clazz : classes) {
                try {
                    String help = new HelpPrinter(clazz).printHelp();
                    Command cmd = (Command) clazz.getAnnotation(Command.class);
                    File output = new File(targetFolder, cmd.scope() + "-" + cmd.name() + ".xml");
                    Writer writer = new OutputStreamWriter(new FileOutputStream(output));
                    writer.write(help);
                    writer.close();

                    Set<String> cmds = commands.get(cmd.scope());
                    if (cmds == null) {
                        cmds = new TreeSet<String>();
                        commands.put(cmd.scope(), cmds);
                    }
                    cmds.add(cmd.name());
                    getLog().info("Found command: " + cmd.scope() + ":" + cmd.name());
                } catch (Exception e) {
                    getLog().warn("Unable to write help for " + clazz.getName(), e);
                }
            }

            PrintStream writer = new PrintStream(new FileOutputStream(new File(targetFolder, "commands.xml")));
            writer.println("<chapter id='commands' xmlns:xi=\"http://www.w3.org/2001/XInclude\">");
            writer.println("  <title>Commands</title>");
            writer.println("  <toc></toc>");

            for (String key : commands.keySet()) {
                writer.println("  <section id='commands-" + key + "'>");
                writer.println("    <title>" + key + "</title>");
                for (String cmd : commands.get(key)) {
                    writer.println("    <xi:include href='" + key + "-" + cmd + ".xml' parse='xml'/>");
                }
                writer.println("  </section>");
            }
            writer.println("</chapter>");
            writer.close();

        } catch (Exception e) {
            throw new MojoExecutionException("Error building commands help", e);
        }
    }

    public static class HelpPrinter extends BlueprintCommand {

        private final Class<Action> actionClass;

        public HelpPrinter(Class<Action> actionClass) {
            this.actionClass = actionClass;
        }

        public String printHelp() throws Exception {
            PrintStream oldout = System.out;
            try {
                Action action = actionClass.newInstance();
                CommandSession session = new DummyCommandSession();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream newout = new PrintStream(baos);
                System.setOut(newout);
                new Preparator().prepare(action, session, Collections.<Object>singletonList("--help"));
                newout.close();
                baos.close();
                return baos.toString();
            } finally {
                System.setOut(oldout);
            }
        }

        protected class Preparator extends BlueprintActionPreparator {

            @Override
            protected void printUsage(CommandSession session, Command command, Set<Option> options, Set<Argument> args, PrintStream out)
            {
                List<Argument> arguments = new ArrayList<Argument>(args);
                Collections.sort(arguments, new Comparator<Argument>() {
                    public int compare(Argument o1, Argument o2) {
                        return Integer.valueOf(o1.index()).compareTo(Integer.valueOf(o2.index()));
                    }
                });
                options = new HashSet<Option>(options);
                options.add(HELP);

                out.println("<section>");
                out.print("  <title>");
                out.print(command.scope());
                out.print(":");
                out.print(command.name());
                out.println("</title>");
                out.println("  <section>");
                out.println("    <title>Description</title>");
                out.println("    <para>");
                out.println(command.description());
                out.println("    </para>");
                out.println("  </section>");

                StringBuffer syntax = new StringBuffer();
                syntax.append(String.format("%s:%s", command.scope(), command.name()));
                if (options.size() > 0) {
                    syntax.append(" [options]");
                }
                if (arguments.size() > 0) {
                    syntax.append(' ');
                    for (Argument argument : arguments) {
                        syntax.append(String.format(argument.required() ? "%s " : "[%s] ", argument.name()));
                    }
                }
                out.println("  <section>");
                out.println("    <title>Syntax</title>");
                out.println("    <para>");
                out.println(syntax.toString());
                out.println("    </para>");
                out.println("  </section>");

                if (arguments.size() > 0)
                {
                    out.println("  <section>");
                    out.println("    <title>Arguments</title>");
                    out.println("    <informaltable>");
                    for (Argument argument : arguments)
                    {
                        out.println("    <tr>");
                        out.println("      <td>" + argument.name() + "</td>");
                        out.println("      <td>" + argument.description() + "</td>");
                        out.println("    </tr>");
                    }

                    out.println("    </informaltable>");
                    out.println("  </section>");
                }
                if (options.size() > 0)
                {
                    out.println("  <section>");
                    out.println("    <title>Options</title>");
                    out.println("    <informaltable>");

                    for (Option option : options)
                    {
                        String opt = option.name();
                        for (String alias : option.aliases())
                        {
                            opt += ", " + alias;
                        }
                        out.println("    <tr>");
                        out.println("      <td>" + opt + "</td>");
                        out.println("      <td>" + option.description() + "</td>");
                        out.println("    </tr>");
                    }

                    out.println("    </informaltable>");
                    out.println("  </section>");
                }
                out.println("</section>");
            }

        }

        protected static class DummyCommandSession implements CommandSession {
            public Object convert(Class<?> type, Object instance) {
                return null;
            }
            public CharSequence format(Object target, int level) {
                return null;
            }
            public void put(String name, Object value) {
            }
            public Object get(String name) {
                return null;
            }
            public PrintStream getConsole() {
                return null;
            }
            public InputStream getKeyboard() {
                return null;
            }
            public void close() {
            }
            public Object execute(CharSequence commandline) throws Exception {
                return null;
            }
        }

    }

}
