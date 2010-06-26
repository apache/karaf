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

/**
 * Represents Apache Felix as the underlying OSGi platform
 */
public class Felix extends Framework {

    /**
     * Create a instance of Karaf running with Felix in the given base directory
     *
     * @param base the base directory
     */
    public Felix(File base) {
        super(base);
    }

    public String getName() {
        return "Felix";
    }

    public void enableDebug(File directory) throws IOException {
        setConfigProperty("felix.log.level", "4");
        saveConfigProperties();
        
        System.out.printf("- set felix.log.level=4 in etc/config.properties%n%n");
        System.out.printf("Restart Karaf now to enable Felix debug logging%n");
    }

    public void disableDebug(File directory) throws IOException {
        removeConfigProperty("felix.log.level");
        saveConfigProperties();

        System.out.printf("- removed felix.log.level from etc/config.properties%n%n");
        System.out.printf("Restart Karaf now to disable Felix debug logging%n");
    }
}
