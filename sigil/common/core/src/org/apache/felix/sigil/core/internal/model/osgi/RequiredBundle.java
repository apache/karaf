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


import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.model.AbstractModelElement;
import org.apache.felix.sigil.model.ICapabilityModelElement;
import org.apache.felix.sigil.model.eclipse.IBundleCapability;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;


public class RequiredBundle extends AbstractModelElement implements IRequiredBundle
{
    private static final long serialVersionUID = 1L;

    private String symbolicName;
    private VersionRange versions = VersionRange.ANY_VERSION;
    private boolean optional;


    public RequiredBundle()
    {
        super( "OSGi Bundle Requirement" );
    }


    public String getSymbolicName()
    {
        return symbolicName;
    }


    public void setSymbolicName( String symbolicName )
    {
        this.symbolicName = symbolicName == null ? null : symbolicName.intern();
    }


    public VersionRange getVersions()
    {
        return versions;
    }


    public void setVersions( VersionRange versions )
    {
        this.versions = versions == null ? VersionRange.ANY_VERSION : versions;
    }


    public boolean isOptional()
    {
        return optional;
    }


    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }


    @Override
    public String toString()
    {
        return "RequiredBundle[" + symbolicName + ":" + versions + ":" + ( optional ? "optional" : "mandatory" ) + "]";
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;

        if ( obj instanceof RequiredBundle )
        {
            RequiredBundle rb = ( RequiredBundle ) obj;
            return symbolicName.equals( rb.symbolicName ) && versions.equals( rb.versions ) && optional == rb.optional;
        }
        else
        {
            return false;
        }
    }


    @Override
    public int hashCode()
    {
        int hc = symbolicName.hashCode() * versions.hashCode();

        if ( optional )
        {
            hc *= -1;
        }

        return hc;
    }


    public boolean accepts( ICapabilityModelElement provider )
    {
        if ( provider instanceof IBundleCapability )
        {
            IBundleCapability bndl = ( IBundleCapability ) provider;
            return symbolicName.equals( bndl.getSymbolicName() ) && versions.contains( bndl.getVersion() );
        }
        else
        {
            return false;
        }
    }


    public int compareTo( IRequiredBundle o )
    {
        int i = symbolicName.compareTo( o.getSymbolicName() );

        if ( i == 0 )
        {
            i = compareVersion( o.getVersions() );
        }

        return i;
    }


    private int compareVersion( VersionRange range )
    {
        if ( versions == null )
        {
            if ( range == null )
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
            if ( range == null )
            {
                return -1;
            }
            else
            {
                return versions.getCeiling().compareTo( range.getCeiling() );
            }
        }
    }
}
