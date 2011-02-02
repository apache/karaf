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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.util.StringEscapeUtils;
import org.osgi.framework.Bundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

@Command(scope = "osgi", name = "info", description = "Displays detailed information of a given bundle.")
public class Info extends OsgiCommandSupport {

    @Argument(index = 0, name = "ids", description = "A list of bundle IDs separated by whitespaces", required = false, multiValued = true)
    List<Long> ids;

    protected Object doExecute() throws Exception {
        if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle != null) {
                    printInfo(bundle);
                } else {
                    System.err.println("Bundle ID " + id + " is invalid.");
                }
            }
        } else {
            Bundle[] bundles = getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                printInfo(bundle);
            }
        }
        return null;
    }

    /**
     * <p>
     * Get the OSGI-INF/bundle.info entry from the bundle and display it.
     * </p>
     *
     * @param bundle the bundle.
     */
    protected void printInfo(Bundle bundle) {
        String title = Util.getBundleName(bundle);
        System.out.println("\n" + title);
        System.out.println(Util.getUnderlineString(title));
        URL bundleInfo = bundle.getEntry("OSGI-INF/bundle.info");
        if (bundleInfo != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(bundleInfo.openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(StringEscapeUtils.unescapeJava(line));
                }
                reader.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

}
