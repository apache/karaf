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
package org.apache.felix.bundleplugin;


import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;


/**
 * Extension of {@link aQute.lib.osgi.Builder} to handle package versions
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class PackageVersionAnalyzer extends Builder
{
    /*
     * Remove META-INF subfolders from exports and set package versions to bundle version.
     */
    public Map analyzeBundleClasspath( Jar dot, Map bundleClasspath, Map contained, Map referred, Map uses )
        throws IOException
    {
        Map classSpace = super.analyzeBundleClasspath( dot, bundleClasspath, contained, referred, uses );
        String bundleVersion = getProperty( BUNDLE_VERSION );
        for ( Iterator it = contained.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = ( Map.Entry ) it.next();

            /* remove packages under META-INF */
            String packageName = ( String ) entry.getKey();
            if ( packageName.startsWith( "META-INF." ) )
            {
                it.remove();
            }

            /* set package versions to bundle version values */
            if ( bundleVersion != null )
            {
                Map values = ( Map ) entry.getValue();
                if ( values.get( "version" ) == null )
                {
                    values.put( "version", bundleVersion );
                }
            }
        }
        return classSpace;
    }
}
