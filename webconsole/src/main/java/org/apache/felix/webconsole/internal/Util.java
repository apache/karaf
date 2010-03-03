/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


/**
 * The <code>Util</code> class contains various utility methods used internally
 * by the web console implementation and the build-in plugins.
 */
public class Util
{

    // FIXME: from the constants below only PARAM_ACTION is used, consider removeal of others?

    /** web apps subpage */
    public static final String PAGE_WEBAPPS = "/webapps";

    /** vm statistics subpage */
    public static final String PAGE_VM_STAT = "/vmstat";

    /** Logs subpage */
    public static final String PAGE_LOGS = "/logs";

    /** Parameter name */
    public static final String PARAM_ACTION = "action";

    /** Parameter name */
    public static final String PARAM_CONTENT = "content";

    /** Parameter name */
    public static final String PARAM_SHUTDOWN = "shutdown";

    /** Parameter value */
    public static final String VALUE_SHUTDOWN = "shutdown";

    /**
     * Return a display name for the given <code>bundle</code>:
     * <ol>
     * <li>If the bundle has a non-empty <code>Bundle-Name</code> manifest
     * header that value is returned.</li>
     * <li>Otherwise the symbolic name is returned if set</li>
     * <li>Otherwise the bundle's location is returned if defined</li>
     * <li>Finally, as a last resort, the bundles id is returned</li>
     * </ol>
     *
     * @param bundle the bundle which name to retrieve
     * @param locale the locale, in which the bundle name is requested
     * @return the bundle name - see the description of the method for more details.
     */
    public static String getName( Bundle bundle, Locale locale )
    {
        final String loc = locale == null ? null : locale.toString();
        String name = ( String ) bundle.getHeaders( loc ).get( Constants.BUNDLE_NAME );
        if ( name == null || name.length() == 0 )
        {
            name = bundle.getSymbolicName();
            if ( name == null )
            {
                name = bundle.getLocation();
                if ( name == null )
                {
                    name = String.valueOf( bundle.getBundleId() );
                }
            }
        }
        return name;
    }

    /**
     * Returns the value of the header or the empty string if the header
     * is not available.
     *
     * @param bundle the bundle which header to retrieve
     * @param headerName the name of the header to retrieve
     * @return the header or empty string if it is not set
     */
    public static String getHeaderValue( Bundle bundle, String headerName )
    {
       Object value = bundle.getHeaders().get(headerName);
       if ( value != null )
       {
           return value.toString();
       }
       return "";
    }

    /**
     * Orders the bundles according to their name as returned by
     * {@link #getName(Bundle, Locale)}, with the exception that the system bundle is
     * always place as the first entry. If two bundles have the same name, they
     * are ordered according to their version. If they have the same version,
     * the bundle with the lower bundle id comes before the other.
     *
     * @param bundles the bundles to sort
     * @param locale the locale, used to obtain the localized bundle name
     */
    public static void sort( Bundle[] bundles, Locale locale )
    {
        Arrays.sort( bundles, new BundleNameComparator( locale ) );
    }


    /**
     * This method is the same as Collections#list(Enumeration). The reason to
     * duplicate it here, is that it is missing in OSGi/Minimum execution
     * environment.
     *
     * @param e the enumeration which to convert
     * @return the list containing all enumeration entries.
     */
    public static final ArrayList list( Enumeration e )
    {
        ArrayList l = new ArrayList();
        while ( e.hasMoreElements() )
        {
            l.add( e.nextElement() );
        }
        return l;
    }

    private static final class BundleNameComparator implements Comparator
    {
        private final Locale locale;


        BundleNameComparator( final Locale locale )
        {
            this.locale = locale;
        }


        public int compare( Object o1, Object o2 )
        {
            return compare( ( Bundle ) o1, ( Bundle ) o2 );
        }


        public int compare( Bundle b1, Bundle b2 )
        {

            // the same bundles
            if ( b1 == b2 || b1.getBundleId() == b2.getBundleId() )
            {
                return 0;
            }

            // special case for system bundle, which always is first
            if ( b1.getBundleId() == 0 )
            {
                return -1;
            }
            else if ( b2.getBundleId() == 0 )
            {
                return 1;
            }

            // compare the symbolic names
            int snComp = Util.getName( b1, locale ).compareToIgnoreCase( Util.getName( b2, locale ) );
            if ( snComp != 0 )
            {
                return snComp;
            }

            // same names, compare versions
            Version v1 = Version.parseVersion( ( String ) b1.getHeaders().get( Constants.BUNDLE_VERSION ) );
            Version v2 = Version.parseVersion( ( String ) b2.getHeaders().get( Constants.BUNDLE_VERSION ) );
            int vComp = v1.compareTo( v2 );
            if ( vComp != 0 )
            {
                return vComp;
            }

            // same version ? Not really, but then, we compare by bundle id
            if ( b1.getBundleId() < b2.getBundleId() )
            {
                return -1;
            }

            // b1 id must be > b2 id because equality is already checked
            return 1;
        }
    }
}