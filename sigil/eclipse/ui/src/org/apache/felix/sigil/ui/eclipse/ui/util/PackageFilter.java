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

package org.apache.felix.sigil.ui.eclipse.ui.util;


import java.util.HashSet;
import java.util.Set;

import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;


public class PackageFilter implements IFilter<IPackageImport>
{

    private Set<String> names = new HashSet<String>();


    public PackageFilter( String[] packageNames )
    {
        for ( String name : packageNames )
        {
            names.add( name );
        }
    }


    public PackageFilter( IPackageExport[] packages )
    {
        for ( IPackageExport packageExport : packages )
        {
            names.add( packageExport.getPackageName() );
        }
    }


    public boolean select( IPackageImport element )
    {
        return !names.contains( element.getPackageName() );
    }

}
