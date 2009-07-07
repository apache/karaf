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
// DWB1: osgi:each too verbose (formats reults to System.out)
// DWB2: ClassNotFoundException should be caught in convert() method
package org.apache.felix.gogo.runtime.osgi;

import org.osgi.framework.*;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Converter;
import org.osgi.service.command.Function;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;

public class OSGiCommands implements Converter
{
    Bundle bundle;
    String COLUMN = "%40s %s\n";

    protected OSGiCommands(Bundle bundle)
    {
        this.bundle = bundle;
    }

//	Bundle[] getBundles() {
//		return getContext().getBundles();
//	}

    public BundleContext getContext()
    {
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING
                && bundle.getState() != Bundle.STOPPING)
        {
            throw new IllegalStateException("Framework is not started yet");
        }
        return bundle.getBundleContext();
    }

    CharSequence print(Bundle bundle)
    {
        String version = (String) bundle.getHeaders().get("Bundle-Version");
        if (version == null)
        {
            version = "0.0.0";
        }
        return String.format("%06d %s %s-%s", bundle.getBundleId(), getState(bundle), bundle.getSymbolicName(), version);
    }

    CharSequence print(ServiceReference ref)
    {
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);

        String spid = "";
        Object pid = ref.getProperty("service.pid");
        if (pid != null)
        {
            spid = pid.toString();
        }

        f.format("%06d %3s %-40s %s", ref.getProperty("service.id"), ref.getBundle().getBundleId(), getShortNames((String[]) ref.getProperty("objectclass")), spid);
        return sb;
    }

    CharSequence getShortNames(String[] list)
    {
        StringBuilder sb = new StringBuilder();
        String del = "";
        for (String s : list)
        {
            sb.append(del + getShortName(s));
            del = " | ";
        }
        return sb;
    }

    CharSequence getShortName(String name)
    {
        int n = name.lastIndexOf('.');
        if (n < 0)
        {
            n = 0;
        }
        else
        {
            n++;
        }
        return name.subSequence(n, name.length());
    }

    private String getState(Bundle bundle)
    {
        switch (bundle.getState())
        {
            case Bundle.ACTIVE:
                return "ACT";

            case Bundle.INSTALLED:
                return "INS";

            case Bundle.RESOLVED:
                return "RES";

            case Bundle.STARTING:
                return "STA";

            case Bundle.STOPPING:
                return "STO";

            case Bundle.UNINSTALLED:
                return "UNI ";
        }
        return null;
    }

    public void grep(String match) throws IOException
    {
        Pattern p = Pattern.compile(match);
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null)
        {
            if (p.matcher(s).find())
            {
                System.out.println(s);
            }
            s = rdr.readLine();
        }
    }

    public String tac() throws IOException
    {
        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        String s = rdr.readLine();
        while (s != null)
        {
            sw.write(s);
            s = rdr.readLine();
        }
        return sw.toString();
    }

    public CharSequence echo(CommandSession session, Object args[])
    {
        StringBuilder sb = new StringBuilder();
        String del = "";
        for (Object arg : args)
        {
            sb.append(del);
            if (arg != null)
            {
                sb.append(arg);
                del = " ";
            }
        }
        return sb;
    }

    public void each(CommandSession session, Collection<Object> list, Function closure) throws Exception
    {
        List<Object> args = new ArrayList<Object>();
        args.add(null);
        for (Object x : list)
        {
            args.set(0, x);
            //Object result = closure.execute(session, args);
            // System.out.println(session.format(result,Converter.INSPECT));
            // derek: this is way too noisy
            closure.execute(session, args);
        }
    }

    public Bundle bundle(Bundle i)
    {
        return i;
    }

    public String[] ls(CommandSession session, File f) throws Exception
    {
        File cwd = (File) session.get("_cwd");
        if (cwd == null)
        {
            cwd = new File("").getAbsoluteFile();
        }

        if (f == null)
        {
            f = cwd;
        }
        else
        {
            if (!f.isAbsolute())
            {
                f = new File(cwd, f.getPath());
            }
        }

        if (f.isDirectory())
        {
            return f.list();
        }

        if (f.isFile())
        {
            cat(session, f);
        }

        return null;
    }

    public Object cat(CommandSession session, File f) throws Exception
    {
        File cwd = (File) session.get("_cwd");
        if (cwd == null)
        {
            cwd = new File("").getAbsoluteFile();
        }

        if (!f.isAbsolute())
        {
            f = new File(cwd, f.getPath());
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        FileInputStream in = new FileInputStream(f);
        byte[] buffer = new byte[(int) (f.length() % 100000)];
        int size = in.read(buffer);
        while (size > 0)
        {
            bout.write(buffer, 0, size);
            size = in.read(buffer);
        }
        return new String(bout.toByteArray());
    }

    public Object convert(Class<?> desiredType, Object in) throws Exception
    {
        if (desiredType == Bundle.class)
        {
            return convertBundle(in);
        }
        else
        {
            if (desiredType == ServiceReference.class)
            {
                return convertServiceReference(in);
            }
            else
            {
                if (desiredType == Class.class)
                {
                    // derek.baum@paremus.com - added try/catch
                    try
                    {
                        return Class.forName(in.toString());
                    }
                    catch (ClassNotFoundException e)
                    {
                        return null;
                    }
                }
                else
                {
                    if (desiredType.isAssignableFrom(String.class) && in instanceof InputStream)
                    {
                        return read(((InputStream) in));
                    }
                }
            }
        }

        return null;
    }

    private Object convertServiceReference(Object in) throws InvalidSyntaxException
    {
        String s = in.toString();
        if (s.startsWith("(") && s.endsWith(")"))
        {
            ServiceReference refs[] = getContext().getServiceReferences(null, String.format("(|(service.id=%s)(service.pid=%s))", in, in));
            if (refs != null && refs.length > 0)
            {
                return refs[0];
            }
        }

        ServiceReference refs[] = getContext().getServiceReferences(null, String.format("(|(service.id=%s)(service.pid=%s))", in, in));
        if (refs != null && refs.length > 0)
        {
            return refs[0];
        }
        return null;
    }

    private Object convertBundle(Object in)
    {
        String s = in.toString();
        try
        {
            long id = Long.parseLong(s);
            return getContext().getBundle(id);
        }
        catch (NumberFormatException nfe)
        {
            // Ignore
        }

        Bundle bundles[] = getContext().getBundles();
        for (Bundle b : bundles)
        {
            if (b.getLocation().equals(s))
            {
                return b;
            }

            if (b.getSymbolicName().equals(s))
            {
                return b;
            }
        }

        return null;
    }

    public CharSequence format(Object target, int level, Converter converter) throws IOException
    {
        if (level == INSPECT && target instanceof InputStream)
        {
            return read(((InputStream) target));
        }
        if (level == LINE && target instanceof Bundle)
        {
            return print((Bundle) target);
        }
        if (level == LINE && target instanceof ServiceReference)
        {
            return print((ServiceReference) target);
        }
        if (level == PART && target instanceof Bundle)
        {
            return ((Bundle) target).getSymbolicName();
        }
        if (level == PART && target instanceof ServiceReference)
        {
            return getShortNames((String[]) ((ServiceReference) target).getProperty("objectclass"));
        }
        return null;
    }

    public CharSequence read(InputStream in) throws IOException
    {
        int c;
        StringBuffer sb = new StringBuffer();
        while ((c = in.read()) > 0)
        {
            if (c >= 32 && c <= 0x7F || c == '\n' || c == '\r')
            {
                sb.append((char) c);
            }
            else
            {
                String s = Integer.toHexString(c).toUpperCase();
                sb.append("\\");
                if (s.length() < 1)
                {
                    sb.append(0);
                }
                sb.append(s);
            }
        }
        return sb;
    }

    public void start(Bundle b) throws BundleException
    {
        b.start();
    }

    public void stop(Bundle b) throws BundleException
    {
        b.stop();
    }

    public Object service(String clazz, String filter) throws InvalidSyntaxException
    {
        ServiceReference ref[] = getContext().getServiceReferences(clazz, filter);
        if (ref == null)
        {
            return null;
        }

        return getContext().getService(ref[0]);
    }

}
