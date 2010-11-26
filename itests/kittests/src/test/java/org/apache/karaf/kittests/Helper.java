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
package org.apache.karaf.kittests;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.internal.InstanceImpl;
import org.apache.karaf.shell.commands.utils.StreamPumper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Helper {

    private Helper() {
    }

    public static class Instance {
        public static final String STOPPED = "Stopped";
        public static final String STARTING = "Starting";
        public static final String STARTED = "Started";
        public static final String ERROR = "Error";

        private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";
        private static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";
        private static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";
        private static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";
        private static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";
        private static final String KARAF_SHUTDOWN_PID_FILE = "karaf.shutdown.pid.file";
        private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

        private String location;
        private Process process;

        public Instance(String location, Process process) {
            this.location = location;
            this.process = process;
        }

        public boolean exists() {
            return new File(location).isDirectory();
        }

        public int getSshPort() {
            InputStream is = null;
            try {
                File f = new File(location, "etc/org.apache.karaf.shell.cfg");
                is = new FileInputStream(f);
                Properties props = new Properties();
                props.load(is);
                String loc = props.getProperty("sshPort");
                return Integer.parseInt(loc);
            } catch (Exception e) {
                return 0;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        public synchronized String getState() {
            int port = getSshPort();
            if (!exists() || port <= 0) {
                return ERROR;
            }
            checkProcess();
            if (this.process == null) {
                return STOPPED;
            } else {
                try {
                    Socket s = new Socket("localhost", port);
                    s.close();
                    return STARTED;
                } catch (Exception e) {
                    // ignore
                }
                return STARTING;
            }
        }

        protected void checkProcess() {
            if (this.process != null) {
                try {
                    this.process.exitValue();
                    this.process = null;
                } catch (IllegalThreadStateException e) {
                }
            }
        }

        private static String getProperty(Map<String,String> props, String key, String deflt) {
            String res = props.get(key);
            if (res == null) {
                res = deflt;
            }
            return res;
        }

        public void stop() throws Exception {
            checkProcess();
            if (this.process != null) {
                // Try a clean shutdown
                cleanShutdown();
                if (this.process != null) {
                    this.process.destroy();
                }
            }
        }

        protected void cleanShutdown() {
            try {
                File file = new File(new File(location, "etc"), CONFIG_PROPERTIES_FILE_NAME);
                URL configPropURL = file.toURI().toURL();
                Map<String,String> props = loadPropertiesFile(configPropURL);
                props.put("karaf.base", new File(location).getCanonicalPath());
                props.put("karaf.home", new File(location).getCanonicalPath());
                props.put("karaf.data", new File(new File(location), "data").getCanonicalPath());
                InterpolationHelper.performSubstitution(props);
                int port = Integer.parseInt(getProperty(props, KARAF_SHUTDOWN_PORT, "0"));
                String host = getProperty(props, KARAF_SHUTDOWN_HOST, "localhost");
                String portFile = props.get(KARAF_SHUTDOWN_PORT_FILE);
                String shutdown = getProperty(props, KARAF_SHUTDOWN_COMMAND, DEFAULT_SHUTDOWN_COMMAND);
                if (port == 0 && portFile != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
                    String portStr = r.readLine();
                    port = Integer.parseInt(portStr);
                    r.close();
                }
                // We found the port, try to send the command
                if (port > 0) {
                    Socket s = new Socket(host, port);
                    s.getOutputStream().write(shutdown.getBytes());
                    s.close();
                    long t = System.currentTimeMillis() + 5000;
                    do {
                        Thread.sleep(100);
                        checkProcess();
                    } while (System.currentTimeMillis() < t && process != null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected static Map<String,String> loadPropertiesFile(URL configPropURL) throws Exception {
            // Read the properties file.
            Properties configProps = new Properties();
            InputStream is = null;
            try {
                is = configPropURL.openConnection().getInputStream();
                configProps.load(is);
                is.close();
            }
            catch (Exception ex) {
                System.err.println(
                        "Error loading config properties from " + configPropURL);
                System.err.println("Main: " + ex);
                try {
                    if (is != null) is.close();
                }
                catch (IOException ex2) {
                    // Nothing we can do.
                }
                return null;
            }
            return (Map) configProps;
        }

    }

    protected static final boolean IS_WINDOWS_OS = System.getProperty("os.name").toLowerCase().contains("windows");

    public static void extractKit(File targetDir) throws Exception {
        if (isWindowsOs()) {
            extractWindowsKit(targetDir);
        } else {
            extractUnixKit(targetDir);
        }
    }

    public static boolean isWindowsOs() {
        return IS_WINDOWS_OS;
    }

    protected static void extractWindowsKit(File targetDir) throws Exception {
        InputStream is = Helper.class.getResourceAsStream("/karaf.zip");
        extract(new ZipArchiveInputStream(is), targetDir);
    }

    protected static void extractUnixKit(File targetDir) throws Exception {
        InputStream is = Helper.class.getResourceAsStream("/karaf.tar.gz");
        extract(new TarArchiveInputStream(new GzipCompressorInputStream(is)), targetDir);
        File bin = new File(targetDir, "bin");
        String[] files = bin.list();
        List<String> args = new ArrayList();
        Collections.addAll(args, "chmod", "+x");
        Collections.addAll(args, files);
        Process chmod = new ProcessBuilder()
            .directory(new File(targetDir, "bin"))
            .command(args)
            .start();
        PumpStreamHandler pump = new PumpStreamHandler(System.in, System.out, System.err);
        pump.attach(chmod);
        pump.start();
        waitForProcessEnd(chmod, 5000);
    }

    protected static void extract(ArchiveInputStream is, File targetDir) throws IOException {
        try {
            if (targetDir.exists()) {
                FileUtils.forceDelete(targetDir);
            }
            targetDir.mkdirs();
            ArchiveEntry entry = is.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                name = name.substring(name.indexOf("/") + 1);
                File file = new File(targetDir, name);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(file);
                    try {
                        IOUtils.copy(is, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                    }
                }
                entry = is.getNextEntry();
            }
        } finally {
            is.close();
        }
    }

    public static Process launchScript(File homeDir, String script, String args) throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(homeDir);
        if (isWindowsOs()) {
            builder.command("cmd.exe", "/c", new File(homeDir, "bin\\" + script + ".bat").getAbsolutePath(), args);
        } else {
            builder.command("/bin/sh", new File(homeDir, "bin/" + script).getAbsolutePath(), args);
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        PumpStreamHandler pump = new PumpStreamHandler(System.in, System.out, System.err);
        pump.attach(process);
        pump.start();
        return process;
    }

    public static Instance startKaraf(File home) throws Exception {
        Process karaf = launchScript(home, "karaf", "server");
        return new Instance(home.getAbsolutePath(), karaf);
//        InstanceImpl instance = new InstanceImpl(null, "root", home.getAbsolutePath(), null, true);
//        instance.attach(karaf.getPid());
//        return instance;
    }

    public static void waitForKarafStarted(Instance karaf, long timeout) throws Exception {
        for (int i = 0; i < timeout / 100; i++) {
            if (Instance.STARTING.equals(karaf.getState())) {
                Thread.sleep(100);
            } else {
                break;
            }
        }
        if (!Instance.STARTED.equals(karaf.getState())) {
            throw new Exception("Karaf did not start correctly");
        }
    }

    public static void waitForKarafStopped(Instance karaf, long timeout) throws Exception {
        waitForProcessEnd(karaf.process, timeout);
    }

    public static void waitForProcessEnd(Process process, long timeout) throws Exception {
        for (int i = 0; i < timeout / 100; i++) {
            try {
                process.exitValue();
                return;
            } catch (IllegalThreadStateException e) {
            }
            Thread.sleep(100);
        }
        throw new Exception("Process is still running");
    }

    public static void kill(Process process) {
        try {
            process.destroy();
        } catch (Throwable e) {
        }
    }

    public static void kill(Instance instance) {
        try {
            instance.stop();
        } catch (Throwable e) {
        }
    }

    public static class PumpStreamHandler
    {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private Thread outputThread;

        private Thread errorThread;

        private StreamPumper inputPump;

        //
        // NOTE: May want to use a ThreadPool here, 3 threads per/pair seems kinda expensive :-(
        //

        public PumpStreamHandler(final InputStream in, final OutputStream out, final OutputStream err) {
            assert in != null;
            assert out != null;
            assert err != null;

            this.in = in;
            this.out = out;
            this.err = err;
        }

        public PumpStreamHandler(final OutputStream out, final OutputStream err) {
            this(null, out, err);
        }

        public PumpStreamHandler(final OutputStream outAndErr) {
            this(outAndErr, outAndErr);
        }

        /**
         * Set the input stream from which to read the standard output of the child.
         */
        public void setChildOutputStream(final InputStream in) {
            assert in != null;

            createChildOutputPump(in, out);
        }

        /**
         * Set the input stream from which to read the standard error of the child.
         */
        public void setChildErrorStream(final InputStream in) {
            assert in != null;

            if (err != null) {
                createChildErrorPump(in, err);
            }
        }

        /**
         * Set the output stream by means of which input can be sent to the child.
         */
        public void setChildInputStream(final OutputStream out) {
            assert out != null;

            if (in != null) {
                inputPump = createInputPump(in, out, true);
            }
            else {
                try {
                    out.close();
                } catch (IOException e) { }
            }
        }

        /**
         * Attach to a child streams from the given process.
         *
         * @param p     The process to attach to.
         */
        public void attach(final Process p) {
            assert p != null;

            setChildInputStream(p.getOutputStream());
            setChildOutputStream(p.getInputStream());
            setChildErrorStream(p.getErrorStream());
        }
        /**
         * Start pumping the streams.
         */
        public void start() {
            if (outputThread != null) {
                outputThread.start();
            }

            if (errorThread != null) {
                errorThread.start();
            }

            if (inputPump != null) {
                Thread inputThread = new Thread(inputPump);
                inputThread.setDaemon(true);
                inputThread.start();
            }
        }

        /**
         * Stop pumping the streams.
         */
        public void stop() {
            if (outputThread != null) {
                try {
                    outputThread.join();
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }

            if (errorThread != null) {
                try {
                    errorThread.join();
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }

            if (inputPump != null) {
                inputPump.stop();
            }

            try {
                err.flush();
            } catch (IOException e) { }
            try {
                out.flush();
            } catch (IOException e) { }
        }

        /**
         * Create the pump to handle child output.
         */
        protected void createChildOutputPump(final InputStream in, final OutputStream out) {
            assert in != null;
            assert out != null;

            outputThread = createPump(in, out);
        }

        /**
         * Create the pump to handle error output.
         */
        protected void createChildErrorPump(final InputStream in, final OutputStream out) {
            assert in != null;
            assert out != null;

            errorThread = createPump(in, out);
        }

        /**
         * Creates a stream pumper to copy the given input stream to the given output stream.
         */
        protected Thread createPump(final InputStream in, final OutputStream out) {
            assert in != null;
            assert out != null;

            return createPump(in, out, false);
        }

        /**
         * Creates a stream pumper to copy the given input stream to the
         * given output stream.
         *
         * @param in                    The input stream to copy from.
         * @param out                   The output stream to copy to.
         * @param closeWhenExhausted    If true close the inputstream.
         * @return                      A thread object that does the pumping.
         */
        protected Thread createPump(final InputStream in, final OutputStream out, final boolean closeWhenExhausted) {
            assert in != null;
            assert out != null;

            final Thread result = new Thread(new StreamPumper(in, out, closeWhenExhausted));
            result.setDaemon(true);
            return result;
        }

        /**
         * Creates a stream pumper to copy the given input stream to the
         * given output stream. Used for standard input.
         */
        protected StreamPumper createInputPump(final InputStream in, final OutputStream out, final boolean closeWhenExhausted) {
            assert in != null;
            assert out != null;

            StreamPumper pumper = new StreamPumper(in, out, closeWhenExhausted);
            pumper.setAutoflush(true);
            return pumper;
        }
    }
}