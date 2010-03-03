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

import org.apache.felix.sigil.model.AbstractModelElement;
import org.apache.felix.sigil.model.eclipse.IBundleCapability;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.osgi.framework.Version;

public class BundleCapability extends AbstractModelElement implements IBundleCapability
{

    private final String bsn;
    private final Version version;

    public BundleCapability(IBundleModelElement bundle)
    {
        super("Bundle Capability");
        this.bsn = bundle.getSymbolicName();
        this.version = bundle.getVersion();
        setParent(bundle.getParent());
    }

    public String getSymbolicName()
    {
        return bsn;
    }

    public Version getVersion()
    {
        return version;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == this ) return true;
        if ( obj == null ) return false;

        if ( obj instanceof BundleCapability ) {
            BundleCapability bc = (BundleCapability) obj;
            return (bsn == null ? bc.bsn == null : bsn.equals(bc.bsn)) && 
                (version == null ? bc.version == null : version.equals(bc.version));
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int hc = 11;
        
        if ( bsn!= null ) {
            hc *= bsn.hashCode();
        }
        
        if ( version != null ) {
            hc *= version.hashCode();
        }

        return hc;
    }
}
