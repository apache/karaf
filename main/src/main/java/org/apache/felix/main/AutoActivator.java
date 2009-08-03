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
package org.apache.felix.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.osgi.framework.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;

public class AutoActivator implements BundleActivator
{
    /**
     * The property name used for the bundle directory.
    **/
    public static final String AUTO_DEPLOY_DIR_PROPERY = "felix.auto.deploy.dir";
    /**
     * The default name used for the bundle directory.
    **/
    public static final String AUTO_DEPLOY_DIR_VALUE = "bundle";
    /**
     * The property name used to enable/disable automatic bundle deployment.
    **/
    public static final String AUTO_DEPLOY_PROP = "felix.auto.deploy";
    /**
     * The property name prefix for the launcher's auto-install property.
    **/
    public static final String AUTO_INSTALL_PROP = "felix.auto.install";
    /**
     * The property name prefix for the launcher's auto-start property.
    **/
    public static final String AUTO_START_PROP = "felix.auto.start";

    private final Map m_configMap;

    public AutoActivator(Map configMap)
    {
        m_configMap = configMap;
    }

    /**
     * Used to instigate auto-install and auto-start configuration
     * property processing via a custom framework activator during
     * framework startup.
     * @param context The system bundle context.
    **/
    public void start(BundleContext context)
    {
        processAutoDeploy(context);
        processAutoProperties(context);
    }

    /**
     * Currently does nothing as part of framework shutdown.
     * @param context The system bundle context.
    **/
    public void stop(BundleContext context)
    {
        // Do nothing.
    }

    private void processAutoDeploy(BundleContext context)
    {
        // Determine if auto deploy is enabled; default is enabled.
        String enabled = (String) m_configMap.get(AUTO_DEPLOY_PROP);
        enabled = (enabled == null) ? Boolean.TRUE.toString() : enabled;
        if (Boolean.valueOf(enabled).booleanValue())
        {
            // Get the auto deploy directory.
            String autoDir = (String) m_configMap.get(AUTO_DEPLOY_DIR_PROPERY);
            autoDir = (autoDir == null) ? AUTO_DEPLOY_DIR_VALUE : autoDir;
            // Look in the specified bundle directory to create a list
            // of all JAR files to install.
            File[] files = new File(autoDir).listFiles();
            if (files != null)
            {
                Arrays.sort(files);
                List bundleList = new ArrayList();
                for (int i = 0; i < files.length; i++)
                {
                    if (files[i].getName().endsWith(".jar"))
                    {
                        bundleList.add(files[i]);
                    }
                }

                // Install bundle JAR files and remember the bundle objects.
                final List installedList = new ArrayList();
                for (int i = 0; i < bundleList.size(); i++)
                {
                    try
                    {
                        Bundle b = context.installBundle(
                            ((File) bundleList.get(i)).toURI().toString());
                        installedList.add(b);
                    }
                    catch (BundleException ex)
                    {
                        System.err.println("Auto-deploy install: "
                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
                    }
                }

                // Start all installed bundles.
                for (int i = 0; i < installedList.size(); i++)
                {
                    try
                    {
                        if (!isFragment((Bundle) installedList.get(i)))
                        {
                            ((Bundle) installedList.get(i)).start();
                        }
                    }
                    catch (BundleException ex)
                    {
                        System.err.println("Auto-deploy start: "
                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Processes the auto-install and auto-start properties from the
     * specified configuration properties.
     * </p>
     */
    private void processAutoProperties(BundleContext context)
    {
        // Retrieve the Start Level service, since it will be needed
        // to set the start level of the installed bundles.
        StartLevel sl = (StartLevel) context.getService(
            context.getServiceReference(org.osgi.service.startlevel.StartLevel.class.getName()));

        // Retrieve all auto-install and auto-start properties and install
        // their associated bundles. The auto-install property specifies a
        // space-delimited list of bundle URLs to be automatically installed
        // into each new profile, while the auto-start property specifies
        // bundles to be installed and started. The start level to which the
        // bundles are assigned is specified by appending a ".n" to the
        // property name, where "n" is the desired start level for the list
        // of bundles. If no start level is specified, the default start
        // level is assumed.
        for (Iterator i = m_configMap.keySet().iterator(); i.hasNext(); )
        {
            String key = ((String) i.next()).toLowerCase();

            // Ignore all keys that are not an auto property.
            if (!key.startsWith(AUTO_INSTALL_PROP) && !key.startsWith(AUTO_START_PROP))
            {
                continue;
            }

            // If the auto property does not have a start level,
            // then assume it is the default bundle start level, otherwise
            // parse the specified start level.
            int startLevel = sl.getInitialBundleStartLevel();
            if (!key.equals(AUTO_INSTALL_PROP) && !key.equals(AUTO_START_PROP))
            {
                try
                {
                    startLevel = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
                }
                catch (NumberFormatException ex)
                {
                    System.err.println("Invalid property: " + key);
                }
            }

            // Parse and install the bundles associated with the key.
            StringTokenizer st = new StringTokenizer((String) m_configMap.get(key), "\" ", true);
            for (String location = nextLocation(st); location != null; location = nextLocation(st))
            {
                try
                {
                    Bundle b = context.installBundle(location, null);
                    sl.setBundleStartLevel(b, startLevel);
                }
                catch (Exception ex)
                {
                    System.err.println("Auto-properties install: "
                        + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
                }
            }
        }

        // Now loop through the auto-start bundles and start them.
        for (Iterator i = m_configMap.keySet().iterator(); i.hasNext(); )
        {
            String key = ((String) i.next()).toLowerCase();
            if (key.startsWith(AUTO_START_PROP))
            {
                StringTokenizer st = new StringTokenizer((String) m_configMap.get(key), "\" ", true);
                for (String location = nextLocation(st); location != null; location = nextLocation(st))
                {
                    // Installing twice just returns the same bundle.
                    try
                    {
                        Bundle b = context.installBundle(location, null);
                        if (b != null)
                        {
                            b.start();
                        }
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Auto-properties start: "
                            + ex + ((ex.getCause() != null) ? " - " + ex.getCause() : ""));
                    }
                }
            }
        }
    }

    private static String nextLocation(StringTokenizer st)
    {
        String retVal = null;

        if (st.countTokens() > 0)
        {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok = null;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit))
            {
                tok = st.nextToken(tokenList);
                if (tok.equals("\""))
                {
                    inQuote = ! inQuote;
                    if (inQuote)
                    {
                        tokenList = "\"";
                    }
                    else
                    {
                        tokenList = "\" ";
                    }

                }
                else if (tok.equals(" "))
                {
                    if (tokStarted)
                    {
                        retVal = tokBuf.toString();
                        tokStarted=false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                }
                else
                {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && (tokStarted))
            {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }

    private static boolean isFragment(Bundle bundle)
    {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
    }
}