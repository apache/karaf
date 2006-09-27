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

package org.apache.felix.jmood.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.utils.InstrumentationSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * This class implements the MBean interface. In order to obtain 
 * the string representation of bundles, services and packages
 * it uses the CoreUtils static methods.
 * @see org.apache.felix.jmood.utils.InstrumentationSupport
 *
 */
public class ManagedBundle implements ManagedBundleMBean {
    private Bundle bundle;
    private AgentContext ac;
    public ManagedBundle(Bundle bundle, AgentContext ac) {
        super();
        this.bundle=bundle;
        this.ac=ac;
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getBundleId()
     */
    public long getBundleId() {
        return bundle.getBundleId();
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getExportedPackages()
     */
    public String[] getExportedPackages() throws ServiceNotAvailableException{
        return InstrumentationSupport.getPackageNames(ac.getPackageadmin().getExportedPackages(bundle));
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getFragments()
     */
    public String[] getFragments() throws ServiceNotAvailableException{
        return InstrumentationSupport.getSymbolicNames(ac.getPackageadmin().getFragments(bundle));
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getHeaders()
     */
    public Hashtable getHeaders() {
        Hashtable ht=new Hashtable();
        Enumeration keys=bundle.getHeaders().keys();
        while(keys.hasMoreElements()) {
            Object key=keys.nextElement();
            ht.put(key, bundle.getHeaders().get(key));
        }
        return ht;
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getHosts()
     */
    public String[] getHosts() throws ServiceNotAvailableException{
        return InstrumentationSupport.getSymbolicNames(ac.getPackageadmin().getHosts(bundle));
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getImportedPackages()
     */
    public String[] getImportedPackages() throws ServiceNotAvailableException {
        return InstrumentationSupport.getPackageNames(InstrumentationSupport.getImportedPackages(bundle, ac));
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getLastModified()
     */
    public long getLastModified() {
        return bundle.getLastModified();
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getRegisteredServices()
     */
    public String[] getRegisteredServices() {
        return InstrumentationSupport.getServiceNames(bundle.getRegisteredServices());
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getRequiredBundles()
     */
    public String[] getBundleDependencies() throws ServiceNotAvailableException{
        Bundle[] required=InstrumentationSupport.getBundleDependencies(bundle, ac);
        return InstrumentationSupport.getSymbolicNames(required);
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getRequiringBundles()
     */
    public String[] getRequiringBundles() throws ServiceNotAvailableException {
        return InstrumentationSupport.getSymbolicNames(InstrumentationSupport.getRequiringBundles(bundle, ac));
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getServicesInUse()
     */
    public String[] getServicesInUse() {
        return InstrumentationSupport.getServiceNames(bundle.getServicesInUse());
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getStartLevel()
     */
    public int getStartLevel() throws ServiceNotAvailableException {
        return ac.getStartLevel().getBundleStartLevel(bundle);
    }
    public void setStartLevel(int level) throws ServiceNotAvailableException{
        ac.getStartLevel().setBundleStartLevel(this.bundle, level);
    }

    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getState()
     */
    public String getState() {
        return InstrumentationSupport.getState(bundle.getState());
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#getSymbolicName()
     */
    public String getSymbolicName() {
        return bundle.getSymbolicName();
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#isBundlePersistentlyStarted()
     */
    public boolean isBundlePersistentlyStarted() throws ServiceNotAvailableException {
    	try{
    	return ac.getStartLevel().isBundlePersistentlyStarted(bundle);
    	} catch (NullPointerException npe){
    		ac.error("npe", npe);
    		throw npe;
    	}
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#isFragment()
     */
    public boolean isFragment() throws ServiceNotAvailableException{
        return InstrumentationSupport.isFragment(bundle, ac);
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#isRemovalPending()
     */
    public boolean isRemovalPending() throws ServiceNotAvailableException{
    	RequiredBundle r=InstrumentationSupport.getRequiredBundle(bundle, ac);
    	if(r==null) return false; 
        return r.isRemovalPending();
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedBundleMBean#isRequired()
     */
    public boolean isRequired() throws ServiceNotAvailableException{
        return InstrumentationSupport.isBundleRequired(bundle, ac);
    }
	public void start() throws BundleException {
		bundle.start();
		
	}
	public void stop() throws BundleException {
		bundle.stop();
	}
	public void update() throws BundleException {
		bundle.update();
		
	}
	public void updateFromUrl(String url) throws MalformedURLException, BundleException, IOException {
		//TODO should we use url handler service?
		bundle.update(new URL(url).openStream());
		
	}
	public void refreshBundle() throws BundleNotAvailableException, ServiceNotAvailableException {
		CoreController c=new CoreController(ac);
		String[] b={bundle.getSymbolicName()};
		c.refreshPackages(b);
		
	}
	public void resolveBundle() throws BundleNotAvailableException, ServiceNotAvailableException {
		CoreController c=new CoreController(ac);
		String[] b={bundle.getSymbolicName()};
		c.resolveBundles(b);
	}
	public void uninstall() throws BundleException {
		bundle.uninstall();
		
	}
    

}
