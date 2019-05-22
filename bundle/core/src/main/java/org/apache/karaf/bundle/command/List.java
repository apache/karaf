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
package org.apache.karaf.bundle.command;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.FrameworkStartLevel;

import java.util.ArrayList;

@Command(scope = "bundle", name = "list", description = "Lists all installed bundles.")
@Service
public class List extends BundlesCommand {

    @Option(name = "-name", aliases="-n",  description = "Show bundle name", required = false, multiValued = false)
    boolean showName;

    @Option(name = "-l", aliases = {}, description = "Show the locations", required = false, multiValued = false)
    boolean showLocation;

    @Option(name = "-s", description = "Shows the symbolic name", required = false, multiValued = false)
    boolean showSymbolic;

    @Option(name = "-u", description = "Shows the update locations", required = false, multiValued = false)
    boolean showUpdate;
    
    @Option(name = "-r", description = "Shows the bundle revisions", required = false, multiValued = false)
    boolean showRevisions;

    @Option(name = "-t", valueToShowInHelp = "", description = "Specifies the bundle threshold; bundles with a start-level less than this value will not get printed out.", required = false, multiValued = false)
    int bundleLevelThreshold = -1;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Option(name = "--no-ellipsis")
    boolean noEllipsis;

    @Reference
    BundleContext bundleContext;

    @Reference
    BundleService bundleService;

    @Reference(optional = true)
    Terminal terminal;

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
    }

    @Override
    protected Object doExecute(java.util.List<Bundle> bundles) throws Exception {
        if (noFormat) {
            noEllipsis = true;
        }

        determineBundleLevelThreshold();
        
        // Display active start level.
        FrameworkStartLevel fsl = this.bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
        if (fsl != null) {
            System.out.println("START LEVEL " + fsl.getStartLevel() + " , List Threshold: " + bundleLevelThreshold);
        }

        ShellTable table = new ShellTable();
        if (!noEllipsis && terminal != null && terminal.getWidth() > 0) {
            table.size(terminal.getWidth() - 1);
        }
        table.column("ID").alignRight();
        table.column("State");
        table.column("Lvl").alignRight();
        table.column("Version");

        boolean effectiveShowName = showName || (!showLocation && !showSymbolic && !showUpdate && !showRevisions); 
        
        if (effectiveShowName) {
            table.column("Name");
        }

        if (showLocation) {
            table.column(new Col("Location") {
                @Override
                protected String cut(String value, int size) {
                    if (showLocation && value.length() > size) {
                        String[] parts = value.split("/");
                        String cut = "";
                        int c = parts[0].length() + 4;
                        for (int idx = parts.length - 1; idx > 0; idx--) {
                            if (cut.length() + c + parts[idx].length() + 1 < size) {
                                cut = "/" + parts[idx] + cut;
                            } else {
                                break;
                            }
                        }
                        cut = parts[0] + "/..." + cut;
                        return cut;
                    } else {
                        return super.cut(value, size);
                    }
                }
            });
        }
        if (showSymbolic) {
            table.column("Symbolic name");
        }
        if (showUpdate) {
            table.column("Update location");
        }
        if (showRevisions) {
            table.column("Revisions");
        }

        for (Bundle bundle : bundles) {
            BundleInfo info = this.bundleService.getInfo(bundle);
            if (info.getStartLevel() >= bundleLevelThreshold) {
                String version = info.getVersion();
                ArrayList<Object> rowData = new ArrayList<>();
                rowData.add(info.getBundleId());
                rowData.add(getStateString(info.getState()));
                rowData.add(info.getStartLevel());
                rowData.add(version);
                if (effectiveShowName) {
                    String bundleName = (info.getName() == null) ? info.getSymbolicName() : info.getName();
                    bundleName = (bundleName == null) ? info.getUpdateLocation() : bundleName;
                    String name = bundleName + printFragments(info) + printHosts(info);
                    rowData.add(name);
                }
                if (showLocation) {
                    rowData.add(info.getUpdateLocation());
                }
                if (showSymbolic) {
                    rowData.add(info.getSymbolicName() == null ? "<no symbolic name>" : info.getSymbolicName());
                }
                if (showUpdate) {
                    rowData.add(info.getUpdateLocation());
                }
                if (showRevisions) {
                    rowData.add(info.getRevisions());
                }
                Row row = table.addRow();
                row.addContent(rowData);
            }
        }
        table.print(System.out, !noFormat);
        return null;
    }

    private void determineBundleLevelThreshold() {
        if (bundleLevelThreshold < 0) {
            bundleLevelThreshold = bundleService.getSystemBundleThreshold();
        }
    }

    private String printHosts(BundleInfo info) {
        if (info.getFragmentHosts().size() <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(", Hosts: ");
        boolean first = true;
        for (Bundle host : info.getFragmentHosts()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(host.getBundleId());
            first = false;
        }
        return builder.toString();
    }

    private String printFragments(BundleInfo info) {
        if (info.getFragments().size() <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(", Fragments: ");
        boolean first = true;
        for (Bundle host : info.getFragments()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(host.getBundleId());
            first = false;
        }
        return builder.toString();
    }

    private String getStateString(BundleState state) {
        return (state == null) ? "" : state.toString();
    }

}
