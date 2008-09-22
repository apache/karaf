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
package org.apache.servicemix.kernel.testing.support;

import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.net.URLStreamHandlerFactory;
import java.net.URLStreamHandler;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.springframework.osgi.test.platform.FelixPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.springframework.util.ClassUtils;
import org.apache.felix.main.Main;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.SecureAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import sun.misc.CompoundEnumeration;

public class SmxKernelPlatform implements OsgiPlatform {

    private static final Log log = LogFactory.getLog(FelixPlatform.class);

    private static final String FELIX_CONF_FILE = "felix.config.properties";

    private static final String FELIX_CONFIG_PROPERTY = "felix.config.properties";

    private static final String FELIX_PROFILE_DIR_PROPERTY = "felix.cache.profiledir";

    private BundleContext context;

    private Object platform;

    private File felixStorageDir;

    private Properties configurationProperties = new Properties();

    protected Properties getPlatformProperties() {
        // load Felix configuration
        Properties props = new Properties();
        props.putAll(getFelixConfiguration());
        props.putAll(getLocalConfiguration());
        return props;
    }

    public Properties getConfigurationProperties() {
        // local properties
        configurationProperties.putAll(getPlatformProperties());
        // system properties
        configurationProperties.putAll(System.getProperties());
        return configurationProperties;
    }

    public BundleContext getBundleContext() {
        return context;
    }

    private Set<String> getJars(Class... classes) {
        Set<String> jars = new HashSet<String>();
        for (Class cl : classes) {
            String name = cl.getName().replace('.', '/') + ".class";
            URL url = (cl.getClassLoader() != null ? cl.getClassLoader() : getClass().getClassLoader()).getResource(name);
            String path = url.toString();
            path = path.substring(0, path.indexOf('!'));
            jars.add(path);
        }
        return jars;
    }

