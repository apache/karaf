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
package org.apache.servicemix.kernel.gshell.packages;

import java.io.PrintWriter;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

public class ExportsCommand extends PackageCommandSupport {

    @Option(name = "-i", aliases = { "--imports"}, description = "List bundles importing the packages")
    boolean imports;

    @Argument(required = false, multiValued = true, description = "bundle ids")
    List<Long> ids;

    protected void doExecute(PackageAdmin admin) throws Exception {
        if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle != null) {
                    printExports(io.out, bundle, admin.getExportedPackages(bundle));
                } else {
                    io.err.println("Bundle ID " + id + " is invalid.");
                }
            }
        }
        else {
            printExports(io.out, null, admin.getExportedPackages((Bundle) null));
        }
    }

    protected void printExports(PrintWriter out, Bundle target, ExportedPackage[] exports) {
        if ((exports != null) && (exports.length > 0)) {
            for (int i = 0; i < exports.length; i++) {
                Bundle bundle = exports[i].getExportingBundle();
                out.print(getBundleName(bundle));
                out.println(": " + exports[i]);
                if (imports) {
                    Bundle[] bs = exports[i].getImportingBundles();
                    if (bs != null) {
                        for (Bundle b : bs) {
                            out.println("\t" + getBundleName(b));
                        }
                    }
                }
            }
        } else {
            out.println(getBundleName(target) + ": No active exported packages.");
        }
    }

    public static String getBundleName(Bundle bundle) {
        if (bundle != null) {
            String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
            return (name == null)
                ? "Bundle " + Long.toString(bundle.getBundleId())
                : name + " (" + Long.toString(bundle.getBundleId()) + ")";
        }
        return "[STALE BUNDLE]";
    }

}
