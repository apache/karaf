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
package org.apache.felix.karaf.main;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Convenience class for configuring java.util.logging to append to
 * the configured log4j log.  This could be used for bootstrap logging
 * prior to start of the framework.
 * 
 */
public class BootstrapLogManager {
    private static Handler handler;
    private static final String KARAF_BOOTSTRAP_LOG = "karaf.bootstrap.log";

    private static Properties configProps;

    public static Handler getDefaultHandler () {
        String filename;
        File log;
        Properties props = new Properties();
        filename = configProps.getProperty(KARAF_BOOTSTRAP_LOG);

        if (filename != null) {
            log = new File(filename);
        } else {
            // Make a best effort to log to the default file appender configured for log4j
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(System.getProperty("karaf.base") + "/etc/org.ops4j.pax.logging.cfg");
                props.load(fis);
            } catch (IOException e) {
                props.setProperty("log4j.appender.out.file", "${karaf.base}/data/log/karaf.log");
            } finally {
                if (fis != null) { 
                    try {
                        fis.close(); 
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
            filename = Main.substVars(props.getProperty("log4j.appender.out.file"),"log4j.appender.out.file", null, null);
            log = new File(filename);
        }


        try {
            handler = new BootstrapLogManager.SimpleFileHandler(log);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return handler;
    }

    public static void setProperties(Properties configProps) {
        BootstrapLogManager.configProps = configProps;
    }


    /**
     * Implementation of java.util.logging.Handler that does simple appending
     * to a named file.  Should be able to use this for bootstrap logging
     * via java.util.logging prior to startup of pax logging.
     */
    public static class SimpleFileHandler extends StreamHandler {

        public SimpleFileHandler (File file) throws IOException {
            open(file, true);
        }

        private void open (File logfile, boolean append) throws IOException {
            if (!logfile.getParentFile().exists()) {
                try {
                    logfile.getParentFile().mkdirs();
                } catch (SecurityException se) {
                    throw new IOException(se.getMessage());
                }
            }
            FileOutputStream fout = new FileOutputStream(logfile, append);
            BufferedOutputStream out = new BufferedOutputStream(fout);
            setOutputStream(out);
        }

        public synchronized void publish (LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            super.publish(record);
            flush();
        }
    }


}
