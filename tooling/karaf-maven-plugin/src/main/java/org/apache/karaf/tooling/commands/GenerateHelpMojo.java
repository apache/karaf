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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.meta.ActionMetaData;
import org.apache.karaf.shell.commands.meta.ActionMetaDataFactory;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.xbean.finder.ClassFinder;

/**
 * Generates help documentation for Karaf commands
 *
 * @goal commands-generate-help
 * @phase generate-resources
 * @execute phase="generate-resources"
 * @requiresDependencyResolution runtime
 * @inheritByDefault false
 * @description Generates help for Karaf commands
 */
public class GenerateHelpMojo extends AbstractMojo {

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

    /**
     * The output format
     *
     * @parameter default-value="docbx" expression="${format}"
     */
    protected String format;

    /**
     * The classloader to use for loading the commands.
     * Can be "project" or "plugin"
     *
     * @parameter default-value="project"
     */
    protected String classLoader;

    /**
     * Includes the --help command output in the generated documentation
     *
     * @parameter default_value="true"
     */
    protected boolean includeHelpOption;

    private static final String FORMAT_CONF = "conf";
    private static final String FORMAT_DOCBX = "docbx";

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (!FORMAT_DOCBX.equals(format) && !FORMAT_CONF.equals(format)) {
                throw new MojoFailureException("Unsupported format: " + format + ". Supported formats are: docbx or conf.");
            }
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }

            ClassFinder finder = createFinder(classLoader);
            List<Class<?>> classes = finder.findAnnotatedClasses(Command.class);
            if (classes.isEmpty()) {
                throw new MojoFailureException("No command found");
            }
            
            CommandHelpPrinter helpPrinter = FORMAT_DOCBX.equals(format) 
                ? new DocBookCommandHelpPrinter()
                : new UserConfCommandHelpPrinter();

            Map<String, Set<String>> commands = new TreeMap<String, Set<String>>();

            String commandSuffix = FORMAT_DOCBX.equals(format) ? "xml" : "conf"; 
            for (Class<?> clazz : classes) {
                try {
                    Action action = (Action) clazz.newInstance();
                    ActionMetaData meta = new ActionMetaDataFactory().create(action.getClass());
                    Command cmd = meta.getCommand();

                    // skip the *-help command
                    if (cmd.scope().equals("*")) continue;

                    File output = new File(targetFolder, cmd.scope() + "-" + cmd.name() + "." + commandSuffix);
                    FileOutputStream outStream = new FileOutputStream(output);
                    PrintStream out = new PrintStream(outStream);
                    helpPrinter.printHelp(action, meta, out, includeHelpOption);
                    out.close();
                    outStream.close();

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

            String overViewSuffix = FORMAT_DOCBX.equals(format) ? "xml" : "conf";
            PrintStream writer = new PrintStream(new FileOutputStream(new File(targetFolder, "commands." + overViewSuffix)));
            helpPrinter.printOverview(commands, writer);
            writer.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Error building commands help", e);
        }
    }

    private ClassFinder createFinder(String classloaderType) throws DependencyResolutionRequiredException, MalformedURLException,
        Exception, MojoFailureException {
        ClassFinder finder;
        if ("project".equals(classloaderType)) {
            List<URL> urls = new ArrayList<URL>();
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
