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

package org.apache.felix.sigil.core.internal.model.eclipse;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.sigil.model.AbstractCompoundModelElement;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.osgi.framework.Version;


public class Library extends AbstractCompoundModelElement implements ILibrary
{

    private static final long serialVersionUID = 1L;

    private String name;
    private Version version;
    private Set<IRequiredBundle> bundles;
    private Set<IPackageImport> imports;


    public Library()
    {
        super( "Library" );
        bundles = new HashSet<IRequiredBundle>();
        imports = new HashSet<IPackageImport>();
    }


    public void addBundle( IRequiredBundle bundle )
    {
        bundles.add( bundle );
    }


    public void addImport( IPackageImport pi )
    {
        imports.add( pi );
    }


    public Collection<IRequiredBundle> getBundles()
    {
        return bundles;
    }


    public Collection<IPackageImport> getImports()
    {
        return imports;
    }


    public String getName()
    {
        return name;
    }


    public Version getVersion()
    {
        return version;
    }


    public void removeBundle( IRequiredBundle bundle )
    {
        bundles.remove( bundle );
    }


    public void removeImport( IPackageImport pi )
    {
        imports.remove( pi );
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public void setVersion( Version version )
    {
        this.version = version;
    }
}
