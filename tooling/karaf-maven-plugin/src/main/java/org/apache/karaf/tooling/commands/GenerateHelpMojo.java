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
package org.apache.karaf.tooling.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;

/**
 * Generates help documentation for Karaf commands
 */
@Mojo(name = "commands-generate-help", defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        requiresDependencyResolution = ResolutionScope.RUNTIME, inheritByDefault = false, threadSafe = true)
public class GenerateHelpMojo extends AbstractMojo {

    /**
     * The output folder
     */
    @Parameter(defaultValue = "${project.build.directory}/docbkx/sources")
    protected File targetFolder;

    /**
     * The output format
     */
    @Parameter(defaultValue = "docbx")
    protected String format;

    /**
     * The classloader to use for loading the commands.
     * Can be "project" or "plugin"
     */
    @Parameter(defaultValue = "project")
    protected String classLoader;

    /**
     * Includes the --help command output in the generated documentation
     */
    @Parameter(defaultValue = "true")
    protected boolean includeHelpOption;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    private static final String FORMAT_CONF = "conf";
    private static final String FORMAT_DOCBX = "docbx";
    private static final String FORMAT_ASCIIDOC = "asciidoc";

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!FORMAT_DOCBX.equals(format) && !FORMAT_CONF.equals(format) && !FORMAT_ASCIIDOC.equals(format)) {
                throw new MojoFailureException("Unsupported format: " + format + ". Supported formats are: asciidoc, docbx, or conf.");
            }
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }

            ClassFinder finder = createFinder(classLoader);
            List<Class<?>> classes = finder.findAnnotatedClasses(Command.class);
            if (classes.isEmpty()) {
                throw new MojoFailureException("No command found");
            }

            CommandHelpPrinter helpPrinter = null;
            if (FORMAT_ASCIIDOC.equals(format)) {
                helpPrinter = new AsciiDoctorCommandHelpPrinter();
            }
            if (FORMAT_CONF.equals(format)) {
                helpPrinter = new UserConfCommandHelpPrinter();
            }
            if (FORMAT_DOCBX.equals(format)) {
                helpPrinter = new DocBookCommandHelpPrinter();
            }

            Map<String, Set<String>> commands = new TreeMap<>();

            String commandSuffix = null;
            if (FORMAT_ASCIIDOC.equals(format)) {
                commandSuffix = "adoc";
            }
            if (FORMAT_CONF.equals(format)) {
                commandSuffix = "conf";
            }
            if (FORMAT_DOCBX.equals(format)) {
                commandSuffix = "xml";
            }
            for (Class<?> clazz : classes) {
                try {
                    Action action = (Action) clazz.newInstance();
                    Command cmd = clazz.getAnnotation(Command.class);

                    // skip the *-help command
                    if (cmd.scope().equals("*")) continue;

                    File output = new File(targetFolder, cmd.scope() + "-" + cmd.name() + "." + commandSuffix);
                    FileOutputStream outStream = new FileOutputStream(output);
                    PrintStream out = new PrintStream(outStream);
                    helpPrinter.printHelp(action, out, includeHelpOption);
                    out.close();
                    outStream.close();

                    commands.computeIfAbsent(cmd.scope(), k -> new TreeSet<>()).add(cmd.name());
                    getLog().info("Found command: " + cmd.scope() + ":" + cmd.name());
                } catch (Exception e) {
                    getLog().warn("Unable to write help for " + clazz.getName(), e);
                }
            }

            String overViewSuffix = null;
            if (FORMAT_ASCIIDOC.equals(format)) {
                overViewSuffix = "adoc";
            }
            if (FORMAT_CONF.equals(format)) {
                overViewSuffix = "conf";
            }
            if (FORMAT_DOCBX.equals(format)) {
                overViewSuffix = "xml";
            }
            PrintStream writer = new PrintStream(new FileOutputStream(new File(targetFolder, "commands." + overViewSuffix)));
            helpPrinter.printOverview(commands, writer);
            writer.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Error building commands help", e);
        }
    }

    private ClassFinder createFinder(String classloaderType) throws
            Exception {
        ClassFinder finder;
        if ("project".equals(classloaderType)) {
            List<URL> urls = new ArrayList<>();
            for (Object object : project.getCompileClasspathElements()) {
                String path = (String) object;
                urls.add(new File(path).toURI().toURL());
            }
            ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
            finder = new ClassFinder(loader, urls);
        } else if ("plugin".equals(classLoader)) {
            finder = new ClassFinder(getClass().getClassLoader());
        } else {
            throw new MojoFailureException("classLoader attribute must be 'project' or 'plugin'");
        }
        return finder;
    }

}
