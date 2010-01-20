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
package org.apache.felix.framework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import junit.framework.TestCase;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

/**
 * Test to ensure behaviour of "felix.bootdelegation.classloaders" contract.
 * 
 */
public class BootLoaderTest extends TestCase
{
    private File cacheDir;

    public void testCanProvideOwnClassLoader() throws Exception
    {
        final ClassLoader myClassloader = new CL();
        final List queriedFor = new ArrayList();

        Map bundle2Classloader = new HashMap()
        {
            public Object get(Object o)
            {
                queriedFor.add(o);
                return myClassloader;
            }
        };

        Map params = new HashMap();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0,"
            + "org.osgi.service.packageadmin; version=1.2.0,"
            + "org.osgi.service.startlevel; version=1.1.0,"
            + "org.osgi.util.tracker; version=1.3.3,"
            + "org.osgi.service.url; version=1.0.0");
        params.put(FelixConstants.BOOT_CLASSLOADERS_PROP, bundle2Classloader);
        cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);

        String mf = "Bundle-SymbolicName: boot.test\n"
            + "Bundle-Version: 1.1.0\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: boot.test";
        File bundle = createBundle(mf);

        Framework f = new Felix(params);
        f.init();
        f.getBundleContext().installBundle(bundle.toURI().toString());
        f.start();

        Bundle[] arr = f.getBundleContext().getBundles();
        assertEquals("Two, system and mine: " + Arrays.toString(arr), 2, arr.length);
        Class c = arr[1].loadClass("boot.test.Test");
        assertNotNull("Class loaded", c);
        assertEquals("One query", 1, queriedFor.size());
        assertEquals("Queried for my bundle", arr[1], queriedFor.get(0));
    }

    public static final class CL extends ClassLoader
    {
        protected Class findClass(String name) throws ClassNotFoundException
        {
            if (name.equals("boot.test.Test"))
            {
                return String.class;
            }
            throw new ClassNotFoundException();
        }
    }

    private static File createBundle(String manifest) throws IOException
    {
        File f = File.createTempFile("felix-bundle", ".jar");
        f.deleteOnExit();

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);
        os.close();

        return f;
    }
}