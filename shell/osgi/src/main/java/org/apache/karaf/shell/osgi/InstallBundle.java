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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

@Command(scope = "osgi", name = "install", description = "Installs one or more bundles.")
public class InstallBundle extends OsgiCommandSupport {

    @Argument(index = 0, name = "urls", description = "Bundle URLs separated by whitespaces", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-s", aliases={"--start"}, description="Starts the bundles after installation", required = false, multiValued = false)
    boolean start;

    protected Object doExecute() throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        StringBuffer sb = new StringBuffer();
        for (String url : urls) {
            Bundle bundle = install(url, System.out, System.err);
            if (bundle != null) {
                bundles.add(bundle);
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(bundle.getBundleId());
            }
        }
        if (start) {
            for (Bundle bundle : bundles) {
                bundle.start();
            }
        }
        if (sb.toString().indexOf(',') > 0) {
            System.out.println("Bundle IDs: " + sb.toString());
        } else if (sb.length() > 0) {
            System.out.println("Bundle ID: " + sb.toString());
        }
        return null;
    }

    protected Bundle install(String location, PrintStream out, PrintStream err) {
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
