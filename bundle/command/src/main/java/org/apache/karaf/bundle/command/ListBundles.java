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

import java.util.List;

import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;

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

    private List<BundleStateService> bundleStateServices;

    public ListBundles(List<BundleStateService> bundleStateServices) {
        super();
        this.bundleStateServices = bundleStateServices;
    }

    protected Object doExecute() throws Exception {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            // Determine threshold
            final String sbslProp = bundleContext.getProperty("karaf.systemBundlesStartLevel");
            if (sbslProp != null) {
                try {
                   if (bundleLevelThreshold < 0) {
                       bundleLevelThreshold = Integer.valueOf( sbslProp );
                   }
                }
                catch( Exception ignore ) {
                   // ignore
                }
            }
            // Display active start level.
            FrameworkStartLevel fsl = getBundleContext().getBundle(0).adapt(FrameworkStartLevel.class);
            if (fsl != null) {
                System.out.println("START LEVEL " + fsl.getStartLevel() +
                                   " , List Threshold: " + bundleLevelThreshold);
            }

            // Print column headers.
            String msg = " Name";
            if (showLoc) {
               msg = " Location";
            }
            else if (showSymbolic) {
               msg = " Symbolic name";
            }
            else if (showUpdate) {
               msg = " Update location";
            }
            String level = (fsl == null) ? "" : "  Level ";
            String headers = "   ID   State       ";
            for (BundleStateService stateService : bundleStateServices) {
                headers += "  " + stateService.getName() + " ";
            }
            headers += level + msg;
            System.out.println(headers);
            for (int i = 0; i < bundles.length; i++) {
                BundleStartLevel bsl = bundles[i].adapt(BundleStartLevel.class);
            	if (bsl == null || bsl.getStartLevel() >= bundleLevelThreshold) {
	                // Get the bundle name or location.
	                String name = (String) bundles[i].getHeaders().get(Constants.BUNDLE_NAME);
	                // If there is no name, then default to symbolic name.
	                name = (name == null) ? bundles[i].getSymbolicName() : name;
	                // If there is no symbolic name, resort to location.
	                name = (name == null) ? bundles[i].getLocation() : name;
	
	                // Overwrite the default value is the user specifically
	                // requested to display one or the other.
	                if (showLoc) {
	                    name = bundles[i].getLocation();
	                }
	                else if (showSymbolic) {
	                    name = bundles[i].getSymbolicName();
	                    name = (name == null) ? "<no symbolic name>" : name;
	                }
	                else if (showUpdate) {
	                    name = (String) bundles[i].getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
	                    name = (name == null) ? bundles[i].getLocation() : name;
	                }
	                // Show bundle version if not showing location.
	                String version = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
	                name = (!showLoc && !showUpdate && (version != null)) ? name + " (" + version + ")" : name;
	                long l = bundles[i].getBundleId();
	                String id = String.valueOf(l);
	                if (bsl == null) {
	                    level = "1";
	                }
	                else {
	                    level = String.valueOf(bsl.getStartLevel());
	                }
	                while (level.length() < 5) {
	                    level = " " + level;
	                }
	                while (id.length() < 4) {
	                    id = " " + id;
	                }
	                String line = "[" + id + "] [" + getStateString(bundles[i]) + "]";
	                for (BundleStateService stateService : bundleStateServices) {
	                    String state = stateService.getState(bundles[i]);
	                    line += " [" + getStateString(state, stateService.getName().length()) + "]";
	                }
	                line += " [" + level + "] " + name;
	                System.out.println(line);

                    boolean isFragment = bundles[i].getHeaders().get(Constants.FRAGMENT_HOST) != null;
                    if (!isFragment) {
                        int nb = 0;
                        for (BundleRevision revision : bundles[i].adapt(BundleRevisions.class).getRevisions()) {
                            if (revision.getWiring() != null) {
                                List<BundleWire> wires = revision.getWiring().getProvidedWires(null);
                                if (wires != null) {
                                    for (BundleWire w : wires) {
                                        if (w.getCapability().getNamespace().equals(BundleRevision.HOST_NAMESPACE)) {
                                            Bundle b = w.getRequirerWiring().getBundle();
                                            if (nb == 0) {
                                                System.out.print("                                       Fragments: ");
                                            } else {
                                                System.out.print(",");
                                            }
                                            System.out.print(b.getBundleId());
                                            nb++;
                                        }
                                    }
                                }
                             }
                        }
                        if (nb > 0) {
                            System.out.println();
                        }
                    } else {
                        int nb = 0;
                        for (BundleRevision revision : bundles[i].adapt(BundleRevisions.class).getRevisions()) {
                            if (revision.getWiring() != null) {
                                List<BundleWire> wires = revision.getWiring().getRequiredWires(null);
                                if (wires != null) {
                                    for (BundleWire w : wires) {
                                        Bundle b = w.getProviderWiring().getBundle();
                                        if (b != null) {
                                            if (nb == 0) {
                                                System.out.print("                                       Hosts: ");
                                            } else {
                                                System.out.println(",");
                                            }
                                            System.out.print(b.getBundleId());
                                            nb++;
                                        }                                        
                                    }
                                }
                            }
                        }
                        if (nb > 0) {
                            System.out.println();
                        }
                    }
	            }
            }
        }
        else {
            System.out.println("There are no installed bundles.");
        }
        return null;
    }

    public String getStateString(Bundle bundle)
    {
        int state = bundle.getState();
        if (state == Bundle.ACTIVE) {
            return "Active     ";
        } else if (state == Bundle.INSTALLED) {
            return "Installed  ";
        } else if (state == Bundle.RESOLVED) {
            return "Resolved   ";
        } else if (state == Bundle.STARTING) {
            return "Starting   ";
        } else if (state == Bundle.STOPPING) {
            return "Stopping   ";
        } else {
            return "Unknown    ";
        }
    }

    public String getStateString(String state, int length) {
        if (state == null) {
            state = "";
        }
        while (state.length() < length) {
            state += " ";
        }
        return state;
    }
}
