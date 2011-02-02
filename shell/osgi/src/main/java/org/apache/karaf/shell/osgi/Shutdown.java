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
package org.apache.karaf.shell.osgi;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;

/**
 * Command to shut down Karaf
 */
@Command(scope = "osgi", name = "shutdown", description = "Shuts the framework down.")
public class Shutdown extends OsgiCommandSupport {

    protected Object doExecute() throws Exception {
        new Thread() {
            public void run() {
                try {
                    Bundle bundle = getBundleContext().getBundle(0);
                    bundle.stop();
                } catch (Exception e) {
                    log.error("Error when shutting down Apache Karaf", e);
                }
            }
        }.start();
        return null;
    }

}
