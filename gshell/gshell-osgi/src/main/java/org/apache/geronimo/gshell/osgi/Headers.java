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
package org.apache.geronimo.gshell.osgi;

import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 3, 2007
 * Time: 12:10:15 PM
 * To change this template use File | Settings | File Templates.
 */
@CommandComponent(id="osgi:headers", description="Display headers")
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
                    io.err.println("Bundle ID " + id + " is invalid.");
                }
            }
        }
        else {
            Bundle[] bundles = getBundleContext().getBundles();
            for (int i = 0; i < bundles.length; i++) {
                printHeaders(bundles[i]);
            }
        }
        return SUCCESS;
    }

    protected void printHeaders(Bundle bundle) throws Exception {
        String title = Util.getBundleName(bundle);
        io.out.println("\n" + title);
        io.out.println(Util.getUnderlineString(title));
        Dictionary dict = bundle.getHeaders();
        Enumeration keys = dict.keys();
        while (keys.hasMoreElements())
        {
            Object k = (String) keys.nextElement();
            Object v = dict.get(k);
            io.out.println(k + " = " + Util.getValueString(v));
        }
    }

}