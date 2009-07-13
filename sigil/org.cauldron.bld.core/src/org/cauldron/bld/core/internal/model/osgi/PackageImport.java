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

package org.cauldron.bld.core.internal.model.osgi;

import org.cauldron.sigil.model.AbstractModelElement;
import org.cauldron.sigil.model.IModelElement;
import org.cauldron.sigil.model.InvalidModelException;
import org.cauldron.sigil.model.common.VersionRange;
import org.cauldron.sigil.model.osgi.IPackageExport;
import org.cauldron.sigil.model.osgi.IPackageImport;

public class PackageImport extends AbstractModelElement implements IPackageImport {

	private static final long serialVersionUID = 1L;
	
	private String name;
    private VersionRange versions = VersionRange.ANY_VERSION;
    
    // resolution directive
    private boolean optional;
	private boolean dependency = true;
	private OSGiImport osgiImport = OSGiImport.AUTO;

    public PackageImport() {
    	super( "OSGi Package Import" );
    }

    @Override
	public void checkValid() throws InvalidModelException {
    	if ( name == null ) {
    		throw new InvalidModelException( this, "Package name must be set" );
    	}
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageImport#isOptional()
	 */
    public boolean isOptional() {
        return optional;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageImport#setOptional(boolean)
	 */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

	public boolean isDependency() {
		return dependency;
	}

	public void setDependency(boolean dependency) {
		this.dependency  = dependency;
	}
    
	public OSGiImport getOSGiImport() {
		return osgiImport;
	}

	public void setOSGiImport(OSGiImport osgiHeader) {
		this.osgiImport = osgiHeader;
	}

    public String getPackageName() {
        return name;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageImport#setName(java.lang.String)
	 */
    public void setPackageName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageImport#getVersion()
	 */
    public VersionRange getVersions() {
        return versions;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageImport#setVersion(java.lang.String)
	 */
    public void setVersions(VersionRange versions) {
        this.versions = versions == null ? VersionRange.ANY_VERSION : versions;
    }

	@Override
	public String toString() {
		return "Package-Import[" + name + ":" + versions + ":" + (optional ? "optional" : "mandatory") + "]";
	}

	@Override
	public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        
		if ( obj instanceof PackageImport ) {
			PackageImport pi = (PackageImport) obj;
			return name.equals( pi.name ) && versions.equals( pi.versions ) && optional == pi.optional;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hc = name.hashCode() * versions.hashCode();
		
		if ( optional ) {
			hc *= -1;
		}
		
		return hc;
	}

	public boolean accepts(IModelElement provider) {
		if ( provider instanceof IPackageExport ) {
			IPackageExport pe = (IPackageExport) provider;
			return pe.getPackageName().equals( name ) && versions.contains( pe.getVersion() );
		}
		else {
			return false;
		}
	}

	public int compareTo(IPackageImport o) {
		int i = name.compareTo(o.getPackageName());
		
		if ( i == 0 ) {
			i = compareVersion(o.getVersions());
		}
		
		return i;
	}

	private int compareVersion(VersionRange range) {
		if ( versions == null ) {
			if ( range == null ) {
				return 0;
			}
			else {
				return 1;
			}
		}
		else {
			if ( range == null ) {
				return -1;
			}
			else {
				return versions.getCeiling().compareTo(range.getCeiling());
			}
		}
	}

}
