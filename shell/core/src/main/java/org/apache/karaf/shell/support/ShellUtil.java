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
package org.apache.karaf.shell.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;

import javax.security.auth.Subject;

import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

public class ShellUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellUtil.class);

    public static String getBundleName(Bundle bundle) {
        if (bundle != null) {
            String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
            return (name == null)
                    ? "Bundle " + Long.toString(bundle.getBundleId())
                    : name + " (" + Long.toString(bundle.getBundleId()) + ")";
        }
        return "[STALE BUNDLE]";
    }

    public static String getUnderlineString(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append('-');
        }
        return sb.toString();
    }

    public static String getValueString(Object obj) {
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
     * Check if a bundle is a system bundle (start level minor than 50).
     *
     * @param bundleContext the current bundle context.
     * @param bundle the bundle to check.
     * @return true if the bundle has start level minor than 50.
     */
    public static boolean isASystemBundle(BundleContext bundleContext, Bundle bundle) {
        int level = bundle.adapt(BundleStartLevel.class).getStartLevel();
        int sbsl = 49;
        final String sbslProp = bundleContext.getProperty("karaf.systemBundlesStartLevel");
        if (sbslProp != null) {
            try {
                sbsl = Integer.valueOf(sbslProp);
            } catch (Exception ignore) {
                // ignore
            }
        }
        return level <= sbsl;
    }

    public static boolean getBoolean(Session session, String name) {
        Object s = session.get(name);
        if (s == null) {
            s = System.getProperty(name);
        }
        if (s == null) {
            return false;
        }
        if (s instanceof Boolean) {
            return (Boolean) s;
        }
        return Boolean.parseBoolean(s.toString());
    }

    public static void logException(Session session, Throwable t) {
        try {
            // Store last exception in session
            session.put(Session.LAST_EXCEPTION, t);
            // Log exception
            String name = t.getClass().getSimpleName();
            if ("CommandNotFoundException".equals(name)) {
                LOGGER.debug("Unknown command entered", t);
            } else if ("CommandException".equals(name)) {
                LOGGER.debug("Command exception (Undefined option, ...)", t);
            } else {
                LOGGER.error("Exception caught while executing command", t);
            }
            // Display exception
            String pst = getPrintStackTraces(session);
            if ("always".equals(pst)) {
                session.getConsole().print(COLOR_RED);
                t.printStackTrace(session.getConsole());
                session.getConsole().print(COLOR_DEFAULT);
            } else if ("CommandNotFoundException".equals(name)) {
                String str = COLOR_RED + "Command not found: "
                        + INTENSITY_BOLD + t.getClass().getMethod("getCommand").invoke(t) + INTENSITY_NORMAL
                        + COLOR_DEFAULT;
                session.getConsole().println(str);
            } else if ("CommandException".equals(name)) {
                String str;
                try {
                    str = (String) t.getClass().getMethod("getNiceHelp").invoke(t);
                } catch (Throwable ignore) {
                    str = COLOR_RED + t.getMessage() + COLOR_DEFAULT;
                }
                session.getConsole().println(str);
            } else  if ("execution".equals(pst)) {
                session.getConsole().print(COLOR_RED);
                t.printStackTrace(session.getConsole());
                session.getConsole().print(COLOR_DEFAULT);
            } else {
                String str = COLOR_RED + "Error executing command: "
                        + (t.getMessage() != null ? t.getMessage() : t.getClass().getName())
                        + COLOR_DEFAULT;
                session.getConsole().println(str);
            }
        } catch (Exception ignore) {
            // ignore
        }
    }

    private static String getPrintStackTraces(Session session) {
        Object pst = session.get(Session.PRINT_STACK_TRACES);
        if (pst == null) {
            pst = System.getProperty(Session.PRINT_STACK_TRACES);
        }
        if (pst == null) {
            return "never";
        } else if (pst instanceof Boolean) {
            return ((Boolean) pst) ? "always" : "never";
        } else {
            return pst.toString().toLowerCase();
        }
    }

    public static String getCurrentUserName() {
        AccessControlContext acc = AccessController.getContext();
        final Subject subject = Subject.getSubject(acc);
        if (subject != null && subject.getPrincipals(UserPrincipal.class).iterator().hasNext()) {
            return subject.getPrincipals(UserPrincipal.class).iterator().next().getName();
        } else {
            return null;
        }
    }

}
