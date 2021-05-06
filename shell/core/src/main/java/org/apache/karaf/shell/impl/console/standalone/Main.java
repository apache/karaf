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
package org.apache.karaf.shell.impl.console.standalone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.apache.karaf.shell.impl.console.JLineTerminal;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf.shell.impl.console.loader.JarInJarConstants;
import org.apache.karaf.shell.impl.console.loader.JarInJarURLStreamHandlerFactory;
import org.apache.karaf.shell.support.NameScoping;
import org.apache.karaf.shell.support.ShellUtil;
import org.jline.terminal.TerminalBuilder;

public class Main {

    private String application = System.getProperty("karaf.name", "root");
    private String user = "karaf";

    public static void main(String args[]) throws Exception {
        Package p = Package.getPackage("org.apache.karaf.shell.impl.console.standalone");
        if (p != null && p.getImplementationVersion() != null) {
            System.setProperty("karaf.version", p.getImplementationVersion());
        }
        Main main = new Main();
        main.run(args);
    }

    /**
     * Use this method when the shell is being executed as a top level shell.
     *
     * @param args the arguments.
     * @throws Exception in case of a failure.
     */
    public void run(String args[]) throws Exception {

        InputStream in = System.in;
        PrintStream out = System.out;
        PrintStream err = System.err;

        ThreadIOImpl threadio = new ThreadIOImpl();
        threadio.start();

        run(threadio, args, in, out, err);

        // TODO: do we need to stop the threadio that was started?
        // threadio.stop();
    }

