/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.felix.jmood.core.instrumentation;

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.core.ServiceNotAvailableException;
import org.apache.felix.jmood.utils.InstrumentationSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;

public class FrameworkSnapshot {
	private Hashtable bundles = new Hashtable(); //<Long, BundleInfo>

	private Hashtable services = new Hashtable();//<Long, ServiceInfo>

	private Hashtable packages = new Hashtable();//<String, PackageInfo>

	private AgentContext ac;

	private long timestamp;

	public FrameworkSnapshot(AgentContext ac) {
		super();
		this.ac = ac;
		this.populate();
	}

	public long getShotTime() {
		return timestamp;
	}

	public void refreshSnapshot() {
		bundles = new Hashtable();
		services = new Hashtable();
		packages = new Hashtable();
		this.populate();
		ac.debug("factory-bundles found: " + bundles.size());
		ac.debug("factory-services found: " + services.size());
		ac.debug("factory-packages found: " + packages.size());

	}

	public BundleInfo[] getAllBundles() {
		Enumeration b = bundles.elements();
		BundleInfo[] binfo = new BundleInfo[bundles.size()];
		for (int i = 0; i < binfo.length; i++) {
			binfo[i] = (BundleInfo) b.nextElement();
		}
		return binfo;
	}

	public ServiceInfo[] getAllServiceInfo() {
		Enumeration s = services.elements();
		ServiceInfo[] sinfo = new ServiceInfo[services.size()];
		for (int i = 0; i < sinfo.length; i++) {
			sinfo[i] = (ServiceInfo) s.nextElement();
		}
		return sinfo;
	}

	public PackageInfo[] getAllPackageInfo() {
		Enumeration p = packages.elements();
		PackageInfo[] pinfo = new PackageInfo[packages.size()];
		for (int i = 0; i < pinfo.length; i++) {
			pinfo[i] = (PackageInfo) p.nextElement();
		}
		return pinfo;
	}

	// ///////////PRIVATE METHODS//////////////////////////

	private void populate() {
		Bundle[] bundles = ac.getBundleContext().getBundles();
		BundleInfo[] binfo = new BundleInfo[bundles.length];
		for (int i = 0; i < binfo.length; i++) {
			binfo[i] = getBundleInfo(bundles[i]);
		}
		this.timestamp = System.currentTimeMillis();
	}

private BundleInfo getBundleInfo(Bundle bundle) {
        if (bundle==null) return null;
        ac.debug("creating R4 bundleinfo:  "+bundle.getSymbolicName());
        Long key = new Long(bundle.getBundleId());
        if (bundles.containsKey(key))
            return (BundleInfo) bundles.get(key);
        BundleInfo b = new BundleInfo();
        b.setBundleId(bundle.getBundleId());
        bundles.put(key, b);
        try{
            b.setFragments(this.getFragments(bundle));
            b.setHeaders(InstrumentationSupport.getHashtable(bundle.getHeaders()));
            b.setLastModified(bundle.getLastModified());
            b.setRegisteredServices(this.getSvcsInfo(bundle.getRegisteredServices()));
        	b.setBundlePersistentlyStarted(InstrumentationSupport.isBundlePersistentlyStarted(bundle, ac));
        	b.setStartLevel(InstrumentationSupport.getBundleStartLevel(bundle, ac));
        	b.setExportedPackages(this.getPkgInfo(InstrumentationSupport.getExportedPackages(bundle, ac)));
        	b.setFragment(InstrumentationSupport.isFragment(bundle, ac));
        	b.setRequired(InstrumentationSupport.isBundleRequired(bundle, ac));
        	b.setRemovalPending(InstrumentationSupport.isRequiredBundleRemovalPending(bundle, ac));
        	b.setRequiredBundles(this.getBundleDependencies(bundle));
        	b.setRequiringBundles(this.getBInfos(InstrumentationSupport.getRequiringBundles(bundle, ac)));
        	b.setImportedPackages(this.getImportedPackages(bundle));
        }catch(ServiceNotAvailableException sae){
        	//Not needed, since they are default values, placed for clarity
        	b.setBundlePersistentlyStarted(false);
        	b.setStartLevel(-1);
        	b.setExportedPackages(null);
        	b.setFragment(false);
        	b.setRequired(false);
        	b.setRemovalPending(false);
            b.setRequiredBundles(null);
            b.setRequiringBundles(null);
            b.setImportedPackages(null);
            ac.warning(sae.getMessage());
        }
        b.setServicesInUse(this.getSvcsInfo(bundle.getServicesInUse()));
        b.setState(InstrumentationSupport.getState(bundle.getState()));
        b.setSymbolicName(bundle.getSymbolicName());
        return b;
    }	private ServiceInfo getServiceInfo(ServiceReference svc) {
		if (svc == null)
			return null;
		ac.debug("Creating R4service for service id "
				+ svc.getProperty(Constants.SERVICE_ID));
		Long key = (Long) svc.getProperty(Constants.SERVICE_ID);
		if (services.containsKey(key))
			return (ServiceInfo) services.get(key);
		ServiceInfo s = new ServiceInfo();
		services.put(key, s);

		// now we set the atts
		s.setBundle(this.getBundleInfo(svc.getBundle()));
		Bundle[] using = svc.getUsingBundles();
		if (using != null) {
			BundleInfo[] r4Using = new BundleInfo[using.length];
			for (int i = 0; i < r4Using.length; i++) {
				r4Using[i] = getBundleInfo(using[i]);
			}
			s.setUsingBundles(r4Using);
		}
		Hashtable props = new Hashtable();//<String, Object>
		String[] keys=svc.getPropertyKeys();
		for (int i=0; i<keys.length;i++) {
			props.put(keys[i], svc.getProperty(keys[i]));
		}
		s.setProperties(props);
		return s;

	}

