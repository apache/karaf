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
package org.apache.servicemix.kernel.gshell.osgi;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.servicemix.kernel.gshell.core.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public abstract class BundleCommand extends OsgiCommandSupport {

    @Argument(required = true, index = 0)
    long id;

    @Option(name = "--force")
    boolean force;

    protected Object doExecute() throws Exception {
        Bundle bundle = getBundleContext().getBundle(id);
        if (bundle == null) {
            io.out.println("Bundle " + id + " not found");
            return Result.FAILURE;
        }
        if (!force) {
            ServiceReference ref = getBundleContext().getServiceReference(StartLevel.class.getName());
            if (ref != null) {
                StartLevel sl = getService(StartLevel.class, ref);
                if (sl != null) {
                    int level = sl.getBundleStartLevel(bundle);
                    if (level < 50) {
                        for (;;) {
                            StringBuffer sb = new StringBuffer();
                            io.err.print("You are about to access a system bundle.  Do you want to continue (yes/no): ");
                            io.err.flush();
                            for (;;) {
                                int c = io.in.read();
                                if (c < 0) {
                                    return Result.FAILURE;
                                }
                                io.err.print((char) c);
                                if (c == '\r' || c == '\n') {
                                    break;
                                }
                                sb.append((char) c);
                            }
                            String str = sb.toString();
                            if ("yes".equals(str)) {
                                break;
                            }
                            if ("no".equals(str)) {
                                return Result.FAILURE;
                            }
                        }
                    }
                }
            }
        }
        doExecute(bundle);
        return Result.SUCCESS;
    }

    protected abstract void doExecute(Bundle bundle) throws Exception;
}
