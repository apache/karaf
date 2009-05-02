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
package org.apache.felix.karaf.gshell.osgi;

import org.apache.geronimo.gshell.clp.Argument;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public class BundleLevel extends BundleCommand {

    @Argument(required = false, index = 1)
    Integer level;

    protected void doExecute(Bundle bundle) throws Exception {
        // Get package admin service.
        ServiceReference ref = getBundleContext().getServiceReference(StartLevel.class.getName());
        if (ref == null) {
            io.out.println("StartLevel service is unavailable.");
            return;
        }
        StartLevel sl = getService(StartLevel.class, ref);
        if (sl == null) {
            io.out.println("StartLevel service is unavailable.");
            return;
        }

        if (level == null) {
            io.out.println("Level " + sl.getBundleStartLevel(bundle));
        }
        else if ((level < 50) && sl.getBundleStartLevel(bundle) > 50){
            for (;;) {
                StringBuffer sb = new StringBuffer();
                io.err.println("You are about to designate bundle as a system bundle.  Do you want to continue (yes/no): ");
                io.err.flush();
                for (;;) {
                    int c = io.in.read();
                    if (c < 0) {
                        return;
                    }
                    io.err.println((char) c);
                    if (c == '\r' || c == '\n') {
                        break;
                    }
                    sb.append((char) c);
                }
                String str = sb.toString();
                if ("yes".equals(str)) {
                    sl.setBundleStartLevel(bundle, level);
                    break;
                } else if ("no".equals(str)) {
                    break;
                }
            }
        } else {
            sl.setBundleStartLevel(bundle, level);
        }
    }

}