	private PackageInfo getPackageInfo(ExportedPackage pkg) {
		if (pkg == null)
			return null;
		ac.debug("Creating PackageInfo for package " + pkg.getName());
		String key = pkg.getName();
		if (packages.containsKey(key))
			return (PackageInfo) packages.get(key);
		PackageInfo p = new PackageInfo();
		packages.put(key, p);
		p.setExportingBundle(getBundleInfo(pkg.getExportingBundle()));
		Bundle[] importing = pkg.getImportingBundles();
		if (importing != null) {
			BundleInfo[] r4importing = new BundleInfo[importing.length];
			for (int i = 0; i < r4importing.length; i++) {
				r4importing[i] = getBundleInfo(importing[i]);
			}
			p.setImportingBundles(r4importing);
		}
		p.setName(pkg.getName());
		p.setRemovalPending(pkg.isRemovalPending());
		p.setVersion(pkg.getVersion().toString());
		return p;
	}

	private BundleInfo[] getFragments(Bundle bundle) throws ServiceNotAvailableException{
		Bundle[] fragments = ac.getPackageadmin().getFragments(bundle);
		if (fragments == null)
			return null;
		BundleInfo[] r4fragments = new BundleInfo[fragments.length];
		for (int i = 0; i < fragments.length; i++) {
			r4fragments[i] = getBundleInfo(fragments[i]);
		}
		return r4fragments;
	}

	private ServiceInfo[] getSvcsInfo(ServiceReference[] svcs) {
		if (svcs == null)
			return null;
		ServiceInfo[] r4svcs = new ServiceInfo[svcs.length];
		for (int i = 0; i < svcs.length; i++) {
			r4svcs[i] = getServiceInfo(svcs[i]);
		}
		return r4svcs;
	}

	private PackageInfo[] getImportedPackages(Bundle bundle) throws ServiceNotAvailableException {
			return this.getPkgInfo(InstrumentationSupport.getImportedPackages(bundle, ac));
	}

	private PackageInfo[] getPkgInfo(ExportedPackage[] pkgs) {
		if (pkgs == null)
			return null;
		PackageInfo[] r4pkgs = new PackageInfo[pkgs.length];
		for (int i = 0; i < pkgs.length; i++) {
			r4pkgs[i] = getPackageInfo(pkgs[i]);
		}
		return r4pkgs;
	}


	private BundleInfo[] getBundleDependencies(Bundle bundle) throws ServiceNotAvailableException {
			Bundle[] required = InstrumentationSupport.getBundleDependencies(bundle, ac);
			return this.getBInfos(required); 
		}
	private BundleInfo[] getBInfos(Bundle[] bundles){
		if(bundles==null) return null;
		BundleInfo[] info = new BundleInfo[bundles.length];
		for (int i = 0; i < info.length; i++) {
			info[i] = getBundleInfo(bundles[i]);
		}
		return info;
		
	}
}
