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
package org.apache.felix.gogo.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class Basic
{
    private final BundleContext m_bc;

    public Basic(BundleContext bc)
    {
        m_bc = bc;
    }

    @Descriptor("query bundle start level")
    public void bundlelevel(
        @Descriptor("bundle to query") Bundle bundle)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get start level service.
        StartLevel sl = Util.getService(m_bc, StartLevel.class, refs);
        if (sl == null)
        {
            System.out.println("Start Level service is unavailable.");
        }
        // Get the bundle start level.
        else
        {
            if (bundle != null)
            {
                System.out.println(bundle + " is level " + sl.getBundleStartLevel(bundle));
            }
        }

        Util.ungetServices(m_bc, refs);
    }

    @Descriptor("set bundle start level or initial bundle start level")
    public void bundlelevel(
        @Descriptor("set the bundle's start level")
        @Parameter(names={ "-s", "--setlevel" }, presentValue="true",
            absentValue="false") boolean set,
        @Descriptor("set the initial bundle start level")
        @Parameter(names={ "-i", "--setinitial" }, presentValue="true",
            absentValue="false") boolean initial,
        @Descriptor("target level") int level,
        @Descriptor("target identifiers") Bundle[] bundles)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get start level service.
        StartLevel sl = Util.getService(m_bc, StartLevel.class, refs);
        if (sl == null)
        {
            System.out.println("Start Level service is unavailable.");
        }
        else if (set && initial)
        {
            System.out.println("Cannot specify '-s' and '-i' at the same time.");
        }
        else if (!set && !initial)
        {
            System.out.println("Must specify either '-s' or '-i'.");
        }
        else if (level <= 0)
        {
            System.out.println("Specified start level must be greater than zero.");
        }
        // Set the initial bundle start level.
        else if (initial)
        {
            if ((bundles != null) && (bundles.length == 0))
            {
                sl.setInitialBundleStartLevel(level);
            }
            else
            {
                System.out.println(
                    "Cannot specify bundles when setting initial start level.");
            }
        }
        // Set the bundle start level.
        else if (set)
        {
            if ((bundles != null) && (bundles.length == 0))
            {
                for (Bundle bundle: bundles)
                {
                    sl.setBundleStartLevel(bundle, level);
                }
            }
            else
            {
                System.out.println("Must specify target bundles.");
            }
        }

        Util.ungetServices(m_bc, refs);
    }

    @Descriptor("query framework active start level")
    public void frameworklevel()
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get start level service.
        StartLevel sl = Util.getService(m_bc, StartLevel.class, refs);
        if (sl == null)
        {
            System.out.println("Start Level service is unavailable.");
        }
        System.out.println("Level is " + sl.getStartLevel());
        Util.ungetServices(m_bc, refs);
    }

    @Descriptor("set framework active start level")
    public void frameworklevel(
        @Descriptor("target start level") int level)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get start level service.
        StartLevel sl = Util.getService(m_bc, StartLevel.class, refs);
        if (sl == null)
        {
            System.out.println("Start Level service is unavailable.");
        }
        sl.setStartLevel(level);
        Util.ungetServices(m_bc, refs);
    }

    @Descriptor("display bundle headers")
    public void headers(
        @Descriptor("target bundles") Bundle[] bundles)
    {
        bundles = ((bundles == null) || (bundles.length == 0))
            ? m_bc.getBundles() : bundles;
        for (Bundle bundle : bundles)
        {
            String title = Util.getBundleName(bundle);
            System.out.println("\n" + title);
            System.out.println(Util.getUnderlineString(title.length()));
            Dictionary dict = bundle.getHeaders();
            Enumeration keys = dict.keys();
            while (keys.hasMoreElements())
            {
                Object k = (String) keys.nextElement();
                Object v = dict.get(k);
                System.out.println(k + " = " + Util.getValueString(v));
            }
        }
    }

    @Descriptor("displays available commands")
    public void help()
    {
        Map<String, List<Method>> commands = getCommands();
        for (String name : commands.keySet())
        {
            System.out.println(name);
        }
    }

    @Descriptor("displays information about a specific command")
    public void help(
        @Descriptor("target command") String name)
    {
        Map<String, List<Method>> commands = getCommands();

        List<Method> methods = null;

        // If the specified command doesn't have a scope, then
        // search for matching methods by ignoring the scope.
        int scopeIdx = name.indexOf(':');
        if (scopeIdx < 0)
        {
            for (Entry<String, List<Method>> entry : commands.entrySet())
            {
                String k = entry.getKey().substring(entry.getKey().indexOf(':') + 1);
                if (name.equals(k))
                {
                    name = entry.getKey();
                    methods = entry.getValue();
                    break;
                }
            }
        }
        // Otherwise directly look up matching methods.
        else
        {
            methods = commands.get(name);
        }

        if ((methods != null) && (methods.size() > 0))
        {
            for (Method m : methods)
            {
                Descriptor d = m.getAnnotation(Descriptor.class);
                if (d == null)
                {
                    System.out.println("\n" + m.getName());
                }
                else
                {
                    System.out.println("\n" + m.getName() + " - " + d.value());
                }

                System.out.println("   scope: " + name.substring(0, name.indexOf(':')));

                // Get flags and options.
                Class[] paramTypes = m.getParameterTypes();
                Map<String, Parameter> flags = new TreeMap();
                Map<String, String> flagDescs = new TreeMap();
                Map<String, Parameter> options = new TreeMap();
                Map<String, String> optionDescs = new TreeMap();
                List<String> params = new ArrayList();
                Annotation[][] anns = m.getParameterAnnotations();
                for (int paramIdx = 0; paramIdx < anns.length; paramIdx++)
                {
                    Parameter p = findAnnotation(anns[paramIdx], Parameter.class);
                    d = findAnnotation(anns[paramIdx], Descriptor.class);
                    if (p != null)
                    {
                        if (p.presentValue().equals(Parameter.UNSPECIFIED))
                        {
                            options.put(p.names()[0], p);
                            if (d != null)
                            {
                                optionDescs.put(p.names()[0], d.value());
                            }
                        }
                        else
                        {
                            flags.put(p.names()[0], p);
                            if (d != null)
                            {
                                flagDescs.put(p.names()[0], d.value());
                            }
                        }
                    }
                    else if (d != null)
                    {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add(d.value());
                    }
                    else
                    {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add("");
                    }
                }

                // Print flags and options.
                if (flags.size() > 0)
                {
                    System.out.println("   flags:");
                    for (Entry<String, Parameter> entry : flags.entrySet())
                    {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        System.out.print("      " + names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++)
                        {
                            System.out.print(", " + names[aliasIdx]);
                        }
                        System.out.println("   " + flagDescs.get(entry.getKey()));
                    }
                }
                if (options.size() > 0)
                {
                    System.out.println("   options:");
                    for (Entry<String, Parameter> entry : options.entrySet())
                    {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        System.out.print("      " + names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++)
                        {
                            System.out.print(", " + names[aliasIdx]);
                        }
                        System.out.println("   "
                            + optionDescs.get(entry.getKey())
                            + ((entry.getValue().absentValue() == null) ? "" : " [optional]"));
                    }
                }
                if (params.size() > 0)
                {
                    System.out.println("   parameters:");
                    for (Iterator<String> it = params.iterator(); it.hasNext(); )
                    {
                        System.out.println("      " + it.next() + "   " + it.next());
                    }
                }
            }
        }
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] anns, Class<T> clazz)
    {
        for (int i = 0; (anns != null) && (i < anns.length); i++)
        {
            if (clazz.isInstance(anns[i]))
            {
                return clazz.cast(anns[i]);
            }
        }
        return null;
    }

    private Map<String, List<Method>> getCommands()
    {
        ServiceReference[] refs = null;
        try
        {
            refs = m_bc.getAllServiceReferences(null, "(osgi.command.scope=*)");
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
        }

        Map<String, List<Method>> commands = new TreeMap();

        for (ServiceReference ref : refs)
        {
            Object svc = m_bc.getService(ref);
            if (svc != null)
            {
                String scope = (String) ref.getProperty("osgi.command.scope");
                String[] funcs = (String[]) ref.getProperty("osgi.command.function");

                for (String func : funcs)
                {
                    commands.put(scope + ":" + func, new ArrayList());
                }

                if (!commands.isEmpty())
                {
                    Method[] methods = svc.getClass().getMethods();
                    for (Method method : methods)
                    {
                        List<Method> commandMethods = commands.get(scope + ":" + method.getName());
                        if (commandMethods != null)
                        {
                            commandMethods.add(method);
                        }
                    }
                }

                // Remove any missing commands.
                Iterator<Entry<String, List<Method>>> it = commands.entrySet().iterator();
                while (it.hasNext())
                {
                    if (it.next().getValue().size() == 0)
                    {
                        it.remove();
                    }
                }
            }
        }

        return commands;
    }

    @Descriptor("inspects bundle dependency information")
    public void inspect(
        @Descriptor("(package | bundle | fragment | service)") String type,
        @Descriptor("(capability | requirement)") String direction,
        @Descriptor("target bundles") Bundle[] bundles)
    {
        Inspect.inspect(m_bc, type, direction, bundles);
    }

    @Descriptor("install bundle using URLs")
    public void install(
        @Descriptor("target URLs") String[] urls)
    {
        StringBuffer sb = new StringBuffer();

        for (String url : urls)
        {
            String location = url.trim();
            Bundle bundle = null;
            try
            {
                bundle = m_bc.installBundle(location, null);
            }
            catch (IllegalStateException ex)
            {
                System.err.println(ex.toString());
            }
            catch (BundleException ex)
            {
                if (ex.getNestedException() != null)
                {
                    System.err.println(ex.getNestedException().toString());
                }
                else
                {
                    System.err.println(ex.toString());
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
            if (bundle != null)
            {
                if (sb.length() > 0)
                {
                    sb.append(", ");
                }
                sb.append(bundle.getBundleId());
            }
        }
        if (sb.toString().indexOf(',') > 0)
        {
            System.out.println("Bundle IDs: " + sb.toString());
        }
        else if (sb.length() > 0)
        {
            System.out.println("Bundle ID: " + sb.toString());
        }
    }

    @Descriptor("list all installed bundles")
    public void lb(
        @Descriptor("show location")
        @Parameter(names={ "-l", "--location" }, presentValue="true",
            absentValue="false") boolean showLoc,
        @Descriptor("show symbolic name")
        @Parameter(names={ "-s", "--symbolicname" }, presentValue="true",
            absentValue="false") boolean showSymbolic,
        @Descriptor("show update location")
        @Parameter(names={ "-u", "--updatelocation" }, presentValue="true",
            absentValue="false") boolean showUpdate)
    {
        lb(showLoc, showSymbolic, showUpdate, null);
    }

    @Descriptor("list installed bundles matching a substring")
    public void lb(
        @Descriptor("show location")
        @Parameter(names={ "-l", "--location" }, presentValue="true",
            absentValue="false") boolean showLoc,
        @Descriptor("show symbolic name")
        @Parameter(names={ "-s", "--symbolicname" }, presentValue="true",
            absentValue="false") boolean showSymbolic,
        @Descriptor("show update location")
        @Parameter(names={ "-u", "--updatelocation" }, presentValue="true",
            absentValue="false") boolean showUpdate,
        @Descriptor("subtring matched against name or symbolic name") String pattern)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get start level service.
        StartLevel sl = Util.getService(m_bc, StartLevel.class, refs);
        if (sl == null)
        {
            System.out.println("Start Level service is unavailable.");
        }

        List<Bundle> found = new ArrayList();

        if (pattern == null)
        {
            found.addAll(Arrays.asList(m_bc.getBundles()));
        }
        else
        {
            Bundle[] bundles = m_bc.getBundles();

            for (int i = 0; i < bundles.length; i++)
            {
                Bundle bundle = bundles[i];
                String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
                if (matchBundleName(bundle.getSymbolicName(), pattern)
                    || matchBundleName(name, pattern))
                {
                    found.add(bundle);
                }
            }
        }

        if (found.size() > 0)
        {
            printBundleList(
                (Bundle[]) found.toArray(new Bundle[found.size()]), sl,
                showLoc, showSymbolic, showUpdate);
        }
        else
        {
            System.out.println("No matching bundles found");
        }

        Util.ungetServices(m_bc, refs);
    }

    private boolean matchBundleName(String name, String pattern)
    {
        return (name != null) && name.toLowerCase().contains(pattern.toLowerCase());
    }

    @Descriptor("display all matching log entries")
    public void log(
        @Descriptor("minimum log level [ debug | info | warn | error ]")
            String logLevel)
    {
        log(-1, logLevel);
    }

    @Descriptor("display some matching log entries")
    public void log(
        @Descriptor("maximum number of entries") int maxEntries,
        @Descriptor("minimum log level [ debug | info | warn | error ]")
            String logLevel)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get start level service.
        LogReaderService lrs = Util.getService(m_bc, LogReaderService.class, refs);
        if (lrs == null)
        {
            System.out.println("Log reader service is unavailable.");
        }
        else
        {
            Enumeration entries = lrs.getLog();

            int minLevel = logLevelAsInt(logLevel);

            int index = 0;
            while (entries.hasMoreElements()
                && (maxEntries < 0 || index < maxEntries))
            {
                LogEntry entry = (LogEntry) entries.nextElement();
                if (entry.getLevel() <= minLevel)
                {
                    display(entry);
                    index++;
                }
            }

            Util.ungetServices(m_bc, refs);
        }
    }

    private void display(LogEntry entry)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        StringBuffer buffer = new StringBuffer();
        buffer.append(sdf.format(new Date(entry.getTime()))).append(" ");
        buffer.append(logLevelAsString(entry.getLevel())).append(" - ");
        buffer.append("Bundle: ").append(entry.getBundle().getSymbolicName());
        if (entry.getServiceReference() != null)
        {
            buffer.append(" - ");
            buffer.append(entry.getServiceReference().toString());
        }
        buffer.append(" - ").append(entry.getMessage());
        if (entry.getException() != null)
        {
            buffer.append(" - ");
            StringWriter writer = new StringWriter();
            PrintWriter  pw = new PrintWriter(writer);
            entry.getException().printStackTrace(pw);
            buffer.append(writer.toString());
        }

        System.out.println(buffer.toString());
    }

    private static int logLevelAsInt(String logLevel)
    {
        if ("error".equalsIgnoreCase(logLevel))
        {
            return LogService.LOG_ERROR;
        }
        else if ("warn".equalsIgnoreCase(logLevel))
        {
            return LogService.LOG_WARNING;
        }
        else if ("info".equalsIgnoreCase(logLevel))
        {
            return LogService.LOG_INFO;
        }
        return LogService.LOG_DEBUG;
    }

    private static String logLevelAsString(int level)
    {
        switch (level)
        {
            case LogService.LOG_ERROR:
                return "ERROR";
            case LogService.LOG_WARNING:
                return "WARNING";
            case LogService.LOG_INFO:
                return "INFO";
            default:
                return "DEBUG";
        }
    }

    @Descriptor("refresh bundles")
    public void refresh(
        @Descriptor("target bundles (can be null or empty)") Bundle[] bundles)
    {
        if ((bundles != null) && (bundles.length == 0))
        {
            bundles = null;
        }

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(m_bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("Package Admin service is unavailable.");
        }

        pa.refreshPackages((bundles == null) ? null : bundles);

        Util.ungetServices(m_bc, refs);
    }

    @Descriptor("resolve bundles")
    public void resolve(
        @Descriptor("target bundles (can be null or empty)") Bundle[] bundles)
    {
        if ((bundles != null) && (bundles.length == 0))
        {
            bundles = null;
        }

        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(m_bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("Package Admin service is unavailable.");
        }

        pa.resolveBundles((bundles == null) ? null : bundles);

        Util.ungetServices(m_bc, refs);
    }

    @Descriptor("start bundles")
    public void start(
        @Descriptor("start bundle transiently")
        @Parameter(names={ "-t", "--transient" }, presentValue="true",
            absentValue="false") boolean trans,
        @Descriptor("use declared activation policy")
        @Parameter(names={ "-p", "--policy" }, presentValue="true",
            absentValue="false") boolean policy,
        @Descriptor("target bundle identifiers or URLs") String[] ss)
    {
        int options = 0;

        // Check for "transient" switch.
        if (trans)
        {
            options |= Bundle.START_TRANSIENT;
        }

        // Check for "start policy" switch.
        if (policy)
        {
            options |= Bundle.START_ACTIVATION_POLICY;
        }

        // There should be at least one bundle id.
        if ((ss != null) && (ss.length >= 1))
        {
            for (String s : ss)
            {
                String id = s.trim();

                try
                {
                    Bundle bundle = null;

                    // The id may be a number or a URL, so check.
                    if (Character.isDigit(id.charAt(0)))
                    {
                        long l = Long.parseLong(id);
                        bundle = m_bc.getBundle(l);
                    }
                    else
                    {
                        bundle = m_bc.installBundle(id);
                    }

                    if (bundle != null)
                    {
                        bundle.start(options);
                    }
                    else
                    {
                        System.err.println("Bundle ID " + id + " is invalid.");
                    }
                }
                catch (NumberFormatException ex)
                {
                    System.err.println("Unable to parse id '" + id + "'.");
                }
                catch (BundleException ex)
                {
                    if (ex.getNestedException() != null)
                    {
                        ex.printStackTrace();
                        System.err.println(ex.getNestedException().toString());
                    }
                    else
                    {
                        System.err.println(ex.toString());
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
        }
        else
        {
            System.err.println("Incorrect number of arguments");
        }
    }

    @Descriptor("stop bundles")
    public void stop(
        @Descriptor("stop bundle transiently")
        @Parameter(names={ "-t", "--transient" }, presentValue="true",
            absentValue="false") boolean trans,
        @Descriptor("target bundles") Bundle[] bundles)
    {
        if ((bundles == null) || (bundles.length == 0))
        {
            System.out.println("Please specify the bundles to stop.");
        }

        int options = 0;

        // Check for "transient" switch.
        if (trans)
        {
            options |= Bundle.STOP_TRANSIENT;
        }

        for (Bundle bundle : bundles)
        {
            try
            {
                bundle.stop(options);
            }
            catch (BundleException ex)
            {
                if (ex.getNestedException() != null)
                {
                    System.err.println(ex.getNestedException().toString());
                }
                else
                {
                    System.err.println(ex.toString());
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

    @Descriptor("uninstall bundles")
    public void uninstall(
        @Descriptor("target bundles") Bundle[] bundles)
    {
        if ((bundles == null) || (bundles.length == 0))
        {
            System.out.println("Please specify the bundles to uninstall.");
        }
        else
        {
            try
            {
                for (Bundle bundle : bundles)
                {
                    bundle.uninstall();
                }
            }
            catch (BundleException ex)
            {
                if (ex.getNestedException() != null)
                {
                    ex.printStackTrace();
                    System.err.println(ex.getNestedException().toString());
                }
                else
                {
                    System.err.println(ex.toString());
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

    @Descriptor("update bundle")
    public void update(
        @Descriptor("target bundle") Bundle bundle)
    {
        try
        {
            // Get the bundle.
            if (bundle != null)
            {
                bundle.update();
            }
        }
        catch (BundleException ex)
        {
            if (ex.getNestedException() != null)
            {
                System.err.println(ex.getNestedException().toString());
            }
            else
            {
                System.err.println(ex.toString());
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.toString());
        }
    }

    @Descriptor("update bundle from URL")
    public void update(
        @Descriptor("target bundle") Bundle bundle,
        @Descriptor("URL from where to retrieve bundle") String location)
    {
        if (location != null)
        {
            try
            {
                // Get the bundle.
                if (bundle != null)
                {
                    InputStream is = new URL(location).openStream();
                    bundle.update(is);
                }
                else
                {
                    System.err.println("Please specify a bundle to update");
                }
            }
            catch (MalformedURLException ex)
            {
                System.err.println("Unable to parse URL");
            }
            catch (IOException ex)
            {
                System.err.println("Unable to open input stream: " + ex);
            }
            catch (BundleException ex)
            {
                if (ex.getNestedException() != null)
                {
                    System.err.println(ex.getNestedException().toString());
                }
                else
                {
                    System.err.println(ex.toString());
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
        else
        {
            System.err.println("Must specify a location.");
        }
    }

    @Descriptor("determines from where a bundle loads a class")
    public void which(
        @Descriptor("target bundle") Bundle bundle,
        @Descriptor("target class name") String className)
    {
        if (bundle == null)
        {
            System.err.println("Please specify a bundle");
        }
        else
        {
            Class clazz = null;
            try
            {
                clazz = bundle.loadClass(className);
                if (clazz.getClassLoader() == null)
                {
                    System.out.println("Loaded from: boot class loader");
                }
                else if (clazz.getClassLoader() instanceof BundleReference)
                {
                    Bundle p = ((BundleReference) clazz.getClassLoader()).getBundle();
                    System.out.println("Loaded from: " + p);
                }
                else
                {
                    System.out.println("Loaded from: " + clazz.getClassLoader());
                }
            }
            catch (ClassNotFoundException ex)
            {
                System.out.println("Class not found");
            }
        }
    }

    private static void printBundleList(
        Bundle[] bundles, StartLevel startLevel, boolean showLoc,
        boolean showSymbolic, boolean showUpdate)
    {
        // Display active start level.
        if (startLevel != null)
        {
            System.out.println("START LEVEL " + startLevel.getStartLevel());
        }

        // Determine last column.
        String lastColumn = "Name";
        if (showLoc)
        {
           lastColumn = "Location";
        }
        else if (showSymbolic)
        {
           lastColumn = "Symbolic name";
        }
        else if (showUpdate)
        {
           lastColumn = "Update location";
        }

        // Print column headers.
        if (startLevel != null)
        {
            System.out.println(
                String.format(
                "%5s|%-11s|%5s|%s", "ID", "State", "Level", lastColumn));
        }
        else
        {
            System.out.println(
                String.format(
                "%5s|%-11s|%s", "ID", "State", lastColumn));
        }
        for (Bundle bundle : bundles)
        {
            // Get the bundle name or location.
            String name = (String)
                bundle.getHeaders().get(Constants.BUNDLE_NAME);
            // If there is no name, then default to symbolic name.
            name = (name == null) ? bundle.getSymbolicName() : name;
            // If there is no symbolic name, resort to location.
            name = (name == null) ? bundle.getLocation() : name;

            // Overwrite the default value is the user specifically
            // requested to display one or the other.
            if (showLoc)
            {
                name = bundle.getLocation();
            }
            else if (showSymbolic)
            {
                name = bundle.getSymbolicName();
                name = (name == null)
                    ? "<no symbolic name>" : name;
            }
            else if (showUpdate)
            {
                name = (String)
                    bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
                name = (name == null)
                    ? bundle.getLocation() : name;
            }

            // Show bundle version if not showing location.
            name = (!showLoc && !showUpdate)
                ? name + " (" + bundle.getVersion() + ")" : name;

            // Get the bundle's start level.
            int level = (startLevel == null)
                ? -1
                : startLevel.getBundleStartLevel(bundle);

            if (level < 0)
            {
                System.out.println(
                    String.format(
                        "%5d|%-11s|%s",
                        bundle.getBundleId(), getStateString(bundle),
                        name, bundle.getVersion()));
            }
            else
            {
                System.out.println(
                    String.format(
                        "%5d|%-11s|%5d|%s",
                        bundle.getBundleId(), getStateString(bundle),
                        level, name, bundle.getVersion()));
            }
        }
    }

    private static String getStateString(Bundle bundle)
    {
        int state = bundle.getState();
        if (state == Bundle.ACTIVE)
        {
            return "Active     ";
        }
        else if (state == Bundle.INSTALLED)
        {
            return "Installed  ";
        }
        else if (state == Bundle.RESOLVED)
        {
            return "Resolved   ";
        }
        else if (state == Bundle.STARTING)
        {
            return "Starting   ";
        }
        else if (state == Bundle.STOPPING)
        {
            return "Stopping   ";
        }
        else
        {
            return "Unknown    ";
        }
    }
}