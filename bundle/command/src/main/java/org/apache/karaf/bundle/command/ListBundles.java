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
import org.apache.karaf.shell.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.FrameworkStartLevel;

@Command(scope = "bundle", name = "list", description = "Lists all installed bundles.")
public class ListBundles extends OsgiCommandSupport {

    @Option(name = "-l", aliases = {}, description = "Show the locations", required = false, multiValued = false)
    boolean showLoc;

    @Option(name = "-s", description = "Shows the symbolic name", required = false, multiValued = false)
    boolean showSymbolic;

    @Option(name = "-u", description = "Shows the update locations", required = false, multiValued = false)
    boolean showUpdate;

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
        table.column(getNameHeader());
        
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            BundleInfo info = this.bundleService.getInfo(bundle);
            if (info.getStartLevel() >= bundleLevelThreshold) {
                String name = getNameToShow(info) + printFragments(info) + printHosts(info);
                String version = info.getVersion();
                table.addRow().addContent(info.getBundleId(), getStateString(info.getState()), 
                        info.getStartLevel(), version, name);
            }
        }
        table.print(System.out, !noFormat);

        return null;
    }

    private String getNameHeader() {
        String msg = "Name";
        if (showLoc) {
            msg = "Location";
        } else if (showSymbolic) {
            msg = "Symbolic name";
        } else if (showUpdate) {
            msg = "Update location";
        }
        return msg;
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

    /**
     * Overwrite the default value is the user specifically requested to display
     * one or the other.
     * 
     * @param info
     * @return
     */
    private String getNameToShow(BundleInfo info) {
        if (showLoc) {
            return info.getUpdateLocation();
        } else if (showSymbolic) {
            return info.getSymbolicName() == null ? "<no symbolic name>" : info.getSymbolicName();
        } else if (showUpdate) {
            return info.getUpdateLocation();
        } else {
            String name = (info.getName() == null) ? info.getSymbolicName() : info.getName();
            return (name == null) ? info.getUpdateLocation() : name;
        }
    }

}
