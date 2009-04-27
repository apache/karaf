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

import org.apache.geronimo.gshell.clp.Option;
import org.apache.servicemix.kernel.gshell.core.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class ListBundles extends OsgiCommandSupport {

    @Option(name = "-l", description = "Show locations")
    boolean showLoc;

    @Option(name = "-s", description = "Show symbolic name")
    boolean showSymbolic;

    @Option(name = "-u", description = "Show update")
    boolean showUpdate;

    private SpringApplicationListener springApplicationListener;

    public SpringApplicationListener getSpringApplicationListener() {
        return springApplicationListener;
    }

    public void setSpringApplicationListener(SpringApplicationListener springApplicationListener) {
        this.springApplicationListener = springApplicationListener;
    }

    protected Object doExecute() throws Exception {
        ServiceReference ref = getBundleContext().getServiceReference(StartLevel.class.getName());
        StartLevel sl = null;
        if (ref != null) {
            sl = (StartLevel) getBundleContext().getService(ref);
        }
        if (sl == null) {
            io.out.println("StartLevel service is unavailable.");
        }

        ServiceReference pkgref = getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin admin = null;
        if (pkgref != null) {
            admin = (PackageAdmin) getBundleContext().getService(pkgref);
            if (admin == null) {
                io.out.println("PackageAdmin service is unavailable.");
            }
        }

        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            // Display active start level.
            if (sl != null) {
                io.out.println("START LEVEL " + sl.getStartLevel());
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
            io.out.println("   ID   State         Spring   " + level + msg);
            for (int i = 0; i < bundles.length; i++) {
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
                io.out.println("[" + id + "] ["
                    + getStateString(bundles[i])
                    + "] [" + getSpringStateString(bundles[i])
                    + "] [" + level + "] " + name);

                if (admin != null) {
                    Bundle[] fragments = admin.getFragments(bundles[i]);
                    Bundle[] hosts = admin.getHosts(bundles[i]);

                    if (fragments != null) {
                        io.out.print("                                       Fragments: ");
                        int ii = 0;
                        for (Bundle fragment : fragments) {
                            ii++;
                            io.out.print(fragment.getBundleId());
                            if ((fragments.length > 1) && ii < (fragments.length)) {
                                io.out.print(",");
                            }
                        }
                        io.out.println();
                    }

                    if (hosts != null) {
                        io.out.print("                                       Hosts: ");
                        int ii = 0;
                        for (Bundle host : hosts) {
                            ii++;
                            io.out.print(host.getBundleId());
                            if ((hosts.length > 1) && ii < (hosts.length)) {
                                io.out.print(",");
                            }
                        }
                        io.out.println();
                    }

                }
            }
        }
        else {
            io.out.println("There are no installed bundles.");
        }

        getBundleContext().ungetService(ref);
        getBundleContext().ungetService(pkgref);

        return Result.SUCCESS;
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

    public String getSpringStateString(Bundle bundle) {
        SpringApplicationListener.SpringState state = springApplicationListener.getSpringState(bundle);
        if (state == SpringApplicationListener.SpringState.Waiting) {
            return "Waiting";
        } else if (state == SpringApplicationListener.SpringState.Started) {
            return "Started";
        } else if (state == SpringApplicationListener.SpringState.Failed) {
            return "Failed ";
        } else {
            return "       ";
        }
    }
}
