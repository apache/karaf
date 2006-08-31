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

import java.io.Serializable;
import java.util.Hashtable;
public class BundleInfo implements Serializable {
    
    //mgmt attributes
    private int startLevel;
    private String state;
    private ServiceInfo[] registeredServices; 
    private ServiceInfo[] servicesInUse;
    private Hashtable headers;
    private long bundleId;
    private PackageInfo[] exportedPackages; 
    private PackageInfo[] importedPackages;
    private BundleInfo[] fragments;
    private BundleInfo[] hosts;
    private BundleInfo[] requiredBundles;
    private BundleInfo[] requiringBundles;
    private long lastModified;
    private String symbolicName;
    //private String version; //Included in the headers except for required bundles
    private boolean bundlePersistentlyStarted;
    private boolean removalPending;
    private boolean required; 
    private boolean fragment; 
//    private R4Permission[] permissions;//TODO This should include conditional permz as well as regular ones
//    private R4Configuration[] configurations; //TODO
    
    public BundleInfo() {
    }
    
    
    ////////////GETTERS'n'SETTERS////////////////////////
    public boolean isFragment() {
        return fragment;
    }


    protected void setFragment(boolean fragment) {
        this.fragment = fragment;
    }


    public long getLastModified() {
        return lastModified;
    }


    protected void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }


    public boolean isRequired() {
        return required;
    }


    protected void setRequired(boolean required) {
        this.required = required;
    }
    public long getBundleId() {
        return bundleId;
    }
    protected void setBundleId(long bundleId) {
        this.bundleId = bundleId;
    }
    public boolean isBundlePersistentlyStarted() {
        return bundlePersistentlyStarted;
    }
    protected void setBundlePersistentlyStarted(boolean bundlePersistentlyStarted) {
        this.bundlePersistentlyStarted = bundlePersistentlyStarted;
    }
    public PackageInfo[] getExportedPackages() {
        return exportedPackages;
    }
    protected void setExportedPackages(PackageInfo[] exportedPackages) {
        this.exportedPackages = exportedPackages;
    }
    public BundleInfo[] getFragments() {
        return fragments;
    }
    protected void setFragments(BundleInfo[] fragments) {
        this.fragments = fragments;
    }
    public Hashtable getHeaders() {
        return headers;
    }
    protected void setHeaders(Hashtable headers) {
        this.headers = headers;
    }
    public BundleInfo[] getHosts() {
        return hosts;
    }
    protected void setHosts(BundleInfo[] hosts) {
        this.hosts = hosts;
    }
    public PackageInfo[] getImportedPackages() {
        return importedPackages;
    }
    protected void setImportedPackages(PackageInfo[] importedPackages) {
        this.importedPackages = importedPackages;
    }
    public ServiceInfo[] getRegisteredServices() {
        return registeredServices;
    }
    protected void setRegisteredServices(ServiceInfo[] registeredServices) {
        this.registeredServices = registeredServices;
    }
    public boolean isRemovalPending() {
        return removalPending;
    }
    protected void setRemovalPending(boolean removalPending) {
        this.removalPending = removalPending;
    }
    public BundleInfo[] getRequiredBundles() {
        return requiredBundles;
    }
    protected void setRequiredBundles(BundleInfo[] requiredBundles) {
        this.requiredBundles = requiredBundles;
    }
    public BundleInfo[] getRequiringBundles() {
        return requiringBundles;
    }
    protected void setRequiringBundles(BundleInfo[] requiringBundles) {
        this.requiringBundles = requiringBundles;
    }
    public ServiceInfo[] getServicesInUse() {
        return servicesInUse;
    }
    protected void setServicesInUse(ServiceInfo[] servicesInUse) {
        this.servicesInUse = servicesInUse;
    }
    public int getStartLevel() {
        return startLevel;
    }
    protected void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }
    public String getState() {
        return state;
    }
    protected void setState(String state) {
        this.state = state;
    }
    public String getSymbolicName() {
        return symbolicName;
    }
    protected void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }
    
}
