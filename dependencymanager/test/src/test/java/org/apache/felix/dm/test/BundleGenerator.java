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
package org.apache.felix.dm.test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

import aQute.bnd.main.bnd;

/**
 * Helper used to generate a bundle dynamically.
 */
public class BundleGenerator
{
    /**
     * The Bnd directives
     */
    private Properties _directives = new Properties();

    /**
     * The class loader used to get the classpath where to collect the bundle classes
     */
    private ClassLoader _classLoader = BundleGenerator.class.getClassLoader();

    /**
     * Sets a Bnd directive
     * @param attributeName the directive name (can start with a "-")
     * @param attributeValue the directive value
     * @return ourself
     */
    public BundleGenerator set(String attributeName, String attributeValue)
    {
        _directives.put(attributeName, attributeValue);
        return this;
    }

    /**
     * Sets a specific class loader, which will be used to get the classpath where to collect
     * the bundle classes. By default, the class loader used is the BundleGenerator class loader.
     * @param cl the class loader that will be used to get the bundle classpath
     * @return ourself
     */
    public BundleGenerator setClassLoader(ClassLoader cl)
    {
        _classLoader = cl;
        return this;
    }

    /**
     * Generates the output bundle
     * @return the bundle URL, as a String
     */
    public String build()
    {
        try
        {
            // Check if the system tmp dir exists
            
            File tmp = new File(System.getProperty("java.io.tmpdir"));
            if (tmp != null && ! tmp.exists()) {
                tmp.mkdirs();
            }
            
            // Deduce the classpath from Export-Package, or Private-Package headers.

            String pkg = _directives.getProperty("Export-Package");
            if (pkg == null)
            {
                pkg = _directives.getProperty("Private-Package");
            }
            if (pkg == null)
            {
                throw new IllegalArgumentException(
                    "You must either specify a Private-Package, or an Export-Package directive");
            }

            String classpath = getClassPath(pkg);

            // Create the bundle output file

            File output = File.createTempFile("DMTestBundle", ".jar");
            output.deleteOnExit();

            // Create the bnd directive file

            File directives = File.createTempFile("DMTestBnd", ".bnd");
            directives.deleteOnExit();

            // Write the directives into the bnd file.

            PrintWriter pw = new PrintWriter(new FileWriter(directives));
            try
            {
                for (Object key : _directives.keySet())
                {
                    Object value = _directives.get(key);
                    if (key.toString().startsWith("-"))
                    {
                        pw.println(key.toString() + " " + value.toString());
                    }
                    else
                    {
                        pw.println(key.toString() + ": " + value.toString());
                    }
                }
            }

            finally
            {
                pw.close();
            }

            // Launch Bnd in order to generate our tiny test bundle.

            bnd bnd = new bnd();
            bnd.main(new String[] { "-failok", "build", "-noeclipse", "-classpath", classpath,
                    "-output", output.getPath(), directives.getPath() });

            // Return the URL of the generated bundle, as a String.

            return output.toURI().toURL().toString();
        }

        catch (RuntimeException e)
        {
            throw e;
        }

        catch (Throwable t)
        {
            throw new RuntimeException("Unexpected exception while generating DM test bundle", t);
        }
    }

    /**
     * Gets the bundle classpath, where the bundle classes will be collected.
     * @param pkg either an exported package, or a private package.
     * @return the bundle classpath
     */
    private String getClassPath(String pkg)
    {
        String pkgPath = pkg.replaceAll("\\.", "/");
        URL pkgURL = _classLoader.getResource(pkgPath);
        if (pkgURL == null)
        {
            throw new IllegalArgumentException("Could not find package " + pkg + " in classpath");
        }
        int i = pkgURL.getPath().indexOf(pkgPath);
        return i == -1 ? pkgURL.getPath() : pkgURL.getPath().substring(0, i);
    }
}
