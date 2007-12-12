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
package org.apache.servicemix.gshell.features.internal;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Properties;

import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.obr.ObrCommandSupport;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.apache.servicemix.gshell.features.Feature;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.obr.RepositoryAdmin;

/**
 * Simple command which prompts the user and installs the needed bundles.
 * The bundles need to be available through the OBR repositories.
 */
public class CommandProxy extends ObrCommandSupport {

    private Feature feature;
    private ServiceRegistration registration;

    public CommandProxy(Feature feature, BundleContext bundleContext) {
        this.feature = feature;
        setBundleContext(bundleContext);
        Properties props = new Properties();
        props.put("shell", feature.getName());
        props.put("rank", "-1");
        registration = bundleContext.registerService(Command.class.getName(), this, props);
    }

    protected OsgiCommandSupport createCommand() {
        return this;
    }

    @Deprecated
    public String getId() {
        return feature.getName();
    }

    @Deprecated
    public String getDescription() {
        return feature.getName();
    }

    protected void doExecute(RepositoryAdmin admin) throws Exception {
        io.out.println("This feature is not yet installed.  Do you want to install it (y/n) ? ");
        int c = io.in.read();
        if (c == 'y' || c == 'Y') {
            io.out.println("Installing feature.  Please wait...");
            registration.unregister();
            doDeploy(admin, Arrays.asList(feature.getBundles()), true);
        }
    }

    private String readLine(Reader in) throws IOException {
        StringBuffer buf = new StringBuffer();
        while (true) {
            int i = in.read();
            if ((i == -1) || (i == '\n') || (i == '\r')) {
                return buf.toString();
            }
            buf.append((char) i);
        }
    }
}
