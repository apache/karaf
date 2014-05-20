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
package org.apache.karaf.shell.osgi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

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

    public static String getUnderlineString(String s)
    {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append('-');
        }
        return sb.toString();
    }

    public static String getValueString(Object obj)
    {
        if (obj == null) {
            return "null";
        } else if (obj instanceof boolean[]) {
            return Arrays.toString((boolean[]) obj);
        } else if (obj instanceof byte[]) {
            return Arrays.toString((byte[]) obj);
        } else if (obj instanceof char[]) {
            return Arrays.toString((char[]) obj);
        } else if (obj instanceof double[]) {
            return Arrays.toString((double[]) obj);
        } else if (obj instanceof float[]) {
            return Arrays.toString((float[]) obj);
        } else if (obj instanceof int[]) {
            return Arrays.toString((int[]) obj);
        } else if (obj instanceof long[]) {
            return Arrays.toString((long[]) obj);
        } else if (obj instanceof short[]) {
            return Arrays.toString((short[]) obj);
        } else if (obj instanceof Collection<?>) {
            Object[] array = ((Collection<?>) obj).toArray();
            return getValueString(array);
        } else if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < array.length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(getValueString(array[i]));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return obj.toString();
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
            System.err.println("You are about to access system bundle " + bundleId + ".  Do you wish to continue (yes/no): ");
            System.err.flush();
            for (;;) {
                int c = session.getKeyboard().read();
                if (c < 0) {
                    return false;
                }
                if (c == '\r' || c == '\n') {
                    System.err.println();
                    System.err.flush();
                    break;
                }
                if (c == 127 || c == 'b') {
                    System.err.print((char)'\b');
                    System.err.print((char)' ');
                    System.err.print((char)'\b');
                } else {
                    System.err.print((char)c);
                }
                
                System.err.flush();
                if (c == 127 || c == 'b') {
                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                } else {
                    sb.append((char)c);
                }
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
