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
package org.apache.felix.gogo.shell;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Formatter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;
import org.osgi.service.startlevel.StartLevel;

public class Converters implements Converter
{
    private final BundleContext context;
    public Converters(BundleContext context)
    {
        this.context = context;
    }

    private CharSequence print(Bundle bundle)
    {
        // [ ID ] [STATE      ] [ SL ] symname
        StartLevel sl = null;
        ServiceReference ref = context.getServiceReference(StartLevel.class.getName());
        if (ref != null)
        {
            sl = (StartLevel) context.getService(ref);
        }

        if (sl == null)
        {
            return String.format("%5d|%-11s|%s (%s)", bundle.getBundleId(),
                getState(bundle), bundle.getSymbolicName(), bundle.getVersion());
        }

        int level = sl.getBundleStartLevel(bundle);
        context.ungetService(ref);

        return String.format("%5d|%-11s|%5d|%s (%s)", bundle.getBundleId(),
            getState(bundle), level, bundle.getSymbolicName(), bundle.getVersion());
    }

    private CharSequence print(ServiceReference ref)
    {
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);

        String spid = "";
        Object pid = ref.getProperty("service.pid");
        if (pid != null)
        {
            spid = pid.toString();
        }

        f.format("%06d %3s %-40s %s", ref.getProperty("service.id"),
            ref.getBundle().getBundleId(),
            getShortNames((String[]) ref.getProperty("objectclass")), spid);
        return sb;
    }

    private CharSequence getShortNames(String[] list)
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

    private CharSequence getShortName(String name)
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
                return "Active";

            case Bundle.INSTALLED:
                return "Installed";

            case Bundle.RESOLVED:
                return "Resolved";

            case Bundle.STARTING:
                return "Starting";

            case Bundle.STOPPING:
                return "Stopping";

            case Bundle.UNINSTALLED:
                return "Uninstalled ";
        }
        return null;
    }

    public Bundle bundle(Bundle i)
    {
        return i;
    }

    public Object convert(Class<?> desiredType, final Object in) throws Exception
    {
        if (desiredType == Bundle.class)
        {
            return convertBundle(in);
        }

        if (desiredType == ServiceReference.class)
        {
            return convertServiceReference(in);
        }

        if (desiredType == Class.class)
        {
            try
            {
                return Class.forName(in.toString());
            }
            catch (ClassNotFoundException e)
            {
                return null;
            }
        }

        if (desiredType.isAssignableFrom(String.class) && in instanceof InputStream)
        {
            return read(((InputStream) in));
        }

        if (in instanceof Function && desiredType.isInterface()
            && desiredType.getDeclaredMethods().length == 1)
        {
            return Proxy.newProxyInstance(desiredType.getClassLoader(),
                new Class[] { desiredType }, new InvocationHandler()
                {
                    Function command = ((Function) in);

                    public Object invoke(Object proxy, Method method, Object[] args)
                        throws Throwable
                    {
                        return command.execute(null, Arrays.asList(args));
                    }
                });
        }

        return null;
    }

    private Object convertServiceReference(Object in) throws InvalidSyntaxException
    {
        String s = in.toString();
        if (s.startsWith("(") && s.endsWith(")"))
        {
            ServiceReference refs[] = context.getServiceReferences(null, String.format(
                "(|(service.id=%s)(service.pid=%s))", in, in));
            if (refs != null && refs.length > 0)
            {
                return refs[0];
            }
        }

        ServiceReference refs[] = context.getServiceReferences(null, String.format(
            "(|(service.id=%s)(service.pid=%s))", in, in));
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
            return context.getBundle(id);
        }
        catch (NumberFormatException nfe)
        {
            // Ignore
        }

        Bundle bundles[] = context.getBundles();
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

    public CharSequence format(Object target, int level, Converter converter)
        throws IOException
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

    private CharSequence read(InputStream in) throws IOException
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

}
