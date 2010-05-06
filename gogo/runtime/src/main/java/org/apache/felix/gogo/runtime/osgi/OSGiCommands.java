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
package org.apache.felix.gogo.runtime.osgi;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.apache.felix.gogo.runtime.shell.CommandProcessorImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.CommandSession;
import org.osgi.service.packageadmin.PackageAdmin;

public class OSGiCommands
{
    final BundleContext context;

    public OSGiCommands(BundleContext context)
    {
        this.context = context;
    }

    private Object service(String clazz, String filter) throws InvalidSyntaxException
    {
        ServiceReference ref[] = context.getServiceReferences(clazz, filter);
        if (ref != null)
        {
            return context.getService(ref[0]);
        }

        return null;
    }

    public void registerCommands(CommandProcessorImpl processor, Bundle bundle)
    {
        processor.addCommand("osgi", this);
        processor.addCommand("osgi", new Procedural());
        processor.addCommand("osgi", bundle);
        processor.addCommand("osgi", context, BundleContext.class);

        try
        {
            processor.addCommand("osgi",
                this.service(PackageAdmin.class.getName(), null), PackageAdmin.class);

            try
            {
                // dynamically load StartLevel to avoid import dependency
                String sl = "org.osgi.service.startlevel.StartLevel";
                Class<?> slClass = bundle.loadClass(sl);
                processor.addCommand("osgi", this.service(sl, null), slClass);
            }
            catch (ClassNotFoundException e)
            {
            }

            try
            {
                // dynamically load PermissionAdmin to avoid import dependency
                String pa = "org.osgi.service.permissionadmin.PermissionAdmin";
                Class<?> paClass = bundle.loadClass(pa);
                processor.addCommand("osgi", this.service(pa, null), paClass);
            }
            catch (ClassNotFoundException e)
            {
            }
        }
        catch (InvalidSyntaxException e)
        {
            // can't happen with null filter
        }
    }

    public Bundle bundle(Bundle i)
    {
        return i;
    }

    public void start(Bundle b) throws BundleException
    {
        b.start();
    }

    public void stop(Bundle b) throws BundleException
    {
        b.stop();
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

}
