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

import org.apache.karaf.wrapper.WrapperService;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
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

    public File[] install(String name, String displayName, String description, String startType) throws Exception {

        File base = new File(System.getProperty("karaf.base"));
        File etc = new File(System.getProperty("karaf.etc"));
        File bin = new File(base, "bin");
        File lib = new File(base, "lib");

        if (name == null) {
            name = base.getName();
        }

        HashMap<String, String> props = new HashMap<String, String>();
        props.put("${java.home}", System.getenv("JAVA_HOME"));
        props.put("${karaf.home}", System.getProperty("karaf.home"));
        props.put("${karaf.base}", base.getPath());
        props.put("${karaf.data}", System.getProperty("karaf.data"));
        props.put("${karaf.etc}", System.getProperty("karaf.etc"));
        props.put("${name}", name);
        props.put("${displayName}", displayName);
        props.put("${description}", description);
        props.put("${startType}", startType);

        String os = System.getProperty("os.name", "Unknown");
        File serviceFile = null;
        File wrapperConf = null;
        if (os.startsWith("Win")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("amd64") || arch.equalsIgnoreCase("x86_64")) {
                mkdir(bin);

                copyResourceTo(new File(bin, name + "-wrapper.exe"), "windows64/karaf-wrapper.exe", false);

                serviceFile = new File(bin, name + "-service.bat");
                wrapperConf = new File(etc, name + "-wrapper.conf");

                copyFilteredResourceTo(wrapperConf, "windows64/karaf-wrapper.conf", props);
                copyFilteredResourceTo(serviceFile, "windows64/karaf-service.bat", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "wrapper.dll"), "windows64/wrapper.dll", false);
            } else {
                mkdir(bin);

                copyResourceTo(new File(bin, name + "-wrapper.exe"), "windows/karaf-wrapper.exe", false);

                serviceFile = new File(bin, name + "-service.bat");
                wrapperConf = new File(etc, name + "-wrapper.conf");

                copyFilteredResourceTo(wrapperConf, "windows/karaf-wrapper.conf", props);
                copyFilteredResourceTo(serviceFile, "windows/karaf-service.bat", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "wrapper.dll"), "windows/wrapper.dll", false);
            }
        } else if (os.startsWith("Mac OS X")) {
            mkdir(bin);

            File file = new File(bin, name + "-wrapper");
            copyResourceTo(file, "macosx/karaf-wrapper", false);
            chmod(file, "a+x");

            serviceFile = new File(bin, name + "-service");
            copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
            chmod(serviceFile, "a+x");

            wrapperConf = new File(etc, name + "-wrapper.conf");
            copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

            File plistConf = new File(bin, "org.apache.karaf."+ name + ".plist");
            copyFilteredResourceTo(plistConf, "macosx/org.apache.karaf.KARAF.plist", props);
            
            mkdir(lib);

            copyResourceTo(new File(lib, "libwrapper.jnilib"), "macosx/libwrapper.jnilib", false);
        } else if (os.startsWith("Linux")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("amd64") || arch.equalsIgnoreCase("x86_64")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "linux64/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "linux64/libwrapper.so", false);
            } else {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "linux/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "linux/libwrapper.so", false);
            }
        } else if (os.startsWith("AIX")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("ppc64")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "aix/ppc64/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.a"), "aix/ppc64/libwrapper.a", false);
            } else {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "aix/ppc32/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.a"), "aix/ppc32/libwrapper.a", false);
            }
        } else if (os.startsWith("Solaris") || os.startsWith("SunOS")) {
            String arch = System.getProperty("os.arch");
            if (arch.equalsIgnoreCase("sparc")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/sparc64/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/sparc64/libwrapper.so", false);
            } else if (arch.equalsIgnoreCase("x86")) {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/x86/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/x86/libwrapper.so", false);
            } else {
                mkdir(bin);

                File file = new File(bin, name + "-wrapper");
                copyResourceTo(file, "solaris/sparc32/karaf-wrapper", false);
                chmod(file, "a+x");

                serviceFile = new File(bin, name + "-service");
                copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
                chmod(serviceFile, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.so"), "solaris/sparc32/libwrapper.so", false);
            }
        } else if (os.startsWith("HP-UX") || os.startsWith("HPUX")) {
            mkdir(bin);

            File file = new File(bin, name + "-wrapper");
            copyResourceTo(file, "hpux/parisc64/karaf-wrapper", false);
            chmod(file, "a+x");

            serviceFile = new File(bin, name + "-service");
            copyFilteredResourceTo(serviceFile, "unix/karaf-service", props);
            chmod(serviceFile, "a+x");

            wrapperConf = new File(etc, name + "-wrapper.conf");
            copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

            mkdir(lib);
            copyResourceTo(new File(lib, "libwrapper.sl"), "hpux/parisc64/libwrapper.sl", false);
        } else {
            throw new IllegalStateException("Your operating system '" + os + "' is not currently supported.");
        }

        // install the wrapper jar to the lib directory
        mkdir(lib);
        copyResourceTo(new File(lib, "karaf-wrapper.jar"), "all/karaf-wrapper.jar", false);
        mkdir(etc);

        createJar(new File(lib, "karaf-wrapper-main.jar"), "org/apache/karaf/wrapper/internal/Main.class");

        File[] wrapperPaths = new File[2];
        wrapperPaths[0] = wrapperConf;
        wrapperPaths[1] = serviceFile;

        return wrapperPaths;
    }

    private void mkdir(File file) {
        if (!file.exists()) {
            LOGGER.info("Creating missing directory: {}", file.getPath());
            System.out.println(Ansi.ansi().a("Creating missing directory: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(file.getPath()).a(Ansi.Attribute.RESET).toString());
            file.mkdirs();
        }
    }

    private void copyResourceTo(File outFile, String resource, boolean text) throws Exception {
        if (!outFile.exists()) {
            LOGGER.info("Creating file: {}", outFile.getPath());
            System.out.println(Ansi.ansi().a("Creating file: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
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
                        int c = 0;
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
            System.out.println(Ansi.ansi()
                    .fg(Ansi.Color.RED).a("File already exists").a(Ansi.Attribute.RESET)
                    .a(". Move it out of the way if you wish to recreate it: ").a(outFile.getPath()).toString());
        }
    }

    private void copyFilteredResourceTo(File outFile, String resource, HashMap<String, String> props) throws Exception {
        if (!outFile.exists()) {
            LOGGER.info("Creating file: {}", outFile.getPath());
            System.out.println(Ansi.ansi().a("Creating file: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
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
                } finally {
                    safeClose(out);
                }
            } finally {
                safeClose(is);
            }
        } else {
            LOGGER.warn("File already exists. Move it out of the way if you wish to recreate it: {}", outFile.getPath());
            System.out.println(Ansi.ansi()
                    .fg(Ansi.Color.RED).a("File already exists").a(Ansi.Attribute.RESET)
                    .a(". Move it out of the way if you wish to recreate it: ").a(outFile.getPath()).toString());
        }
    }

    private void safeClose(InputStream is) throws IOException {
        if (is == null)
            return;
        try {
            is.close();
        } catch (Throwable ignore) {
            // nothing to do
        }
    }

    private void safeClose(OutputStream is) throws IOException {
        if (is == null)
            return;
        try {
            is.close();
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

    private int chmod(File serviceFile, String mode) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("chmod", mode, serviceFile.getCanonicalPath());
        Process p = builder.start();

        PumpStreamHandler handler = new PumpStreamHandler(System.in, System.out, System.err);
        handler.attach(p);
        handler.start();
        int status = p.waitFor();
        handler.stop();
        return status;
    }

    private void createJar(File outFile, String resource) throws Exception {
        if (!outFile.exists()) {
            LOGGER.info("Creating file: {}", outFile.getPath());
            System.out.println(Ansi.ansi().a("Creating file: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                throw new IllegalStateException("Resource " + resource + " not found!");
            }
            try {
                JarOutputStream jar = new JarOutputStream(new FileOutputStream(outFile));
                int idx = resource.indexOf('/');
                while (idx > 0) {
                    jar.putNextEntry(new ZipEntry(resource.substring(0, idx)));
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
            }
        }
    }

}
