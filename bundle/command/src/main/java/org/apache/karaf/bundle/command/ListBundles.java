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
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.karaf.shell.table.Row;
import org.apache.karaf.shell.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.FrameworkStartLevel;

import java.util.ArrayList;

@Command(scope = "bundle", name = "list", description = "Lists all installed bundles.")
public class ListBundles extends OsgiCommandSupport {

    @Option(name = "--no-name", description = "Don't show bundle name", required = false, multiValued = false)
    boolean dontShowName;

    @Option(name = "-l", aliases = {}, description = "Show the locations", required = false, multiValued = false)
    boolean showLoc;

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

    private BundleService bundleService;

    public void setBundleService(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    protected Object doExecute() throws Exception {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles == null) {
            System.out.println("There are no installed bundles.");
            return null;
        }

        determineBundleLevelThreshold();

        // Display active start level.
        FrameworkStartLevel fsl = getBundleContext().getBundle(0).adapt(FrameworkStartLevel.class);
        if (fsl != null) {
            System.out.println("START LEVEL " + fsl.getStartLevel() + " , List Threshold: " + bundleLevelThreshold);
        }

        ShellTable table = new ShellTable();
        table.column("ID").alignRight();
        table.column("State");
        table.column("Lvl").alignRight();
        table.column("Version");

        if (!dontShowName) {
            table.column("Name");
        }
        if (showLoc) {
            table.column("Location");
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

        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            BundleInfo info = this.bundleService.getInfo(bundle);
            if (info.getStartLevel() >= bundleLevelThreshold) {
                ArrayList<Object> rowData = new ArrayList<Object>();
                rowData.add(info.getBundleId());
                rowData.add(getStateString(info.getState()));
                rowData.add(info.getStartLevel());
                rowData.add(info.getVersion());
                if (!dontShowName) {
                    String bundleName = (info.getName() == null) ? info.getSymbolicName() : info.getName();
                    bundleName = (bundleName == null) ? info.getUpdateLocation() : bundleName;
                    String name = bundleName + printFragments(info) + printHosts(info);
                    rowData.add(name);
                }
                if (showLoc) {
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
        final String sbslProp = bundleContext.getProperty("karaf.systemBundlesStartLevel");
        if (sbslProp != null) {
            try {
                if (bundleLevelThreshold < 0) {
                    bundleLevelThreshold = Integer.valueOf(sbslProp);
                }
            } catch (Exception ignore) {
                // ignore
            }
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
            builder.append((first ? "" : ", ") + host.getBundleId());
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
            builder.append((first ? "" : ", ") + host.getBundleId());
            first = false;
        }
        return builder.toString();
    }

    private String getStateString(BundleState state) {
        return (state == null) ? "" : state.toString();
    }

}
