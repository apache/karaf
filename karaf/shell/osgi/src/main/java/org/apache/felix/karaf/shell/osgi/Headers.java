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
package org.apache.felix.karaf.shell.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;

@Command(scope = "osgi", name = "headers", description = "Display OSGi headers of a given bundle")
public class Headers extends OsgiCommandSupport {

    @Argument(required = false, multiValued = true, description = "Bundles ids")
    List<Long> ids;

    protected Object doExecute() throws Exception {
        if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle != null) {
                    printHeaders(bundle);
                }
                else {
                    System.err.println("Bundle ID " + id + " is invalid.");
                }
            }
        }
        else {
            Bundle[] bundles = getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++) {
                printHeaders(bundles[i]);
            }
        }
        return null;
    }

    protected void printHeaders(Bundle bundle) throws Exception {
        String title = Util.getBundleName(bundle);
        System.out.println("\n" + title);
        System.out.println(Util.getUnderlineString(title));
        Dictionary dict = bundle.getHeaders();
        Enumeration keys = dict.keys();
        while (keys.hasMoreElements())
        {
            Object k = (String) keys.nextElement();
            Object v = dict.get(k);
            System.out.println(k + " = " + Util.getValueString(v));
        }
    }

}
