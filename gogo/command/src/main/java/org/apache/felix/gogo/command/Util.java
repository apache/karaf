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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

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
    private final static StringBuffer m_sb = new StringBuffer();

    public static String getUnderlineString(int len)
    {
        synchronized (m_sb)
        {
            m_sb.delete(0, m_sb.length());
            for (int i = 0; i < len; i++)
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

    public static <T> T getService(
        BundleContext bc, Class<T> clazz, List<ServiceReference> refs)
    {
        ServiceReference ref = bc.getServiceReference(clazz.getName());
        if (ref == null)
        {
            return null;
        }
        T t = (T) bc.getService(ref);
        if (t != null)
        {
            refs.add(ref);
        }
        return t;
    }

    public static void ungetServices(BundleContext bc, List<ServiceReference> refs)
    {
        while (refs.size() > 0)
        {
            bc.ungetService(refs.remove(0));
        }
    }

    public static void downloadSource(
        PrintStream out, PrintStream err,
        URL srcURL, File localDir, boolean extract)
    {
        // Get the file name from the URL.
        String fileName = (srcURL.getFile().lastIndexOf('/') > 0)
            ? srcURL.getFile().substring(srcURL.getFile().lastIndexOf('/') + 1)
            : srcURL.getFile();

        try
        {
            out.println("Connecting...");

            if (!localDir.exists())
            {
                err.println("Destination directory does not exist.");
            }
            File file = new File(localDir, fileName);

            OutputStream os = new FileOutputStream(file);
            URLConnection conn = srcURL.openConnection();
            Util.setProxyAuth(conn);
            int total = conn.getContentLength();
            InputStream is = conn.getInputStream();

            if (total > 0)
            {
                out.println("Downloading " + fileName
                    + " ( " + total + " bytes ).");
            }
            else
            {
                out.println("Downloading " + fileName + ".");
            }
            byte[] buffer = new byte[4096];
            int count = 0;
            for (int len = is.read(buffer); len > 0; len = is.read(buffer))
            {
                count += len;
                os.write(buffer, 0, len);
            }

            os.close();
            is.close();

            if (extract)
            {
                is = new FileInputStream(file);
                JarInputStream jis = new JarInputStream(is);
                out.println("Extracting...");
                unjar(jis, localDir);
                jis.close();
                file.delete();
            }
        }
        catch (Exception ex)
        {
            err.println(ex);
        }
    }

    public static void unjar(JarInputStream jis, File dir)
        throws IOException
    {
        // Reusable buffer.
        byte[] buffer = new byte[4096];

        // Loop through JAR entries.
        for (JarEntry je = jis.getNextJarEntry();
             je != null;
             je = jis.getNextJarEntry())
        {
            if (je.getName().startsWith("/"))
            {
                throw new IOException("JAR resource cannot contain absolute paths.");
            }

            File target = new File(dir, je.getName());

            // Check to see if the JAR entry is a directory.
            if (je.isDirectory())
            {
                if (!target.exists())
                {
                    if (!target.mkdirs())
                    {
                        throw new IOException("Unable to create target directory: "
                            + target);
                    }
                }
                // Just continue since directories do not have content to copy.
                continue;
            }

            int lastIndex = je.getName().lastIndexOf('/');
            String name = (lastIndex >= 0) ?
                je.getName().substring(lastIndex + 1) : je.getName();
            String destination = (lastIndex >= 0) ?
                je.getName().substring(0, lastIndex) : "";

            // JAR files use '/', so convert it to platform separator.
            destination = destination.replace('/', File.separatorChar);
            copy(jis, dir, name, destination, buffer);
        }
    }

    public static void copy(
        InputStream is, File dir, String destName, String destDir, byte[] buffer)
        throws IOException
    {
        if (destDir == null)
        {
            destDir = "";
        }

        // Make sure the target directory exists and
        // that is actually a directory.
        File targetDir = new File(dir, destDir);
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new IOException("Unable to create target directory: "
                    + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new IOException("Target is not a directory: "
                + targetDir);
        }

        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(targetDir, destName)));
        int count = 0;
        while ((count = is.read(buffer)) > 0)
        {
            bos.write(buffer, 0, count);
        }
        bos.close();
    }

    public static void setProxyAuth(URLConnection conn) throws IOException
    {
        // Support for http proxy authentication
        String auth = System.getProperty("http.proxyAuth");
        if ((auth != null) && (auth.length() > 0))
        {
            if ("http".equals(conn.getURL().getProtocol())
                || "https".equals(conn.getURL().getProtocol()))
            {
                String base64 = Base64Encoder.base64Encode(auth);
                conn.setRequestProperty("Proxy-Authorization", "Basic " + base64);
            }
        }
    }

    public static InputStream openURL(final URL url) throws IOException
    {
        // Do it the manual way to have a chance to
        // set request properties as proxy auth (EW).
        return openURL(url.openConnection());
    }

    public static InputStream openURL(final URLConnection conn) throws IOException
    {
        // Do it the manual way to have a chance to
        // set request properties as proxy auth (EW).
        setProxyAuth(conn);
        return conn.getInputStream();
    }
}