    private void run(ThreadIO threadio, String[] args, InputStream in, PrintStream out, PrintStream err) throws Exception {
        StringBuilder sb = new StringBuilder();
        String classpath = null;
        boolean batch = false;
        String file = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--classpath=")) {
                classpath = arg.substring("--classpath=".length());
            } else if (arg.startsWith("-c=")) {
                classpath = arg.substring("-c=".length());
            } else if (arg.equals("--classpath") || arg.equals("-c")) {
                classpath = args[++i];
            } else if (arg.equals("-b") || arg.equals("--batch")) {
                batch = true;
            } else if (arg.startsWith("--file=")) {
                file = arg.substring("--file=".length());
            } else if (arg.startsWith("-f=")) {
                file = arg.substring("-f=".length());
            } else if (arg.equals("--file") || arg.equals("-f")) {
                file = args[++i];
            } else {
                sb.append(arg);
                sb.append(' ');
            }
        }

        if (file != null) {
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                sb.setLength(0);
                for (int c = reader.read(); c >= 0; c = reader.read()) {
                    sb.append((char) c);
                }
            }
        } else if (batch) {
            Reader reader = new BufferedReader(new InputStreamReader(System.in));
            sb.setLength(0);
            for (int c = reader.read(); c >= 0; reader.read()) {
                sb.append((char) c);
            }
        }

        ClassLoader cl = Main.class.getClassLoader();
        if (classpath != null) {
            List<URL> urls = getFiles(new File(classpath));
            // Load jars in class path to be able to load jars inside them
            ClassLoader tmpClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), cl);
            URL.setURLStreamHandlerFactory(new JarInJarURLStreamHandlerFactory( tmpClassLoader));
            List<URL> jarsInJars = getJarsInJars(urls);
            // Create ClassLoader with jars in jars and parent main ClassLoader
            cl = new URLClassLoader(jarsInJars.toArray(new URL[jarsInJars.size()]), cl);
            // Load original Jars with jarsInJars ClassLoader as parent.
            // This is needed cause if you load Class from main jar which depends on class in inner jar.
            // The loaded class has its class loader and resolve dependant classes in its or parent classloaders
            // which exclude jarInJar classloader.
            cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), cl);
        }

        SessionFactory sessionFactory = createSessionFactory(threadio);

        run(sessionFactory, sb.toString(), in, out, err, cl);
    }

    private List<URL> getJarsInJars(List<URL> urls) throws IOException, URISyntaxException {
        List<URL> result = new ArrayList<>();
        for (URL url : urls) {
            try (JarFile jarFile = new JarFile(url.toURI().getPath())) {
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    String embeddedArtifacts = manifest.getMainAttributes().getValue(JarInJarConstants.REDIRECTED_CLASS_PATH_MANIFEST_NAME);
                    if (embeddedArtifacts != null) {
                        String[] artifacts = embeddedArtifacts.split(",");
                        for (String artifact : artifacts) {
                            if (!artifact.endsWith(JarInJarConstants.JAR_EXTENSION)) {
                                continue;
                            }
                            result.add(new URL(JarInJarConstants.JAR_INTERNAL_URL_PROTOCOL_WITH_COLON + artifact + JarInJarConstants.JAR_INTERNAL_SEPARATOR));
                        }
                    }
                }
            }
        }
        return result;
    }

    private void run(final SessionFactory sessionFactory, String command, final InputStream in, final PrintStream out, final PrintStream err, ClassLoader cl) throws Exception {

        try (org.jline.terminal.Terminal jlineTerminal = TerminalBuilder.terminal()) {
            final Terminal terminal = new JLineTerminal(jlineTerminal);
            try (Session session = createSession(sessionFactory, command.length() > 0 ? null : in, out, err, terminal)) {
                session.put("USER", user);
                session.put("APPLICATION", application);

                discoverCommands(session, cl, getDiscoveryResource());

                if (command.length() > 0) {
                    // Shell is directly executing a sub/command, we don't setup a console
                    // in this case, this avoids us reading from stdin un-necessarily.
                    session.put(NameScoping.MULTI_SCOPE_MODE_KEY, Boolean.toString(isMultiScopeMode()));
                    session.put(Session.PRINT_STACK_TRACES, "execution");
                    try {
                        session.execute(command);
                    } catch (Throwable t) {
                        ShellUtil.logException(session, t);
                    }

                } else {
                    // We are going into full blown interactive shell mode.
                    session.run();
                }
            }
        }
    }

    /**
     * Allow sub classes of main to change the ConsoleImpl implementation used.
     *
     * @param sessionFactory the session factory.
     * @param in the input stream (console std in).
     * @param out the output stream (console std out).
     * @param err the error stream (console std err).
     * @param terminal the terminal.
     * @return the created session.
     * @throws Exception if something goes wrong during session creation.
     */
    protected Session createSession(SessionFactory sessionFactory, InputStream in, PrintStream out, PrintStream err, Terminal terminal) throws Exception {
        return sessionFactory.create(in, out, err, terminal, null, null);
    }

    protected SessionFactory createSessionFactory(ThreadIO threadio) {
        SessionFactoryImpl sessionFactory = new SessionFactoryImpl(threadio);
        sessionFactory.register(new ManagerImpl(sessionFactory, sessionFactory));
        return sessionFactory;
    }

    /**
     * Sub classes can override so that their registered commands do not conflict with the default shell
     * implementation.
     *
     * @return the location of the discovery resource.
     */
    public String getDiscoveryResource() {
        return "META-INF/services/org/apache/karaf/shell/commands";
    }

    protected void discoverCommands(Session session, ClassLoader cl, String resource) throws IOException, ClassNotFoundException {
        Manager manager = new ManagerImpl(session.getRegistry(), session.getFactory().getRegistry(), true);
        Enumeration<URL> urls = cl.getResources(resource);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = r.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    final Class<?> actionClass = cl.loadClass(line);
                    manager.register(actionClass);
                }
                line = r.readLine();
            }
            r.close();
        }
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * <p>Returns whether or not we are in multi-scope mode.</p>
     *
     * <p>The default mode is multi-scoped where we prefix commands by their scope. If we are in single
     * scoped mode then we don't use scope prefixes when registering or tab completing commands.</p>
     *
     * @return true if the console is multi-scope, false else.
     */
    public boolean isMultiScopeMode() {
        return true;
    }

    private static List<URL> getFiles(File base) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        getFiles(base, urls);
        return urls;
    }

    private static void getFiles(File base, List<URL> urls) throws MalformedURLException {
        for (File f : base.listFiles()) {
            if (f.isDirectory()) {
                getFiles(f, urls);
            } else if (f.getName().endsWith(".jar")) {
                urls.add(f.toURI().toURL());
            }
        }
    }
}
