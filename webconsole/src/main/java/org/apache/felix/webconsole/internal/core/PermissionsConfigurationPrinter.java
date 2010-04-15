/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole.internal.core;

import java.io.PrintWriter;
import java.lang.reflect.Method;

import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * PermissionsPrinter reads the given permissions from PermissionAdmin and 
 * ConditionalPermissionAdmin and prints them.
 */
public final class PermissionsConfigurationPrinter extends AbstractConfigurationPrinter
{

    private static final String TITLE = "Permissions";

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public final String getTitle()
    {
        return TITLE;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public final void printConfiguration(PrintWriter pw)
    {
        final BundleContext bc = getBundleContext();
        final ServiceReference paRef = bc.getServiceReference(PermissionAdmin.class.getName());
        final ServiceReference cpaRef = bc.getServiceReference(ConditionalPermissionAdmin.class.getName());
        final PermissionAdmin pa = paRef != null ? (PermissionAdmin) bc.getService(paRef)
            : null;
        final ConditionalPermissionAdmin cpa = cpaRef != null ? (ConditionalPermissionAdmin) bc.getService(cpaRef)
            : null;

        try
        {
            pw.print("Status: Permission Admin ");
            if (null == pa)
                pw.print("not ");
            pw.print("available, Conditional Permission Admin ");
            if (null == cpa)
                pw.print("not ");
            pw.println("available.");

            if (pa != null)
            {
                pw.println();
                pw.println("Permission Admin");

                pw.println("  Default Permissions:");
                print(pa.getDefaultPermissions(), pw);

                final String locations[] = pa.getLocations();
                for (int i = 0; locations != null && i < locations.length; i++)
                {
                    pw.print("  Location: ");
                    pw.println(locations[i]);
                    print(pa.getPermissions(locations[i]), pw);
                }
            }

            if (cpa != null)
            {
                pw.println();
                pw.println("Conditional Permission Admin");

                Method getAccessDecision = null;
                try
                {
                    getAccessDecision = ConditionalPermissionInfo.class.getMethod(
                        "getAccessDecision", null);
                }
                catch (Throwable t)
                {
                    // it is r4.0 framework, not r4.2
                }

                boolean hasPermissions = false;
                //final java.util.List list = cpa.newConditionalPermissionUpdate().getConditionalPermissionInfos();
                //for (int i = 0; list != null && i < list.size(); i++)
                for (java.util.Enumeration e = cpa.getConditionalPermissionInfos(); e.hasMoreElements();)
                {
                    hasPermissions = true;
                    //final ConditionalPermissionInfo info = (ConditionalPermissionInfo) list.get(i);
                    final ConditionalPermissionInfo info = (ConditionalPermissionInfo) e.nextElement();
                    pw.print("  ");
                    pw.print(info.getName());

                    if (getAccessDecision != null)
                    {
                        try
                        {
                            final Object ad = getAccessDecision.invoke(info, null);
                            pw.print(" (");
                            pw.print(ad);
                            pw.print(")");
                        }
                        catch (Throwable t)
                        {
                            // ignore - will not print it
                        }
                    }

                    pw.println();
                    pw.println("  Conditions:");
                    print(info.getConditionInfos(), pw);
                    pw.println("  Permissions:");
                    print(info.getPermissionInfos(), pw);
                }

                if (!hasPermissions)
                    pw.println("  n/a");
            }
        }
        finally
        {
            if (paRef != null)
                bc.ungetService(paRef);
            if (cpaRef != null)
                bc.ungetService(cpaRef);
        }
    }

    private static final void print(PermissionInfo[] infos, PrintWriter pw)
    {
        if (infos == null || infos.length == 0)
        {
            pw.println("    n/a");
        }
        else
        {
            for (int i = 0, len = infos.length; i < len; i++)
            {
                pw.print("    ");
                pw.println(infos[i].getEncoded());
            }
        }
        pw.println();
    }

    private static final void print(ConditionInfo[] infos, PrintWriter pw)
    {
        if (infos == null || infos.length == 0)
        {
            pw.println("    empty conditions set");
        }
        else
        {
            for (int i = 0, len = infos.length; i < len; i++)
            {
                pw.print("    ");
                pw.println(infos[i].getEncoded());
            }
        }
        pw.println();
    }

}
