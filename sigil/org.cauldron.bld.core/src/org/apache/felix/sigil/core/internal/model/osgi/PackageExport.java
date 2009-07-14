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

import java.util.Collection;
import java.util.HashSet;

import org.apache.felix.sigil.model.AbstractModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.osgi.framework.Version;

public class PackageExport extends AbstractModelElement implements IPackageExport {

	private static final long serialVersionUID = 1L;

	private String name;
    private Version version;
    private HashSet<String> uses = new HashSet<String>();

	public PackageExport() {
		super("OSGi Package Export");
	}

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageExport#getPackageName()
	 */
    public String getPackageName() {
        return name;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageExport#setPackageName(java.lang.String)
	 */
    public void setPackageName(String packageName) {
        this.name = packageName;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageExport#getVersion()
	 */
    public Version getVersion() {
    	Version result;
        if(version != null) {
        	result = version;
        } else {
	        ISigilBundle owningBundle = getAncestor(ISigilBundle.class);
	        if(owningBundle == null) {
	        	result = Version.emptyVersion;
	        } else {
	        	result = owningBundle.getVersion();
	        }
        }
        return result;
    }
    
    public Version getRawVersion() {
    	return version;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.model.osgi.IPackageExport#setVersion(java.lang.String)
	 */
    public void setVersion(Version version) {
        this.version = version; // == null ? Version.emptyVersion : version;
    }
    
    public void addUse(String use) {
    	uses.add(use);
	}

	public Collection<String> getUses() {
		return uses;
	}

	public void removeUse(String use) {
		uses.remove(use);
	}

	@Override
    public String toString() {
    	return "PackageExport[" + name + ":" + version + ":uses=" + uses + "]";
    }

	public void setUses(Collection<String> uses) {
		this.uses.clear();
		this.uses.addAll(uses);
	}

	public int compareTo(IPackageExport o) {
		int i = name.compareTo(o.getPackageName());
		
		if ( i == 0 ) {
			i = compareVersion(o.getVersion());
		}
		
		return i;
	}

	private int compareVersion(Version other) {
		if ( version == null ) {
			if ( other == null ) {
				return 0;
			}
			else {
				return 1;
			}
		}
		else {
			if ( other == null ) {
				return -1;
			}
			else {
				return version.compareTo(other);
			}
		}
	}
	
}
