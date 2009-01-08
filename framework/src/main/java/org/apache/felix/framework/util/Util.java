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
package org.apache.felix.framework.util;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class Util
{
    /**
     * Converts a module identifier to a bundle identifier. Module IDs
     * are typically <tt>&lt;bundle-id&gt;.&lt;revision&gt;</tt>; this
     * method returns only the portion corresponding to the bundle ID.
    **/
    public static long getBundleIdFromModuleId(String id)
    {
        try
        {
            String bundleId = (id.indexOf('.') >= 0)
                ? id.substring(0, id.indexOf('.')) : id;
            return Long.parseLong(bundleId);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    /**
     * Converts a module identifier to a bundle identifier. Module IDs
     * are typically <tt>&lt;bundle-id&gt;.&lt;revision&gt;</tt>; this
     * method returns only the portion corresponding to the revision.
    **/
    public static int getModuleRevisionFromModuleId(String id)
    {
        try
        {
            int index = id.indexOf('.');
            if (index >= 0)
            {
                return Integer.parseInt(id.substring(index + 1));
            }
        }
        catch (NumberFormatException ex)
        {
        }
        return -1;
    }

    public static String getClassName(String className)
    {
        if (className == null)
        {
            className = "";
        }
        return (className.lastIndexOf('.') < 0)
            ? "" : className.substring(className.lastIndexOf('.') + 1);
    }

    public static String getClassPackage(String className)
    {
        if (className == null)
        {
            className = "";
        }
        return (className.lastIndexOf('.') < 0)
            ? "" : className.substring(0, className.lastIndexOf('.'));
    }

    public static String getResourcePackage(String resource)
    {
        if (resource == null)
        {
            resource = "";
        }
        // NOTE: The package of a resource is tricky to determine since
        // resources do not follow the same naming conventions as classes.
        // This code is pessimistic and assumes that the package of a
        // resource is everything up to the last '/' character. By making
        // this choice, it will not be possible to load resources from
        // imports using relative resource names. For example, if a
        // bundle exports "foo" and an importer of "foo" tries to load
        // "/foo/bar/myresource.txt", this will not be found in the exporter
        // because the following algorithm assumes the package name is
        // "foo.bar", not just "foo". This only affects imported resources,
        // local resources will work as expected.
        String pkgName = (resource.startsWith("/")) ? resource.substring(1) : resource;
        pkgName = (pkgName.lastIndexOf('/') < 0)
            ? "" : pkgName.substring(0, pkgName.lastIndexOf('/'));
        pkgName = pkgName.replace('/', '.');
        return pkgName;
    }

    /**
     * <p>
     * This is a simple utility class that attempts to load the named
     * class using the class loader of the supplied class or
     * the class loader of one of its super classes or their implemented
     * interfaces. This is necessary during service registration to test
     * whether a given service object implements its declared service
     * interfaces.
     * </p>
     * <p>
     * To perform this test, the framework must try to load
     * the classes associated with the declared service interfaces, so
     * it must choose a class loader. The class loader of the registering
     * bundle cannot be used, since this disallows third parties to
     * register service on behalf of another bundle. Consequently, the
     * class loader of the service object must be used. However, this is
     * also not sufficient since the class loader of the service object
     * may not have direct access to the class in question.
     * </p>
     * <p>
     * The service object's class loader may not have direct access to
     * its service interface if it extends a super class from another
     * bundle which implements the service interface from an imported
     * bundle or if it implements an extension of the service interface
     * from another bundle which imports the base interface from another
     * bundle. In these cases, the service object's class loader only has
     * access to the super class's class or the extended service interface,
     * respectively, but not to the actual service interface.
     * </p>
     * <p>
     * Thus, it is necessary to not only try to load the service interface
     * class from the service object's class loader, but from the class
     * loaders of any interfaces it implements and the class loaders of
     * all super classes.
     * </p>
     * @param svcObj the class that is the root of the search.
     * @param name the name of the class to load.
     * @return the loaded class or <tt>null</tt> if it could not be
     *         loaded.
    **/
    public static Class loadClassUsingClass(Class clazz, String name)
    {
        Class loadedClass = null;

        while (clazz != null)
        {
            // Get the class loader of the current class object.
            ClassLoader loader = clazz.getClassLoader();
            // A null class loader represents the system class loader.
            loader = (loader == null) ? ClassLoader.getSystemClassLoader() : loader;
            try
            {
                return loader.loadClass(name);
            }
            catch (ClassNotFoundException ex)
            {
                // Ignore and try interface class loaders.
            }

            // Try to see if we can load the class from
            // one of the class's implemented interface
            // class loaders.
            Class[] ifcs = clazz.getInterfaces();
            for (int i = 0; i < ifcs.length; i++)
            {
                loadedClass = loadClassUsingClass(ifcs[i], name);
                if (loadedClass != null)
                {
                    return loadedClass;
                }
            }

            // Try to see if we can load the class from
            // the super class class loader.
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    /**
     * This method determines if the requesting bundle is able to cast
     * the specified service reference based on class visibility rules
     * of the underlying modules.
     * @param requester The bundle requesting the service.
     * @param ref The service in question.
     * @return <tt>true</tt> if the requesting bundle is able to case
     *         the service object to a known type.
    **/
    public static boolean isServiceAssignable(Bundle requester, ServiceReference ref)
    {
        // Boolean flag.
        boolean allow = true;
        // Get the service's objectClass property.
        String[] objectClass = (String[]) ref.getProperty(FelixConstants.OBJECTCLASS);

        // The the service reference is not assignable when the requesting
        // bundle is wired to a different version of the service object.
        // NOTE: We are pessimistic here, if any class in the service's
        // objectClass is not usable by the requesting bundle, then we
        // disallow the service reference.
        for (int classIdx = 0; (allow) && (classIdx < objectClass.length); classIdx++)
        {
            if (!ref.isAssignableTo(requester, objectClass[classIdx]))
            {
                allow = false;
            }
        }
        return allow;
    }

    public static ICapability getSatisfyingCapability(IModule m, IRequirement req)
    {
        ICapability[] caps = m.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.length); i++)
        {
            if (caps[i].getNamespace().equals(req.getNamespace()) &&
                req.isSatisfied(caps[i]))
            {
                return caps[i];
            }
        }
        return null;
    }

    /**
     * Returns all the capabilities from a module that has a specified namespace.
     *
     * @param module    module providing capabilities
     * @param namespace capability namespace
     * @return array of matching capabilities or empty if none found
     */
    public static ICapability[] getCapabilityByNamespace(IModule module, String namespace)
    {
        final List matching = new ArrayList();
        final ICapability[] caps = module.getCapabilities();
        for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
        {
            if (caps[capIdx].getNamespace().equals(namespace))
            {
                matching.add(caps[capIdx]);
            }
        }
        return (ICapability[]) matching.toArray(new ICapability[matching.size()]);
    }

    public static IWire getWire(IModule m, String name)
    {
        IWire[] wires = m.getWires();
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            if (wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE) &&
                ((Capability) wires[i].getCapability()).getPackageName().equals(name))
            {
                return wires[i];
            }
        }
        return null;
    }

    private static final byte encTab[] = { 0x41, 0x42, 0x43, 0x44, 0x45, 0x46,
        0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52,
        0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x61, 0x62, 0x63, 0x64,
        0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70,
        0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x30, 0x31,
        0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x2b, 0x2f };

    private static final byte decTab[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1,
        -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1,
        -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, -1, -1, -1, -1, -1 };

    public static String base64Encode(String s) throws IOException
    {
        return encode(s.getBytes(), 0);
    }

    /**
     * Encode a raw byte array to a Base64 String.
     *
     * @param in Byte array to encode.
     * @param len Length of Base64 lines. 0 means no line breaks.
    **/
    public static String encode(byte[] in, int len) throws IOException
    {
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try
        {
            baos = new ByteArrayOutputStream();
            bais = new ByteArrayInputStream(in);
            encode(bais, baos, len);
            // ASCII byte array to String
            return (new String(baos.toByteArray()));
        }
        finally
        {
            if (baos != null)
            {
                baos.close();
            }
            if (bais != null)
            {
                bais.close();
            }
        }
    }

    public static void encode(InputStream in, OutputStream out, int len)
        throws IOException
    {

        // Check that length is a multiple of 4 bytes
        if (len % 4 != 0)
        {
            throw new IllegalArgumentException("Length must be a multiple of 4");
        }

        // Read input stream until end of file
        int bits = 0;
        int nbits = 0;
        int nbytes = 0;
        int b;

        while ((b = in.read()) != -1)
        {
            bits = (bits << 8) | b;
            nbits += 8;
            while (nbits >= 6)
            {
                nbits -= 6;
                out.write(encTab[0x3f & (bits >> nbits)]);
                nbytes++;
                // New line
                if (len != 0 && nbytes >= len)
                {
                    out.write(0x0d);
                    out.write(0x0a);
                    nbytes -= len;
                }
            }
        }

        switch (nbits)
        {
            case 2:
                out.write(encTab[0x3f & (bits << 4)]);
                out.write(0x3d); // 0x3d = '='
                out.write(0x3d);
                break;
            case 4:
                out.write(encTab[0x3f & (bits << 2)]);
                out.write(0x3d);
                break;
        }

        if (len != 0)
        {
            if (nbytes != 0)
            {
                out.write(0x0d);
                out.write(0x0a);
            }
            out.write(0x0d);
            out.write(0x0a);
        }
    }


    private static final String DELIM_START = "${";
    private static final String DELIM_STOP  = "}";

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
    public static String substVars(String val, String currentKey,
        Map cycleMap, Properties configProps)
        throws IllegalArgumentException
    {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
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
        if ((startDelim < 0) && (stopDelim < 0))
        {
            return val;
        }
        // At this point, we found a stop delimiter without a start,
        // so throw an exception.
        else if (((startDelim < 0) || (startDelim > stopDelim))
            && (stopDelim >= 0))
        {
            throw new IllegalArgumentException(
                "stop delimiter with no start delimiter: "
                + val);
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
            val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException(
                "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
            ? configProps.getProperty(variable, null)
            : null;
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
        val = val.substring(0, startDelim)
            + substValue
            + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }

    /**
     * Checks if the provided module definition declares a fragment host.
     *
     * @param module the module to check
     * @return <code>true</code> if the module declares a fragment host, <code>false</code>
     *      otherwise.
     */
    public static boolean isFragment(IModule module)
    {
        Map headerMap = module.getHeaders();
        return headerMap.containsKey(Constants.FRAGMENT_HOST);
    }
}