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
package org.apache.karaf.shell.wrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.karaf.shell.console.AbstractAction;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.jansi.Ansi;

/**
 * Installs this Karaf instance as a service in your operating systems.
 *
 * @version $Rev: 603634 $ $Date: 2007-12-12 16:07:16 +0100 (Wed, 12 Dec 2007) $
 */
@Command(scope = "wrapper", name = "install", description = "Install the container as a system service in the OS.")
public class InstallCommand extends AbstractAction {
    @Option(name = "-n", aliases = {"--name"}, description = "The service name that will be used when installing the service. (Default: karaf)", required = false, multiValued = false)
    private String name = "karaf";

    @Option(name = "-d", aliases = {"--display"}, description = "The display name of the service.", required = false, multiValued = false)
    private String displayName;

    @Option(name = "-D", aliases = {"--description"}, description = "The description of the service.", required = false, multiValued = false)
    private String description = "";

    @Option(name = "-s", aliases = {"--start-type"}, description = "Mode in which the service is installed. AUTO_START or DEMAND_START (Default: AUTO_START)", required = false, multiValued = false)
    private String startType = "AUTO_START";

    protected Object doExecute() throws Exception {

        try {
            String name = getName();
            File base = new File(System.getProperty("karaf.base"));
            File bin = new File(base, "bin");
            File etc = new File(base, "etc");
            File lib = new File(base, "lib");

            HashMap<String, String> props = new HashMap<String, String>();
            props.put("${karaf.home}", System.getProperty("karaf.home"));
            props.put("${karaf.base}", base.getPath());
            props.put("${karaf.data}", System.getProperty("karaf.data"));
            props.put("${name}", name);
            props.put("${displayName}", getDisplayName());
            props.put("${description}", getDescription());
            props.put("${startType}", getStartType());

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

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.jnilib"), "macosx/libwrapper.jnilib", false);

                // TODO: figure out how to hook in the service that it starts up
                // when the machine boots up.
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

                    // TODO: figure out how to hook in the service that it starts up
                    // when the machine boots up.
                }
            } else if (os.startsWith("AIX")) {
                String arch = System.getProperty("os.arch");
                if (arch.equalsIgnoreCase("ppc64")) {
                    mkdir(bin);

                    File file = new File(bin, name + "-wrapper");
                    copyResourceTo(file, "aix/ppc64/karaf-wrapper", false);
                    chmod(file, "a+x");

                    serviceFile = new File(bin, name + "-service");
                    copyResourceTo(file, "unix/karaf-service", false);
                    chmod(file, "a+x");

                    wrapperConf = new File(etc, name + "-wrapper.conf");
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                    mkdir(lib);
                    copyResourceTo(new File(lib, "libwrapper.a"), "aix/ppc64/libwrapper.a", false);
                } else {
                    mkdir(bin);

                    File file = new File(bin, name + "-wrapper");
                    copyResourceTo(file, "aix/ppc64/karaf-wrapper", false);
                    chmod(file, "a+x");

                    serviceFile = new File(bin, name + "-service");
                    copyResourceTo(file, "unix/karaf-service", false);
                    chmod(file, "a+x");

                    wrapperConf = new File(etc, name + "-wrapper.conf");
                    copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                    mkdir(lib);
                    copyResourceTo(new File(lib, "libwrapper.a"), "aix/ppc64/libwrapper.a", false);
                }
            } else if (os.startsWith("Solaris") || os.startsWith("SunOS")) {
                String arch = System.getProperty("os.arch");
                if (arch.equalsIgnoreCase("sparc")) {
                    mkdir(bin);

                    File file = new File(bin, name + "-wrapper");
                    copyResourceTo(file, "solaris/sparc64/karaf-wrapper", false);
                    chmod(file, "a+x");

                    serviceFile = new File(bin, name + "-service");
                    copyResourceTo(file, "unix/karaf-service", false);
                    chmod(file, "a+x");

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
                    copyResourceTo(file, "unix/karaf-service", false);
                    chmod(file, "a+x");

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
                    copyResourceTo(file, "unix/karaf-service", false);
                    chmod(file, "a+x");

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
                copyResourceTo(file, "unix/karaf-service", false);
                chmod(file, "a+x");

                wrapperConf = new File(etc, name + "-wrapper.conf");
                copyFilteredResourceTo(wrapperConf, "unix/karaf-wrapper.conf", props);

                mkdir(lib);
                copyResourceTo(new File(lib, "libwrapper.sl"), "hpux/parisc64/libwrapper.sl", false);
            } else {
                System.out.println("Your operating system '" + os + "' is not currently supported.");
                return 1;
            }

            // Install the wrapper jar to the lib directory..
            mkdir(lib);
            copyResourceTo(new File(lib, "karaf-wrapper.jar"), "all/karaf-wrapper.jar", false);
            mkdir(etc);

            createJar(new File(lib, "karaf-wrapper-main.jar"), "org/apache/karaf/shell/wrapper/Main.class");

            System.out.println("");
            System.out.println("Setup complete.  You may wish to tweak the JVM properties in the wrapper configuration file:");
            System.out.println("\t" + wrapperConf.getPath());
            System.out.println("before installing and starting the service.");
            System.out.println("");
            if (os.startsWith("Win")) {
                System.out.println("");
                System.out.println("To install the service, run: ");
                System.out.println("  C:> " + serviceFile.getPath() + " install");
                System.out.println("");
                System.out.println("Once installed, to start the service run: ");
                System.out.println("  C:> net start \"" + name + "\"");
                System.out.println("");
                System.out.println("Once running, to stop the service run: ");
                System.out.println("  C:> net stop \"" + name + "\"");
                System.out.println("");
                System.out.println("Once stopped, to remove the installed the service run: ");
                System.out.println("  C:> " + serviceFile.getPath() + " remove");
                System.out.println("");
            } else if (os.startsWith("Mac OS X")) {
                System.out.println("");
                System.out.println("At this time it is not known how to get this service to start when the machine is rebooted.");
                System.out.println("If you know how to install the following service script so that it gets started");
                System.out.println("when OS X starts, please email dev@felix.apache.org and let us know how so");
                System.out.println("we can update this message.");
                System.out.println(" ");
                System.out.println("  To start the service:");
                System.out.println("    $ " + serviceFile.getPath() + " start");
                System.out.println("");
                System.out.println("  To stop the service:");
                System.out.println("    $ " + serviceFile.getPath() + " stop");
                System.out.println("");
            } else if (os.startsWith("Linux")) {
                System.out.println("The way the service is installed depends upon your flavor of Linux.");

                // TODO: figure out if we can detect the Linux flavor

                System.out.println("");
                System.out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("On Redhat/Fedora/CentOS Systems:").a(Ansi.Attribute.RESET).toString());
                System.out.println("  To install the service:");
                System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " --add");
                System.out.println("");
                System.out.println("  To start the service when the machine is rebooted:");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " on");
                System.out.println("");
                System.out.println("  To disable starting the service when the machine is rebooted:");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " off");
                System.out.println("");
                System.out.println("  To start the service:");
                System.out.println("    $ service " + serviceFile.getName() + " start");
                System.out.println("");
                System.out.println("  To stop the service:");
                System.out.println("    $ service " + serviceFile.getName() + " stop");
                System.out.println("");
                System.out.println("  To uninstall the service :");
                System.out.println("    $ chkconfig " + serviceFile.getName() + " --del");
                System.out.println("    $ rm /etc/init.d/" + serviceFile.getName());

                System.out.println("");
                System.out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("On Ubuntu/Debian Systems:").a(Ansi.Attribute.RESET).toString());
                System.out.println("  To install the service:");
                System.out.println("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
                System.out.println("");
                System.out.println("  To start the service when the machine is rebooted:");
                System.out.println("    $ update-rc.d " + serviceFile.getName() + " defaults");
                System.out.println("");
                System.out.println("  To disable starting the service when the machine is rebooted:");
                System.out.println("    $ update-rc.d -f " + serviceFile.getName() + " remove");
                System.out.println("");
                System.out.println("  To start the service:");
                System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " start");
                System.out.println("");
                System.out.println("  To stop the service:");
                System.out.println("    $ /etc/init.d/" + serviceFile.getName() + " stop");
                System.out.println("");
                System.out.println("  To uninstall the service :");
                System.out.println("    $ rm /etc/init.d/" + serviceFile.getName());

            }


        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return null;
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

    private void copyResourceTo(File outFile, String resource, boolean text) throws Exception {
        if (!outFile.exists()) {
            System.out.println(Ansi.ansi().a("Creating file: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = InstallCommand.class.getResourceAsStream(resource);
            try {
                if (text) {
                    // Read it line at a time so that we can use the platform line ending when we write it out.
                    PrintStream out = new PrintStream(new FileOutputStream(outFile));
                    try {
                        Scanner scanner = new Scanner(is);
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            System.out.println("writing: " + line);
                            out.println(line);
                        }
                    } finally {
                        safeClose(out);
                    }
                } else {
                    // Binary so just write it out the way it came in.
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
            System.out.println(Ansi.ansi()
                    .fg(Ansi.Color.RED).a("File already exists").a(Ansi.Attribute.RESET)
                    .a(". Move it out of the way if you wish to recreate it: ").a(outFile.getPath()).toString());
        }
    }

    private void copyFilteredResourceTo(File outFile, String resource, HashMap<String, String> props) throws Exception {
        if (!outFile.exists()) {
            System.out.println(Ansi.ansi().a("Creating file: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(outFile.getPath()).a(Ansi.Attribute.RESET).toString());
            InputStream is = InstallCommand.class.getResourceAsStream(resource);
            try {
                // Read it line at a time so that we can use the platform line ending when we write it out.
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
        }
    }

    private void safeClose(OutputStream is) throws IOException {
        if (is == null)
            return;
        try {
            is.close();
        } catch (Throwable ignore) {
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

    private void mkdir(File file) {
        if (!file.exists()) {
            System.out.println(Ansi.ansi().a("Creating missing directory: ")
                    .a(Ansi.Attribute.INTENSITY_BOLD).a(file.getPath()).a(Ansi.Attribute.RESET).toString());
            file.mkdirs();
        }
    }

    public String getName() {
        if (name == null) {
            File base = new File(System.getProperty("karaf.base"));
            name = base.getName();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        if (displayName == null) {
            displayName = getName();
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartType() {
        return startType;
    }

    public void setStartType(String startType) {
        this.startType = startType;
    }
}