    public void start() throws Exception {
        // Check environment.
        // If the classloader contains JAXP providers, the META-INF infos will certainly be found
        // by OSGi, but classes won't be found, leading to errors.
        ClassLoader cll = Thread.currentThread().getContextClassLoader();
        if (cll == null) {
            cll = getClass().getClassLoader();
        }
        URL url1 = cll.getSystemResource("META-INF/services/javax.xml.transform.TransformerFactory");
        URL url2 = cll.getSystemResource("META-INF/services/javax.xml.parsers.DocumentBuilderFactory");
        List<String> additionalPackages = new ArrayList<String>();
        /*
        if (url1 != null) {
            String line = new BufferedReader(new InputStreamReader(url1.openStream())).readLine();
            line = line.substring(0, line.lastIndexOf('.'));
            System.err.println(line);
            additionalPackages.add(line);
        }
        if (url2 != null) {
            String line = new BufferedReader(new InputStreamReader(url2.openStream())).readLine();
            line = line.substring(0, line.lastIndexOf('.'));
            System.err.println(line);
            additionalPackages.add(line);
        }
        */
        if (url1 != null || url2 != null) {
            String s1 = null;
            if (url1 != null) {
                s1 = url1.toString();
                s1 = s1.substring(s1.lastIndexOf(':') + 1, s1.indexOf('!'));
            }
            String s2 = null;
            if (url2 != null) {
                s2 = url2.toString();
                s2 = s2.substring(s2.lastIndexOf(':') + 1, s2.indexOf('!'));
            }
            throw new Exception("An xml parser or xslt engine has been found in the classpath.\n" +
                    "It is probably included as a transitive dependencies by Maven.\n" +
                    "Check by running 'mvn dependency:tree' and exclude the jars from dependencies.\n" +
                    "Offending resources:\n" +
                    (s1 != null ? "\t" + s1 + "\n" : "") +
                    (s2 != null ? "\t" + s2 + "\n" : ""));
        }

        Set<String> jars = getJars(Felix.class);
        //System.out.println(jars);
        ClassLoader classLoader = new GuardClassLoader(toURLs(jars.toArray(new String[jars.size()])), additionalPackages);

        Thread.currentThread().setContextClassLoader(classLoader);
        Class cl = classLoader.loadClass(Felix.class.getName());
        Constructor cns = cl.getConstructor(Map.class, List.class);
        platform = cns.newInstance(getConfigurationProperties(), null);
        platform.getClass().getMethod("start").invoke(platform);

        Bundle systemBundle = (Bundle) platform;

        // call getBundleContext
        final Method getContext = systemBundle.getClass().getDeclaredMethod("getBundleContext", null);

        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                getContext.setAccessible(true);
                return null;
            }
        });
        context = (BundleContext) getContext.invoke(systemBundle, null);
    }

    public void stop() throws Exception {
        try {
            platform.getClass().getMethod("stop").invoke(platform);
        }
        finally {
            // remove cache folder
            delete(felixStorageDir);
        }
    }

    public String toString() {
        return getClass().getName();
    }

    File createTempDir(String suffix) {
        if (suffix == null)
            suffix = "osgi";
        File tempFileName;

        try {
            tempFileName = File.createTempFile("org.sfw.osgi", suffix);
        }
        catch (IOException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Could not create temporary directory, returning a temp folder inside the current folder", ex);
            }
            return new File("./tmp-test");
        }

        tempFileName.delete(); // we want it to be a directory...
        File tempFolder = new File(tempFileName.getAbsolutePath());
        tempFolder.mkdir();
        return tempFolder;
    }

    /**
     * Configuration settings for the OSGi test run.
     *
     * @return
     */
    private Properties getLocalConfiguration() {
        Properties props = new Properties();
        felixStorageDir = createTempDir("felix");
        props.setProperty(FELIX_PROFILE_DIR_PROPERTY, this.felixStorageDir.getAbsolutePath());
        if (log.isTraceEnabled())
            log.trace("felix storage dir is " + felixStorageDir.getAbsolutePath());

        return props;
    }

    /**
     * Loads Felix config.properties.
     *
     * <strong>Note</strong> the current implementation uses Felix's Main class
     * to resolve placeholders as opposed to loading the properties manually
     * (through JDK's Properties class or Spring's PropertiesFactoryBean).
     *
     * @return
     */
    // TODO: this method should be removed once Felix 1.0.2 is released
    private Properties getFelixConfiguration() {
        String location = "/".concat(ClassUtils.classPackageAsResourcePath(getClass())).concat("/").concat(FELIX_CONF_FILE);
        URL url = getClass().getResource(location);
        if (url == null)
            throw new RuntimeException("cannot find felix configuration properties file:" + location);

        // used with Main
        System.getProperties().setProperty(FELIX_CONFIG_PROPERTY, url.toExternalForm());

        // load config.properties (use Felix's Main for resolving placeholders)
        return Main.loadConfigProperties();
    }

    /**
     * Delete the given file (can be a simple file or a folder).
     *
     * @param file the file to be deleted
     * @return if the deletion succeded or not
     */
    public static boolean delete(File file) {

        // bail out quickly
        if (file == null)
            return false;

        // recursively delete children file
        boolean success = true;

        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                success &= delete(new File(file, children[i]));
            }
        }

        // The directory is now empty so delete it
        return (success &= file.delete());
    }

    private static URL[] toURLs(String[] jars) throws MalformedURLException {
        URL[] urls = new URL[jars.length];
        for (int i = 0; i < urls.length; i++) {
            String s = jars[i];
            if (s.startsWith("jar:")) {
                s = s.substring("jar:".length());
            }
            urls[i] = new URL(s);
        }
        return urls;
    }

    public class GuardClassLoader extends URLClassLoader {
        private Set<String> packages = new HashSet<String>();
        private List<ClassLoader> parents = new ArrayList<ClassLoader>();

        public GuardClassLoader(URL[] urls, List<String> additionalPackages) throws MalformedURLException {
            super(urls, SmxKernelPlatform.class.getClassLoader());
            Properties props = getConfigurationProperties();
            String prop = props.getProperty("org.osgi.framework.system.packages");
            String[] ps = prop.split(",");
            for (String p : ps) {
                String[] spack = p.split(";");
                for (String sp : spack) {
                    sp = sp.trim();
                    if (!sp.startsWith("version")) {
                        packages.add(sp);
                    }
                }
            }
            if (additionalPackages != null) {
                packages.addAll(additionalPackages);
            }
            ClassLoader cl = getParent();
            while (cl != null) {
                parents.add(0, cl);
                cl = cl.getParent();
            }
            //System.err.println("Boot packages: " + packages);
        }

        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            //System.err.println("Loading class: " + name);
            Class c = findLoadedClass(name);
            if (c == null) {
                String pkg = name.substring(0, name.lastIndexOf('.'));
                if (name.startsWith("java.") || packages.contains(pkg)) {
                    for (ClassLoader cl : parents) {
                        try {
                            c = cl.loadClass(name);
                            //System.err.println("Class loaded from: " + cl.getResource(name.replace('.', '/') + ".class"));
                            break;
                        } catch (ClassNotFoundException e) {
                        }
                    }
                    if (c == null) {
                        throw new ClassNotFoundException(name);
                    }
                    //c = getParent().loadClass(name);
                } else  {
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        public URL getResource(String name) {
            //System.err.println("GetResource: " + name);
            URL url = getParent().getResource(name);
            if (url != null && url.toString().startsWith("file:")) {
                return url;
            }
            url = findResource(name);
            System.err.println("Resource " + name + " found at " + url);
            return url;
            /*
            URL u = getParent().getResource(name);
            if (u != null) {
                String path = u.toString();
                int idx = path.indexOf('!');
                if (idx > 0) {
                    path = path.substring(0, idx);
                    if (!jars.contains(path)) {
                        return null;
                    }
                } else {
                    idx = 0;
                }
            }
            return u;
            */
        }

        public Enumeration<URL> getResources(final String name) throws IOException {
            //System.err.println("GetResources: " + name);
            Enumeration[] tmp = new Enumeration[2];
            final Enumeration<URL> e = getParent().getResources(name);
            tmp[0] = new Enumeration<URL>() {
                URL next = null;
                public boolean hasMoreElements() {
                    while (next == null && e.hasMoreElements()) {
                        next = e.nextElement();
                        String path = next.toString();
                        if (!path.startsWith("file:")) {
                            next = null;
                        }
                    }
                    return next != null;
                }
                public URL nextElement() {
                    return next;
                }
            };
            tmp[1] = findResources(name);
            return new CompoundEnumeration(tmp) {
                public Object nextElement() {
                    Object next = super.nextElement();
                    System.err.println("Resources " + name + " found at " + next);
                    return next;
                }
            };
        }
    }
}
