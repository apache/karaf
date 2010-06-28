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
package org.apache.felix.karaf.shell.dev.framework;

import java.io.File;
import java.io.IOException;

import org.apache.felix.karaf.shell.dev.util.IO;

/**
 * Represents Eclipse's Equinox as the underlying OSGi framework
 */
public class Equinox extends Framework {

    /**
     * Create a new instance
     *
     * @param base 
     */
    public Equinox(File base) {
        super(base);
    }

    public String getName() {
        return "Equinox";
    }

    public void enableDebug(File directory) throws IOException {
        setConfigProperty("osgi.debug", "etc/equinox-debug.properties");
        saveConfigProperties();

        System.out.printf("- set osgi.debug=etc/equinox-debug.properties in etc/config.properties%n");

        File debug = new File(directory, "etc/equinox-debug.properties");
        if (!debug.exists()) {
            IO.copyTextToFile(
                    Equinox.class.getResourceAsStream("equinox-debug.properties"),
                    debug);
            System.out.printf("- created etc/equinox-debug.properties to configure Equinox debugging options%n");
        }

        System.out.printf("%nEnable specific debug options in etc/equinox-debug.properties%n");
        System.out.printf("and restart Karaf now to enable Equinox debug logging%n");
    }

    @Override
    public void disableDebug(File directory) throws IOException {
        removeConfigProperty("osgi.debug");
        saveConfigProperties();
        
        System.out.printf("- removed osgi.debug from etc/config.properties%n%n");
        System.out.printf("Restart Karaf now to disable Equinox debug logging%n");

    }
}
