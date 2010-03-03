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

package org.apache.felix.sigil.core.internal.model.osgi;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.sigil.model.AbstractModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.osgi.framework.Version;


public class PackageExport extends AbstractModelElement implements IPackageExport
{

    private static final long serialVersionUID = 1L;

    private String name;
    private Version version;
    private String[] uses = new String[0];

    public PackageExport()
    {
        super( "OSGi Package Export" );
    }


    public String getPackageName()
    {
        return name;
    }


    public void setPackageName( String packageName )
    {
        this.name = packageName;
    }


    public Version getVersion()
    {
        Version result;
        if ( version != null )
        {
            result = version;
        }
        else
        {
            ISigilBundle owningBundle = getAncestor( ISigilBundle.class );
            if ( owningBundle == null )
            {
                result = Version.emptyVersion;
            }
            else
            {
                result = owningBundle.getVersion();
            }
        }
        return result;
    }


    public Version getRawVersion()
    {
        return version;
    }


    public void setVersion( Version version )
    {
        this.version = version; // == null ? Version.emptyVersion : version;
    }


    public void addUse( String use )
    {
        ArrayList<String> tmp = new ArrayList<String>(getUses());
        tmp.add(use);
        uses = tmp.toArray( new String[tmp.size()] );
    }


    public Collection<String> getUses()
    {
        return Arrays.asList(uses);
    }


    public void removeUse( String use )
    {
        ArrayList<String> tmp = new ArrayList<String>(getUses());
        tmp.remove(use);
        uses = tmp.toArray( new String[tmp.size()] );
    }


    @Override
    public String toString()
    {
        return "PackageExport[" + name + ":" + version + ":uses=" + uses + "]";
    }


    public void setUses( Collection<String> uses )
    {
        ArrayList<String> tmp = new ArrayList<String>(uses);
        this.uses = tmp.toArray( new String[tmp.size()] );
    }


    public int compareTo( IPackageExport o )
    {
        int i = name.compareTo( o.getPackageName() );

        if ( i == 0 )
        {
            i = compareVersion( o.getVersion() );
        }

        return i;
    }


    private int compareVersion( Version other )
    {
        if ( version == null )
        {
            if ( other == null )
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
        else
        {
            if ( other == null )
            {
                return -1;
            }
            else
            {
                return version.compareTo( other );
            }
        }
    }


    @Override
    public boolean equals(Object obj)
    {
        if ( obj == this ) return true;
        if ( obj == null ) return false;
        
        if ( obj instanceof PackageExport ) 
        {
            PackageExport e = (PackageExport) obj;
            return (name == null ? e.name == null : name.equals( e.name )) && 
                (version == null ? e.version == null : version.equals( e.version ));
        }
        else 
        {
            return false;
        }
    }


    @Override
    public int hashCode()
    {
        int hc = name.hashCode();
        
        if ( version != null ) hc *= version.hashCode();
        
        return hc;
    }
}
