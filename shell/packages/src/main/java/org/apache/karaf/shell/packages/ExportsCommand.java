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
package org.apache.karaf.shell.packages;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

@Command(scope = "packages", name = "exports", description = "Displays exported packages.")
public class ExportsCommand extends PackageCommandSupport {

    @Option(name = "-i", aliases = {"--imports"}, description = "List bundles importing the specified packages")
    boolean imports;
    @Option(name = "-s", description = "Shows the symbolic name", required = false, multiValued = false)
    boolean showSymbolic;
    @Option(name = "-d", aliases = {"--details"}, description = "List bundles in a master detail table")
    boolean details;
    @Argument(index = 0, name = "ids", description = "The IDs of bundles to check", required = false, multiValued = true)
    List<Long> ids;

    protected void doExecute(PackageAdmin admin) throws Exception {
        String format = "";
        int index = 1;
        List<String> headers = new ArrayList<String>();
        headers.add("ID");
        format += "%" + (index++) + "$6s";

        if (showSymbolic) {
            headers.add("Symbolic name");
            format += " %" + (index++) + "$-40s ";
        }

        headers.add("Packages");
        format += " %" + (index++) + "$-40s";

        if (imports) {
            headers.add("Imported by");
            format += " %" + (index++) + "$-40s";
        }
        format += "\n";
        System.out.format(format, headers.toArray());

        if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle != null) {
                    printExports(System.out, format, bundle, admin.getExportedPackages(bundle));
                } else {
                    System.err.println("Bundle ID " + id + " is invalid.");
                }
            }
        } else {
            for (Bundle bundle : getBundleContext().getBundles()) {
                printExports(System.out, format, bundle, admin.getExportedPackages((Bundle) bundle));
            }
        }
    }

    protected void printExports(PrintStream out, String format, Bundle target, ExportedPackage[] exports) {
        List<String> columns = new ArrayList<String>();

        if ((exports != null) && (exports.length > 0)) {
            for (int i = 0; i < exports.length; i++) {
                columns = new ArrayList<String>();
                Bundle bundle = exports[i].getExportingBundle();
                columns.add(String.valueOf(bundle.getBundleId()));

                if (showSymbolic) {
                    columns.add(bundle.getSymbolicName());
                }

                //Do not repeat ID, Symbolic names etc when bundle exports more that one package
                if (i > 0) {
                    int size = columns.size();
                    if (details) {
                        columns = new ArrayList<String>();
                        fillDetailRecord(columns, size);
                    }
                }

                columns.add(exports[i].getName());

                if (imports) {
                    Bundle[] bs = exports[i].getImportingBundles();
                    if (bs != null && bs.length > 0) {
                        for (int j = 0; j < bs.length; j++) {
                            columns.add(getBundleName(bs[j]));
                            out.format(format, columns.toArray());
                            //Do not repeat ID, Symbolic names etc when package is imported by more than one bundles
                            if (details) {
                                int size = columns.size();
                                columns = new ArrayList<String>();
                                fillDetailRecord(columns, size - 1);
                            } else {
                                columns.remove(columns.size() - 1);
                            }
                        }
                    } else {
                        columns.add("");
                        out.format(format, columns.toArray());
                    }
                } else {
                    out.format(format, columns.toArray());
                }
            }
        } else {
            columns.add(String.valueOf(target.getBundleId()));

            if (showSymbolic) {
                columns.add(target.getSymbolicName());
            }

            columns.add("No active exported packages.");
            if (imports) {
                columns.add("");
            }
            out.format(format, columns.toArray());
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

    /**
     * Method that creates an empty list of string that serves as detail record.
     * @param colums
     * @param size
     */
    public void fillDetailRecord(List<String> colums, int size) {
        for (int i = 0; i < size; i++) {
            colums.add("");
        }
    }
}
