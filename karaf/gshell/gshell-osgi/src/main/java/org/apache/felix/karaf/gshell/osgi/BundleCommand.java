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
import org.apache.geronimo.gshell.clp.Option;
import org.apache.felix.karaf.gshell.core.OsgiCommandSupport;
import org.osgi.framework.Bundle;

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

        if (!force && Util.isASystemBundle(getBundleContext(), bundle) && !Util.accessToSystemBundleIsAllowed(bundle.getBundleId(), io)) {
            return Result.FAILURE;
        } else {
            doExecute(bundle);
            return Result.SUCCESS;
        }
    }

    protected abstract void doExecute(Bundle bundle) throws Exception;
}
