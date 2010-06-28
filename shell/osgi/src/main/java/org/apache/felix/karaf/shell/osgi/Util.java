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
package org.apache.felix.karaf.shell.osgi;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.command.CommandSession;

public class Util
{
    public static String getBundleName(Bundle bundle)
    {
        if (bundle != null)
        {
            String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
            return (name == null)
                ? "Bundle " + Long.toString(bundle.getBundleId())
                : name + " (" + Long.toString(bundle.getBundleId()) + ")";
        }
        return "[STALE BUNDLE]";
    }

    private static StringBuffer m_sb = new StringBuffer();

    public static String getUnderlineString(String s)
    {
        synchronized (m_sb)
        {
            m_sb.delete(0, m_sb.length());
            for (int i = 0; i < s.length(); i++)
            {
                m_sb.append('-');
            }
            return m_sb.toString();
        }
    }

    public static String getValueString(Object obj)
    {
        synchronized (m_sb)
        {
            if (obj instanceof String)
            {
                return (String) obj;
            }
            else if (obj instanceof String[])
            {
                String[] array = (String[]) obj;
                m_sb.delete(0, m_sb.length());
                for (int i = 0; i < array.length; i++)
                {
                    if (i != 0)
                    {
                        m_sb.append(", ");
                    }
                    m_sb.append(array[i].toString());
                }
                return m_sb.toString();
            }
            else if (obj instanceof Boolean)
            {
                return ((Boolean) obj).toString();
            }
            else if (obj instanceof Long)
            {
                return ((Long) obj).toString();
            }
            else if (obj instanceof Integer)
            {
                return ((Integer) obj).toString();
            }
            else if (obj instanceof Short)
            {
                return ((Short) obj).toString();
            }
            else if (obj instanceof Double)
            {
                return ((Double) obj).toString();
            }
            else if (obj instanceof Float)
            {
                return ((Float) obj).toString();
            }
            else if (obj == null)
            {
                return "null";
            }
            else
            {
                return obj.toString();
            }
        }
    }

    /**
     * Check if a bundle is a system bundle (start level < 50)
     * 
     * @param bundleContext
     * @param bundle
     * @return true if the bundle has start level minor than 50
     */
    public static boolean isASystemBundle(BundleContext bundleContext, Bundle bundle) {
        ServiceReference ref = bundleContext.getServiceReference(StartLevel.class.getName());
        if (ref != null) {
            StartLevel sl = (StartLevel) bundleContext.getService(ref);
            if (sl != null) {
                int level = sl.getBundleStartLevel(bundle);
                int sbsl = 49;
                final String sbslProp = bundleContext.getProperty( "karaf.systemBundlesStartLevel" );
                if (sbslProp != null) {
                    try {
                       sbsl = Integer.valueOf( sbslProp );
                    }
                    catch( Exception ignore ) {
                      // ignore
                    }
                }
                return level <= sbsl;
            }
        }
        return false;
    }

    /**
     * Ask the user to confirm the access to a system bundle
     * 
     * @param bundleId
     * @param session
     * @return true if the user confirm
     * @throws IOException
     */
    public static boolean accessToSystemBundleIsAllowed(long bundleId, CommandSession session) throws IOException {
        for (;;) {
            StringBuffer sb = new StringBuffer();
            System.err.print("You are about to access system bundle " + bundleId + ".  Do you want to continue (yes/no): ");
            System.err.flush();
            for (;;) {
                int c = session.getKeyboard().read();
                if (c < 0) {
                    return false;
                }
                System.err.print((char) c);
                if (c == '\r' || c == '\n') {
                    break;
                }
                sb.append((char) c);
            }
            String str = sb.toString();
            if ("yes".equals(str)) {
                return true;
            }
            if ("no".equals(str)) {
                return false;
            }
        }
    }

}
