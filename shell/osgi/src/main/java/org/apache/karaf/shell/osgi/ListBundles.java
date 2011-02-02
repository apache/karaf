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
package org.apache.karaf.shell.osgi;

import java.util.List;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

@Command(scope = "osgi", name = "list", description = "Lists all installed bundles.")
public class ListBundles extends OsgiCommandSupport {

    @Option(name = "-l", aliases = {}, description = "Show the locations", required = false, multiValued = false)
    boolean showLoc;

    @Option(name = "-s", description = "Shows the symbolic name", required = false, multiValued = false)
    boolean showSymbolic;

    @Option(name = "-u", description = "Shows the update locations", required = false, multiValued = false)
    boolean showUpdate;
    
    @Option(name = "-t", description = "Specifies the bundle threshold; bundles with a start-level less than this value will not get printed out.", required = false, multiValued = false)
    int bundleLevelThreshold = -1;

    private List<BundleStateListener.Factory> bundleStateListenerFactories;

    public void setBundleStateListenerFactories(List<BundleStateListener.Factory> bundleStateListenerFactories) {
        this.bundleStateListenerFactories = bundleStateListenerFactories;
    }

    protected Object doExecute() throws Exception {
        ServiceReference ref = getBundleContext().getServiceReference(StartLevel.class.getName());
        StartLevel sl = null;
        if (ref != null) {
            sl = (StartLevel) getBundleContext().getService(ref);
        }
        if (sl == null) {
            System.out.println("StartLevel service is unavailable.");
        }

        ServiceReference pkgref = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin admin = null;
        if (pkgref != null) {
            admin = (PackageAdmin) getBundleContext().getService(pkgref);
            if (admin == null) {
                System.out.println("PackageAdmin service is unavailable.");
            }
        }

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
            if (sl != null) {
                System.out.println("START LEVEL " + sl.getStartLevel() + 
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
            String level = (sl == null) ? "" : "  Level ";
            String headers = "   ID   State       ";
            for (BundleStateListener.Factory factory : bundleStateListenerFactories) {
                BundleStateListener listener = factory.getListener();
                if (listener != null) {
                    headers += "  " + listener.getName() + " ";
                }
            }
            headers += level + msg;
            System.out.println(headers);
            for (int i = 0; i < bundles.length; i++) {
            	if (sl.getBundleStartLevel(bundles[i]) >= bundleLevelThreshold) { 
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
	                if (sl == null) {
	                    level = "1";
	                }
	                else {
	                    level = String.valueOf(sl.getBundleStartLevel(bundles[i]));
	                }
	                while (level.length() < 5) {
	                    level = " " + level;
	                }
	                while (id.length() < 4) {
	                    id = " " + id;
	                }
	                String line = "[" + id + "] [" + getStateString(bundles[i]) + "]";
	                for (BundleStateListener.Factory factory : bundleStateListenerFactories) {
	                    BundleStateListener listener = factory.getListener();
	                    if (listener != null) {
	                        String state = listener.getState(bundles[i]);
	                        line += " [" + getStateString(state, listener.getName().length()) + "]";
	                    }
	                }
	                line += " [" + level + "] " + name;
	                System.out.println(line);
	
	                if (admin != null) {
	                    Bundle[] fragments = admin.getFragments(bundles[i]);
	                    Bundle[] hosts = admin.getHosts(bundles[i]);
	
	                    if (fragments != null) {
	                        System.out.print("                                       Fragments: ");
	                        int ii = 0;
	                        for (Bundle fragment : fragments) {
	                            ii++;
	                            System.out.print(fragment.getBundleId());
	                            if ((fragments.length > 1) && ii < (fragments.length)) {
	                                System.out.print(",");
	                            }
	                        }
	                        System.out.println();
	                    }
	
	                    if (hosts != null) {
	                        System.out.print("                                       Hosts: ");
	                        int ii = 0;
	                        for (Bundle host : hosts) {
	                            ii++;
	                            System.out.print(host.getBundleId());
	                            if ((hosts.length > 1) && ii < (hosts.length)) {
	                                System.out.print(",");
	                            }
	                        }
	                        System.out.println();
	                    }
	
	                }
	            }
            }
        }
        else {
            System.out.println("There are no installed bundles.");
        }

        getBundleContext().ungetService(ref);
        getBundleContext().ungetService(pkgref);

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
