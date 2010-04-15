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
package org.apache.felix.webconsole.internal.compendium;

import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.felix.webconsole.internal.AbstractConfigurationPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.wireadmin.Envelope;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;

/**
 * WireAdminConfigurationPrinter reads the configured wires in WireAdmin service, and 
 * prints them along their properties and values.
 */
public final class WireAdminConfigurationPrinter extends AbstractConfigurationPrinter
{

    private static final String TITLE = "Wire Admin";

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
        final BundleContext bc = getBundleContext();
        final ServiceReference ref = bc.getServiceReference(WireAdmin.class.getName());
        final WireAdmin wireAdmin = (WireAdmin) (ref != null ? bc.getService(ref) : null);

        try
        {

            if (wireAdmin == null)
            {
                pw.println("No Wire Admin service available");
            }
            else
            {
                final Wire wires[] = wireAdmin.getWires(null);

                if (null == wires || 0 == wires.length)
                {
                    pw.println("No wires available");
                }
                else
                {
                    final int len = wires.length;
                    int valid = 0;
                    int connected = 0;

                    // status
                    for (int i = 0; i < len; i++)
                    {
                        final Wire wire = wires[i];
                        if (wire.isValid())
                            valid++;
                        if (wire.isConnected())
                            connected++;
                    }
                    pw.print("Status: ");
                    pw.print(len);
                    pw.print(" wires available, ");
                    pw.print(valid);
                    pw.print(" valid, ");
                    pw.print(connected);
                    pw.print(" connected.");
                    pw.println();
                    pw.println();

                    // print wires
                    for (int i = 0; i < len; i++)
                    {
                        pw.print("#");
                        pw.print(i);
                        print(wires[i], pw);
                    }
                }

            }
        }
        catch (InvalidSyntaxException ie)
        {
            // thrown by wireAdmin.getWires(null) - will not happen, we are not setting filter.
        }
        finally
        {
            if (ref != null)
                bc.ungetService(ref);
        }
    }

    private static final void print(Wire wire, PrintWriter pw)
    {
        pw.print("  Valid: ");
        pw.println(wire.isValid());
        pw.print("  Connected: ");
        pw.println(wire.isConnected());

        final Object val = wire.poll();
        if (null == val)
        {
            pw.println("  Value: null");
        }
        else if (val instanceof Envelope)
        {
            print(0, (Envelope) val, pw);
        }
        else if (val instanceof Envelope[])
        {
            final Envelope values[] = (Envelope[]) val;
            for (int i = 0, len = values.length; i < len; i++)
            {
                print(i, values[i], pw);
            }
        }
        else
        {
            pw.print("  Value: ");
            pw.print(val);
            pw.print(" (");
            pw.print(val.getClass().getName());
            pw.println(")");
        }

        String[] scope = wire.getScope();
        if (scope == null)
        {
            pw.println("  Scope: none");
        }
        else
        {
            pw.println("  Scope: ");
            for (int i = 0, len = scope.length; i < len; i++)
            {
                pw.print("              ");
                pw.println(scope[i]);
            }
        }

        Class[] flavors = wire.getFlavors();
        if (flavors == null)
        {
            pw.println("  Flavors: none");
        }
        else
        {
            pw.print("  Flavors: ");
            for (int i = 0, len = flavors.length; i < len; i++)
            {
                pw.print(flavors[i].getName());
                if (i < len - 1)
                    pw.print(", ");
            }
            pw.println();
        }

        Dictionary props = wire.getProperties();
        if (props == null)
        {
            pw.println("  Properties: none");
        }
        else
        {
            pw.println("  Properties: ");
            for (Enumeration e = props.keys(); e.hasMoreElements();)
            {
                final Object key = e.nextElement();
                pw.print("    ");
                pw.print(key.toString());
                pw.print('=');
                pw.println(props.get(key));
            }
        }

        pw.println();
    }

    private static final void print(int index, Envelope envelope, PrintWriter pw)
    {
        pw.print("            : Envelope #");
        pw.print(index);
        pw.print("[scope=");
        pw.print(envelope.getScope());
        pw.print(",identification=");
        pw.print(envelope.getIdentification());
        pw.print(",value=");
        pw.print(envelope.getValue());
        pw.println("]");
    }

}
