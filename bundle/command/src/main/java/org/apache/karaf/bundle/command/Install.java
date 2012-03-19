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
package org.apache.karaf.bundle.command;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.MultiException;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

@Command(scope = "bundle", name = "install", description = "Installs one or more bundles.")
public class Install extends OsgiCommandSupport {

    @Argument(index = 0, name = "urls", description = "Bundle URLs separated by whitespaces", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-s", aliases={"--start"}, description="Starts the bundles after installation", required = false, multiValued = false)
    boolean start;

    protected Object doExecute() throws Exception {
        List<Exception> exceptions = new ArrayList<Exception>();
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (String url : urls) {
            try {
                bundles.add(getBundleContext().installBundle(url, null));
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to install bundle " + url, e));
            }
        }
        if (start) {
            for (Bundle bundle : bundles) {
                try {
                    bundle.start();
                } catch (Exception e) {
                    exceptions.add(new Exception("Unable to start bundle " + bundle.getLocation(), e));
                }
            }
        }
        if (bundles.size() == 1) {
            System.out.println("Bundle ID: " + bundles.get(0).getBundleId());
        } else {
            StringBuffer sb = new StringBuffer("Bundle IDs: ");
            for (Bundle bundle : bundles) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(bundle.getBundleId());
            }
            System.out.println(sb);
        }
        MultiException.throwIf("Error installing bundles", exceptions);
        return null;
    }

    private Bundle install(String location, PrintStream out, PrintStream err) {
        try {
            return getBundleContext().installBundle(location, null);
        } catch (IllegalStateException ex) {
            err.println(ex.toString());
        } catch (BundleException ex) {
            if (ex.getNestedException() != null) {
                err.println(ex.getNestedException().toString());
            } else {
                err.println(ex.toString());
            }
        } catch (Exception ex) {
            err.println(ex.toString());
        }
        return null;
    }

}
