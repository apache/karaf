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
package org.apache.karaf.wrapper.internal;

import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.apache.karaf.wrapper.WrapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Default implementation of the wrapper service.
 */
public class WrapperServiceImpl implements WrapperService {

    private final static Logger LOGGER = LoggerFactory.getLogger(WrapperServiceImpl.class);

    public void install() throws Exception {
        install("karaf", "karaf", "", "AUTO_START");
    }

    public File[] install(String name, 
                          String displayName, 
                          String description, 
                          String startType,
                          String[] envs,
                          String[] includes) throws Exception {

        File base = new File(System.getProperty("karaf.base"));
        File etc = new File(System.getProperty("karaf.etc"));
        File bin = new File(base, "bin");
        File lib = new File(base, "lib/wrapper");

        if (name == null) {
            name = base.getName();
        }

        HashMap<String, String> props = new HashMap<>();
        props.put("${java.home}", System.getenv("JAVA_HOME"));
        props.put("${karaf.home}", System.getProperty("karaf.home"));
        props.put("${karaf.base}", base.getPath());
        props.put("${karaf.data}", System.getProperty("karaf.data"));
        props.put("${karaf.etc}", System.getProperty("karaf.etc"));
        props.put("${karaf.log}", System.getProperty("karaf.log"));
        props.put("${karaf.version}", System.getProperty("karaf.version"));
        props.put("${name}", name);
        props.put("${displayName}", displayName);
        props.put("${description}", description);
        props.put("${startType}", startType);

        String os = System.getProperty("os.name", "Unknown");
        File serviceFile;
        File wrapperConf;
        File systemdFile = null;
        if (os.startsWith("Win")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("amd64") || arch.equalsIgnoreCase("x86_64")) {
                mkdir(bin);

                copyResourceTo(new File(bin, name + "-wrapper.exe"), "windows64/karaf-wrapper.exe", false);

                serviceFile = new File(bin, name + "-service.bat");
                wrapperConf = new File(etc, name + "-wrapper.conf");

                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "windows64/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "windows64/karaf-wrapper-java8.conf", props, envs, includes);
                }

                copyFilteredResourceTo(serviceFile, "windows64/karaf-service.bat", props, envs, includes);

                mkdir(lib);
                copyResourceTo(new File(lib, "wrapper.dll"), "windows64/wrapper.dll", false);
            } else {
                mkdir(bin);

                copyResourceTo(new File(bin, name + "-wrapper.exe"), "windows/karaf-wrapper.exe", false);

                serviceFile = new File(bin, name + "-service.bat");
                wrapperConf = new File(etc, name + "-wrapper.conf");

                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "windows/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "windows/karaf-wrapper-java8.conf", props, envs, includes);
                }

                copyFilteredResourceTo(serviceFile, "windows/karaf-service.bat", props, envs, includes);

