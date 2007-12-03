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

import java.util.ArrayList;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.osgi.framework.Bundle;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 3, 2007
 * Time: 12:10:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BundlesCommand extends OsgiCommandSupport {

    @Argument(required = false, multiValued = true, description = "Bundle IDs")
    List<Long> ids;

    protected Object doExecute() throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle == null) {
                    io.err.println("Bundle ID" + id + " is invalid");
                } else {
                    bundles.add(bundle);
                }
            }
        }
        doExecute(bundles);
        return SUCCESS;
    }

    protected abstract void doExecute(List<Bundle> bundles) throws Exception;
}