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
package org.apache.felix.webconsole.internal.core;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * ServicesConfigurationPrinter provides a configuration printer for inspecting the 
 * registered services.
 */
public class ServicesConfigurationPrinter extends AbstractConfigurationPrinter implements Constants
{
    private static final String TITLE = "Services";

    private static final MessageFormat INFO = new MessageFormat(
        "Service {0} - {1} (pid: {2})");
    private static final MessageFormat FROM = new MessageFormat(
        "  from Bundle {0} - {1} ({2}), version {3}");
    private static final MessageFormat USING = new MessageFormat(
        "  Using Bundle {0} - {1} ({2}), version {3}");

    // don't create empty reference array all the time, create it only once - it is immutable
    private static final ServiceReference[] NO_REFS = new ServiceReference[0];

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public final String getTitle()
    {
        return TITLE;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public final void printConfiguration(PrintWriter pw)
    {
        final Object[] data = new Object[4]; // used as message formatter parameters
        final ServiceReference refs[] = getServices();
        pw.print("Status: ");
        pw.println(ServicesServlet.getStatusLine(refs));

        for (int i = 0; refs != null && i < refs.length; i++)
        {
            try
            {
                final Bundle bundle = refs[i].getBundle();
                final Bundle[] usingBundles = refs[i].getUsingBundles();

                pw.println();
                pw.println(INFO.format(params(refs[i], data)));
                pw.println(FROM.format(params(bundle, data)));

                // print registration properties
                String[] keys = refs[i].getPropertyKeys();
                for (int j = 0; keys != null && j < keys.length; j++)
                {
                    final String key = keys[j];
                    // skip common keys - already added above
                    if (SERVICE_ID.equals(key) || OBJECTCLASS.equals(key)
                        || SERVICE_PID.equals(key))
                        continue;

                    pw.print("    ");
                    pw.print(key);
                    pw.print(": ");
                    pw.println(ServicesServlet.propertyAsString(refs[i], key));
                }

                // using bundles
                for (int j = 0; usingBundles != null && j < usingBundles.length; j++)
                {
                    pw.println(USING.format(params(usingBundles[j], data)));
                }
            }
            catch (Throwable t)
            {
                // a problem handling a service - ignore and continue with the next
            }
        }
    }

    private static final Object[] params(Bundle bundle, Object[] data)
    {
        data[0] = String.valueOf(bundle.getBundleId());
        data[1] = Util.getName(bundle, Locale.ENGLISH);
        data[2] = bundle.getSymbolicName();
        data[3] = Util.getHeaderValue(bundle, Constants.BUNDLE_VERSION);
        return data;
    }

    private static final Object[] params(ServiceReference ref, Object[] data)
    {
        data[0] = ServicesServlet.propertyAsString(ref, SERVICE_ID);
        data[1] = ServicesServlet.propertyAsString(ref, OBJECTCLASS);
        data[2] = ServicesServlet.propertyAsString(ref, SERVICE_PID);
        data[3] = "";
        return data;
    }

    private final ServiceReference[] getServices()
    {
        ServiceReference[] refs = null;
        try
        {
            refs = getBundleContext().getAllServiceReferences(null, null);
        }
        catch (InvalidSyntaxException e)
        {
            // ignore
        }

        // no services or invalid filter syntax (unlikely)
        return refs != null ? refs : NO_REFS;
    }

}