                mkdir(lib);
                copyResourceTo(new File(lib, "wrapper.dll"), "windows/wrapper.dll", false);
            }
        } else if (os.startsWith("Mac OS X")) {
            mkdir(bin);

            File file = new File(bin, name + "-wrapper");
            copyResourceTo(file, "macosx/karaf-wrapper", false);
            makeFileExecutable(file);

            serviceFile = new File(bin, name + "-service");
            copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
            makeFileExecutable(serviceFile);

            wrapperConf = new File(etc, name + "-wrapper.conf");

            if (!System.getProperty("java.version").startsWith("1.")) {
                // we are on Java > 8 (Java 9, 10, 11, ...)
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
            } else {
                // we are on Java 8
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
            }

            File plistConf = new File(bin, "org.apache.karaf."+ name + ".plist");
            copyFilteredResourceTo(plistConf, "macosx/org.apache.karaf.KARAF.plist", props, envs, includes);
            
            mkdir(lib);

            copyResourceTo(new File(lib, "libwrapper.jnilib"), "macosx/libwrapper.jnilib", false);
        } else if (os.startsWith("Linux")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("amd64") || arch.equalsIgnoreCase("x86_64")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "linux64/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                systemdFile = new File(bin, name + ".service");
                copyFilteredResourceTo(systemdFile, "unix/karaf.service", props, envs, includes);
                makeFileExecutable(systemdFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "linux64/libwrapper.so", false);
            } else {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "linux/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                systemdFile = new File(bin, name + ".service");
                copyFilteredResourceTo(systemdFile, "unix/karaf.service", props, envs, includes);
                makeFileExecutable(systemdFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "linux/libwrapper.so", false);
            }
        } else if (os.startsWith("AIX")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("ppc64")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "aix/ppc64/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.a"), "aix/ppc64/libwrapper.a", false);
            } else {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "aix/ppc32/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.a"), "aix/ppc32/libwrapper.a", false);
            }
        } else if (os.startsWith("Solaris") || os.startsWith("SunOS")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("sparc")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/sparc64/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/sparc64/libwrapper.so", false);
            } else if (arch.equalsIgnoreCase("x86")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/x86/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/x86/libwrapper.so", false);
            } else if (arch.equalsIgnoreCase("amd64") || arch.equalsIgnoreCase("x86_64")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/x86_64/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/x86_64/libwrapper.so", false);
            } else {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/sparc32/karaf-wrapper", false);
                makeFileExecutable(file);

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
                makeFileExecutable(serviceFile);

                wrapperConf = new File(etc, name + "-wrapper.conf");
                if (!System.getProperty("java.version").startsWith("1.")) {
                    // we are on Java > 8 (Java 9, 10, 11, ...)
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
                } else {
                    // we are on Java 8
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
                }

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/sparc32/libwrapper.so", false);
            }
        } else if (os.startsWith("HP-UX") || os.startsWith("HPUX")) {
            mkdir(bin);

            File file = new File(bin, name + "-wrapper");
            copyResourceTo(file, "hpux/parisc64/karaf-wrapper", false);
            makeFileExecutable(file);

            serviceFile = new File(bin, name + "-service");
            copyFilteredResourceTo(serviceFile, "unix/karaf-service", props, envs, includes);
            makeFileExecutable(serviceFile);

            wrapperConf = new File(etc, name + "-wrapper.conf");
            if (!System.getProperty("java.version").startsWith("1.")) {
                // we are on Java > 8 (Java 9, 10, 11, ...)
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java11.conf", props, envs, includes);
            } else {
                // we are on Java 8
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper-java8.conf", props, envs, includes);
            }

            mkdir(lib);
            copyResourceTo(new File(lib, "libwrapper.sl"), "hpux/parisc64/libwrapper.sl", false);
        } else {
            throw new IllegalStateException("Your operating system '" + os + "' is not currently supported.");
        }

        // install the wrapper jar to the lib directory
        mkdir(lib);
        copyResourceTo(new File(lib, "karaf-wrapper.jar"), "all/karaf-wrapper.jar", false);
        mkdir(etc);

        createJar(new File(lib, "karaf-wrapper-main.jar"), "org/apache/karaf/wrapper/internal/service/Main.class");

        File[] wrapperPaths = new File[3];
        wrapperPaths[0] = wrapperConf;
        wrapperPaths[1] = serviceFile;
        wrapperPaths[2] = systemdFile;

        return wrapperPaths;
    }

    private void mkdir(File file) {
        if (!file.exists()) {
            LOGGER.info("Creating missing directory: {}", file.getPath());
            System.out.println("Creating missing directory: "
                    + SimpleAnsi.INTENSITY_BOLD + file.getPath() + SimpleAnsi.INTENSITY_NORMAL);
            file.mkdirs();
        }
    }

    private void copyResourceTo(File outFile, String resource, boolean text) throws Exception {
        if (!outFile.exists()) {
            LOGGER.info("Creating file: {}", outFile.getPath());
            System.out.println("Creating file: "
                    + SimpleAnsi.INTENSITY_BOLD + outFile.getPath() + SimpleAnsi.INTENSITY_NORMAL);
            InputStream is = WrapperServiceImpl.class.getResourceAsStream(resource);
            if (is == null) {
                throw new IllegalArgumentException("Resource " + resource + " doesn't exist");
            }
            try {
                if (text) {
                    // read it line at a time so what we can use the platform line ending when we write it out
                    PrintStream out = new PrintStream(new FileOutputStream(outFile));
                    try {
                        Scanner scanner = new Scanner(is);
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            LOGGER.info("writing: {}", line);
                            out.println(line);
                        }
                    } finally {
                        safeClose(out);
                    }
                } else {
                    // binary resource so just write it out the way it came in
                    FileOutputStream out = new FileOutputStream(outFile);
                    try {
                        int c;
                        while ((c = is.read()) >= 0) {
                            out.write(c);
                        }
                    } finally {
                        safeClose(out);
                    }
                }
            } finally {
                safeClose(is);
            }
        } else {
            LOGGER.warn("File already exists. Move it out of the way if you wish to recreate it: {}", outFile.getPath());
            System.out.println(
                    SimpleAnsi.COLOR_RED + "File already exists" + SimpleAnsi.COLOR_DEFAULT
                            + ". Move it out of the way if you wish to recreate it: " + outFile.getPath());
        }
    }

    private void copyFilteredResourceTo(File outFile, String resource, HashMap<String, String> props, String[] envs, String[] includes) throws Exception {
        if (!outFile.exists()) {
            LOGGER.info("Creating file: {}", outFile.getPath());
            System.out.println("Creating file: "
                    + SimpleAnsi.INTENSITY_BOLD + outFile.getPath() + SimpleAnsi.INTENSITY_NORMAL);
            InputStream is = WrapperServiceImpl.class.getResourceAsStream(resource);
            if (is == null) {
                throw new IllegalArgumentException("Resource " + resource + " doesn't exist");
            }
            try {
                // read it line at a time so that we can use the platform line ending when we write it out
                PrintStream out = new PrintStream(new FileOutputStream(outFile));
                try {
                    Scanner scanner = new Scanner(is);
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        line = filter(line, props);
                        out.println(line);
                    }
                    if (outFile.getName().endsWith(".conf")) {
                        if (envs != null && envs.length > 0) {
                            for (String env : envs) {
                                out.println(env);
                            }
                        }
                        if (includes != null && includes.length > 0) {
                            for (String include : includes) {
                                out.println("#include " + include);
                            }
                        }
                    }
                } finally {
                    safeClose(out);
                }
            } finally {
                safeClose(is);
            }
        } else {
            LOGGER.warn("File already exists. Move it out of the way if you wish to recreate it: {}", outFile.getPath());
            System.out.println(
                    SimpleAnsi.COLOR_RED + "File already exists" + SimpleAnsi.COLOR_DEFAULT
                            + ". Move it out of the way if you wish to recreate it: " + outFile.getPath());
        }
    }

    private void safeClose(Closeable c) throws IOException {
        if (c == null)
            return;
        try {
            c.close();
        } catch (Throwable ignore) {
            // nothing to do
        }
    }

    private String filter(String line, HashMap<String, String> props) {
        for (Map.Entry<String, String> i : props.entrySet()) {
            int p1 = line.indexOf(i.getKey());
            if (p1 >= 0) {
                String l1 = line.substring(0, p1);
                String l2 = line.substring(p1 + i.getKey().length());
                line = l1 + i.getValue() + l2;
            }
        }
        return line;
    }

    private void makeFileExecutable(File serviceFile) throws IOException {
        Set<PosixFilePermission> permissions = new HashSet<>();
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.GROUP_EXECUTE);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        // Get the existing permissions and add the executable permissions to them
        Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(serviceFile.toPath());
        filePermissions.addAll(permissions);
        Files.setPosixFilePermissions(serviceFile.toPath(), filePermissions);
    }

    private void createJar(File outFile, String resource) throws Exception {
        if (!outFile.exists()) {
            LOGGER.info("Creating file: {}", outFile.getPath());
            System.out.println("Creating file: "
                    + SimpleAnsi.INTENSITY_BOLD + outFile.getPath() + SimpleAnsi.INTENSITY_NORMAL);
            InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                throw new IllegalStateException("Resource " + resource + " not found!");
            }
            JarOutputStream jar = null;
            try {
                jar = new JarOutputStream(new FileOutputStream(outFile));
                int idx = resource.indexOf('/');
                while (idx > 0) {
                    jar.putNextEntry(new ZipEntry(resource.substring(0, idx + 1)));
                    jar.closeEntry();
                    idx = resource.indexOf('/', idx + 1);
                }
                jar.putNextEntry(new ZipEntry(resource));
                int c;
                while ((c = is.read()) >= 0) {
                    jar.write(c);
                }
                jar.closeEntry();
                jar.close();
            } finally {
                safeClose(is);
                safeClose(jar);
            }
        }
    }

    @Override
    public File[] install(String name, String displayName, String description, String startType) throws Exception {
        return install(name, displayName, description, startType, null, null);
    }

}
