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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.sigil.model.AbstractCompoundModelElement;
import org.apache.felix.sigil.model.InvalidModelException;
import org.apache.felix.sigil.model.eclipse.ILibraryImport;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.osgi.framework.Version;

public class BundleModelElement extends AbstractCompoundModelElement implements IBundleModelElement {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    // required obr values
    private URI updateLocation;
    private String symbolicName;
    private Version version = Version.emptyVersion;
    private Set<IPackageImport> imports;
    private Set<IPackageExport> exports;
    private Set<IRequiredBundle> requires;
    private URI sourceLocation;
    private Set<String> classpathElements;
    private IRequiredBundle fragmentHost;

    // human readable values
    private String name;
    private String description;
    private String category;
    private URI licenseURI;
    private URI docURI;
    private String vendor;
    private String contactAddress;
    private String copyright;
    
    // internal values
    private String activator;
    private Set<ILibraryImport> libraries;

    public BundleModelElement() {
    	super( "OSGi Bundle" );
        this.imports = new HashSet<IPackageImport>();
        this.exports = new HashSet<IPackageExport>();
        this.requires = new HashSet<IRequiredBundle>();
        this.classpathElements = new HashSet<String>();
        this.libraries = new HashSet<ILibraryImport>();
    }

	public String getActivator() {
		return activator;
	}

	public void setActivator(String activator) {
		this.activator = activator;
	}
	
	public void addLibraryImport(ILibraryImport library) {
    	libraries.add(library);
	}

	public Set<ILibraryImport> getLibraryImports() {
		return libraries;
	}

	public void removeLibraryImport(ILibraryImport library) {
		libraries.remove(library);
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getCategory()
	 */
    public String getCategory() {
        return category;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setCategory(java.lang.String)
	 */
    public void setCategory(String category) {
        this.category = category;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getContactAddress()
	 */
    public String getContactAddress() {
        return contactAddress;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setContactAddress(java.lang.String)
	 */
    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getCopyright()
	 */
    public String getCopyright() {
        return copyright;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setCopyright(java.lang.String)
	 */
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getDocURI()
	 */
    public URI getDocURI() {
        return docURI;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setDocURI(java.net.URI)
	 */
    public void setDocURI(URI docURI) {
        this.docURI = docURI;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getExports()
	 */
    public Set<IPackageExport> getExports() {
        return exports;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#addExport(org.cauldron.sigil.model.osgi.PackageExport)
	 */
    public void addExport(IPackageExport packageExport) {
        exports.add(packageExport);
        packageExport.setParent(this);
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#removeExport(org.cauldron.sigil.model.osgi.PackageExport)
	 */
    public void removeExport(IPackageExport packageExport) {
    	if ( exports.remove(packageExport) ) {
    		packageExport.setParent(null);
    	}
    }
    
    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getImports()
	 */
    public Set<IPackageImport> getImports() {
        return imports;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#addImport(org.cauldron.sigil.model.osgi.PackageImport)
	 */
    public void addImport(IPackageImport packageImport) {
        imports.add(packageImport);
        packageImport.setParent(this);
    }
    
    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#removeImport(org.cauldron.sigil.model.osgi.PackageImport)
	 */
    public void removeImport(IPackageImport packageImport) {
    	if ( imports.remove( packageImport ) ) {
    		packageImport.setParent(null);
    	}
    }
    
    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getRequiredBundles()
	 */
    public Set<IRequiredBundle> getRequiredBundles() {
        return requires;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#addRequiredBundle(org.cauldron.sigil.model.osgi.RequiresBundle)
	 */
    public void addRequiredBundle(IRequiredBundle bundle) {
        requires.add( bundle );
        bundle.setParent(this);
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#removeRequiredBundle(org.cauldron.sigil.model.osgi.RequiresBundle)
	 */
    public void removeRequiredBundle(IRequiredBundle bundle) {
    	if ( requires.remove(bundle) ) {
    		bundle.setParent(null);
    	}
    }
    
    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getLicenseURI()
	 */
    public URI getLicenseURI() {
        return licenseURI;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setLicenseURI(java.net.URI)
	 */
    public void setLicenseURI(URI licenseURI) {
        this.licenseURI = licenseURI;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getSourceLocation()
	 */
    public URI getSourceLocation() {
        return sourceLocation;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setSourceLocation(java.net.URI)
	 */
    public void setSourceLocation(URI sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getSymbolicName()
	 */
    public String getSymbolicName() {
        return symbolicName;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setSymbolicName(java.lang.String)
	 */
    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName == null ? null : symbolicName.intern();
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getUpdateLocation()
	 */
    public URI getUpdateLocation() {
        return updateLocation;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setUpdateLocation(java.net.URI)
	 */
    public void setUpdateLocation(URI updateLocation) {
        this.updateLocation = updateLocation;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getVendor()
	 */
    public String getVendor() {
    		return vendor;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setVendor(java.lang.String)
	 */
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#getVersion()
	 */
    public Version getVersion() {
    	return version;
    }

    /* (non-Javadoc)
	 * @see org.cauldron.sigil.model.osgi.IBundleModelElement#setVersion(java.lang.String)
	 */
    public void setVersion(Version version) {
        this.version = version == null ? Version.emptyVersion : version;
    }

    public void checkValid() throws InvalidModelException {
        if (symbolicName == null)
            throw new InvalidModelException(this, "Bundle symbolic name not set");
    }

    public BundleModelElement clone() {
        BundleModelElement bd = (BundleModelElement) super.clone();

        bd.imports = new HashSet<IPackageImport>();
        bd.exports = new HashSet<IPackageExport>();
        bd.requires = new HashSet<IRequiredBundle>();
        
        for (IPackageImport pi : imports ) {
            bd.imports.add((IPackageImport) pi.clone());
        }

        for (IPackageExport pe : exports ) {
            bd.exports.add((IPackageExport) pe.clone());
        }
        
        for ( IRequiredBundle rb : requires ) {
            bd.requires.add((IRequiredBundle) rb.clone());
        }

        return bd;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("BundleModelElement[");
        buf.append(symbolicName);
        buf.append(", ");
        buf.append(version);
        buf.append("]");

        return buf.toString();
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addClasspath(String path) {
		classpathElements.add( path );
	}

	public Collection<String> getClasspaths() {
		return classpathElements.isEmpty() ? Collections.singleton( "." ) : classpathElements;
	}

	public void removeClasspath(String path) {
		classpathElements.remove( path );
	}

	public IRequiredBundle getFragmentHost() {
		return fragmentHost;
	}

	public void setFragmentHost(IRequiredBundle fragmentHost) {
		this.fragmentHost = fragmentHost;
	}
}
