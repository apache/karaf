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
package org.apache.felix.fileinstall.internal;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

public class Util
{
    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";

    private static final String CHECKSUM_SUFFIX = ".checksum";

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     */
    public static void performSubstitution(Dictionary properties)
    {
        for (Enumeration e = properties.keys(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            Object value = properties.get(name);
            properties.put(name,
                value instanceof String
                    ? Util.substVars((String) value, name, null, properties)
                    : value);
        }
    }

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     **/
    public static String substVars(String val, String currentKey, Map cycleMap, Dictionary configProps)
        throws IllegalArgumentException
    {
        if (cycleMap == null)
        {
            cycleMap = new HashMap();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = val.indexOf(DELIM_STOP);

        // Find the matching starting "${" variable delimiter
        // by looping until we find a start delimiter that is
        // greater than the stop delimiter we have found.
        int startDelim = val.indexOf(DELIM_START);
        while (stopDelim >= 0)
        {
            int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim))
            {
                break;
            }
            else if (idx < stopDelim)
            {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) || (stopDelim < 0))
        {
            return val;
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable = val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException("recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (String) ((configProps != null) ? configProps.get(variable) : null);
        if (substValue == null)
        {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim) + substValue + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }

    /**
     * Log a message and optional throwable. If there is a log service we use
     * it, otherwise we log to the console
     *
     * @param message
     *            The message to log
     * @param e
     *            The throwable to log
     */
    public static void log(BundleContext context, long debug, String message, Throwable e)
    {
        getLogger(context).log(debug > 0, message, e);
    }

    private static Logger getLogger(BundleContext context)
    {
        try
        {
            context.getBundle();
            if (logger != null && logger.isValidLogger(context))
            {
                return logger;
            }
            logger = new OsgiLogger(context);
        }
        catch (Throwable t)
        {
            logger = new StdOutLogger();
        }
        return logger;
    }

    private static Logger logger;

    interface Logger
    {
        boolean isValidLogger(BundleContext context);
        void log(boolean debug, java.lang.String message, java.lang.Throwable throwable);
    }

    static class StdOutLogger implements Logger
    {
        public boolean isValidLogger(BundleContext context)
        {
            return true;
        }
        public void log(boolean debug, String message, Throwable throwable)
        {
            System.out.println(message + (throwable == null ? "" : ": " + throwable));
            if (debug && throwable != null)
            {
                throwable.printStackTrace(System.out);
            }
        }
    }

    static class OsgiLogger extends StdOutLogger
    {

        private BundleContext context;

        OsgiLogger(BundleContext context)
        {
            this.context = context;
            // Now make sure we can access the LogService class
            try
            {
                getClass().getClassLoader().loadClass(LogService.class.getName());
            }
            catch (ClassNotFoundException e)
            {
                throw new NoClassDefFoundError(e.getMessage());
            }
        }

        public boolean isValidLogger(BundleContext context)
        {
            return context == this.context;
        }

        public void log(boolean debug, String message, Throwable throwable)
        {
            LogService log = getLogService();
            if (log != null)
            {
                if (throwable != null)
                {
                    log.log(LogService.LOG_ERROR, message, throwable);
                    if (debug)
                    {
                        throwable.printStackTrace();
                    }
                }
                else
                {
                    log.log(LogService.LOG_INFO, message);
                }
            }
            else
            {
                super.log(debug, message, throwable);
            }
        }

        private LogService getLogService()
        {
            ServiceReference ref = context.getServiceReference(LogService.class.getName());
            if (ref != null)
            {
                LogService log = (LogService) context.getService(ref);
                return log;
            }
            return null;
        }
    }


    /**
     * Jar up a directory
     *
     * @param directory
     * @param zipName
     * @throws IOException
     */
    public static void jarDir(File directory, File zipName) throws IOException
    {
        jarDir(directory, new BufferedOutputStream(new FileOutputStream(zipName)));
    }

    public static void jarDir(File directory, OutputStream os) throws IOException
    {
        // create a ZipOutputStream to zip the data to
        JarOutputStream zos = new JarOutputStream(os);
        String path = "";
        File manFile = new File(directory, JarFile.MANIFEST_NAME);
        if (manFile.exists())
        {
            byte[] readBuffer = new byte[8192];
            FileInputStream fis = new FileInputStream(manFile);
            try
            {
                ZipEntry anEntry = new ZipEntry(JarFile.MANIFEST_NAME);
                zos.putNextEntry(anEntry);
                int bytesIn = fis.read(readBuffer);
                while (bytesIn != -1)
                {
                    zos.write(readBuffer, 0, bytesIn);
                    bytesIn = fis.read(readBuffer);
                }
            }
            finally
            {
                fis.close();
            }
            zos.closeEntry();
        }
        zipDir(directory, zos, path, Collections.singleton(JarFile.MANIFEST_NAME));
        // close the stream
        zos.close();
    }

    /**
     * Zip up a directory path
     * @param directory
     * @param zos
     * @param path
     * @param exclusions
     * @throws IOException
     */
    public static void zipDir(File directory, ZipOutputStream zos, String path, Set/* <String> */ exclusions) throws IOException
    {
        // get a listing of the directory content
        File[] dirList = directory.listFiles();
        byte[] readBuffer = new byte[8192];
        int bytesIn = 0;
        // loop through dirList, and zip the files
        for (int i = 0; i < dirList.length; i++)
        {
            File f = dirList[i];
            if (f.isDirectory())
            {
                zipDir(f, zos, path + f.getName() + "/", exclusions);
                continue;
            }
            String entry = path + f.getName();
            if (!exclusions.contains(entry))
            {
                FileInputStream fis = new FileInputStream(f);
                try
                {
                    ZipEntry anEntry = new ZipEntry(entry);
                    zos.putNextEntry(anEntry);
                    bytesIn = fis.read(readBuffer);
                    while (bytesIn != -1)
                    {
                        zos.write(readBuffer, 0, bytesIn);
                        bytesIn = fis.read(readBuffer);
                    }
                }
                finally
                {
                    fis.close();
                }
            }
        }
    }

    /**
     * Stores the checksum into a bundle data file.
     * @param b The bundle whose checksum must be stored
     * @param checksum the lastModified date to be stored in bc
     * @param bc the FileInstall's bundle context where to store the checksum.
     */
    public static void storeChecksum( Bundle b, long checksum, BundleContext bc )
    {
        String key = getBundleKey(b);
        File f = bc.getDataFile( key + CHECKSUM_SUFFIX );
        DataOutputStream dout = null;
        try
        {
            dout = new DataOutputStream( new FileOutputStream( f ) );
            dout.writeLong( checksum );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            if ( dout != null )
            {
                try
                {
                    dout.close();
                }
                catch ( IOException ignored )
                {
                }
            }
        }
    }

    /**
     * Returns the stored checksum of the bundle.
     * @param b the bundle whose checksum must be returned
     * @param bc the FileInstall's bundle context.
     * @return the stored checksum of the bundle
     */
    public static long loadChecksum( Bundle b, BundleContext bc )
    {
        String key = getBundleKey(b);
        File f = bc.getDataFile( key + CHECKSUM_SUFFIX );
        DataInputStream in = null;
        try
        {
            in = new DataInputStream( new FileInputStream( f ) );
            return in.readLong();
        }
        catch ( Exception e )
        {
            return Long.MIN_VALUE;
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( IOException e )
                {
                }
            }
        }
    }

    private static String getBundleKey(Bundle b)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(b.getSymbolicName()).append("_");
        String version = (String) b.getHeaders().get(Constants.BUNDLE_VERSION);
        sb.append(version != null ? version : Version.emptyVersion.toString());
        return sb.toString();
    }

}
