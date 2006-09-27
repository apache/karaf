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

package org.apache.felix.jmood.utils;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.core.BundleNotAvailableException;
import org.apache.felix.jmood.core.ServiceNotAvailableException;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;


/**
 * This class contains helper methods 
 * 
 *
 */
public class InstrumentationSupport {
	private AgentContext ac;
	public InstrumentationSupport(AgentContext ac){
		this.ac=ac;
	}
    /**
     * <p>For each BundleInfo, this method returns the symbolic name String, which we define as the concatenation of  
     * the getSymbolicName of the <code>Bundle</code> interface and the bundle version as specified 
     * in the bundle header. Both parts are divided by a semicolon. An example would be:</p>
     * <p>
     * <code>com.acme.foo;1.0.0</code>
     * </p>
     * @param bundles The <code>Bundle</code> array to be converted
     * @return The String array
     * @see org.osgi.framework.Bundle#getSymbolicName()
     */
    public static String[] getSymbolicNames(Bundle[] bundles) {
        if(bundles==null) return null;
        String[] names=new String[bundles.length];
        for (int i = 0; i < names.length; i++) {
            names[i]=getSymbolicName(bundles[i]);
        }
        return names;
    }
    public static String getSymbolicName(Bundle bundle){
        return bundle.getSymbolicName()+";"+bundle.getHeaders().get(Constants.BUNDLE_VERSION);
    	
    }

    /**
     * <p>
     * 
     * OSGi exported packages can be uniquely identified by the tuple (packageName, packageVersion). 
     * This methods returns a String array representing those packages with the following syntax:
     * </p>
     * <p>
     * <i>packageName</i>;<i>packageVersion</i> 
     * </p><p>
     * where packageName is as returned by the method <i>getName()</i> and packageVersion as returned by the method <i>getVersion()</i>
     * in package admin's <code>ExportedPackage</code> class.
     * </p>
     * @param packages The <code>ExportedPackage</code> array to be converted
     * @return The String array
     * @see org.osgi.service.packageadmin.ExportedPackage
     */
    public static String[] getPackageNames(ExportedPackage[] packages) {
        if (packages==null) return null;
        String[] names=new String[packages.length];
        for (int i = 0; i < names.length; i++) {
            names[i]=getPackageName(packages[i]);
        }
        return names;
    
    }
	public static String getPackageName(ExportedPackage pkg) {
		return pkg.getName()+";"+pkg.getVersion().toString();
	}


    /**
     * <p>
     * OSGi Services can be registered under more than one interface (objectClass in 
     * the spec). Services have a mandatory unique service id (as defined in the SERVICE_ID property of the org.osgi.framework.Constants interface), during their lifetime (i.e, until they are 
     * garbage collected). To show this information in a consistent way, we use the following String representation
     * of the service:
     * </p>
     * <p>
     * <i>objectClass1</i>[;<i>objectClass2</i>[;<i>objectClass3</i>...]]:<i>service.id</i> 
     * </p><p>
     * where objectClass1..objectClassN are the elements of the mandatory objectClass array
     * included in the service property dictionary (and set by the framework at registration time. The property name is defined in <code>org.osgi.framework.Constants#OBJECTCLASS</code> 
     * </p>
     * @param services The <code>ServiceReference</code> array to be converted
     * @return The String array
     * @see org.osgi.framework.Constants#OBJECTCLASS
     * @see org.osgi.framework.Constants#SERVICE_ID
     * @see org.osgi.framework.ServiceReference
     */
    public static String[] getServiceNames(ServiceReference[] services) {
        if(services==null) return null;
        String[] names=new String[services.length];
        for (int i = 0; i < names.length; i++) {
            String[] objectClass=(String[])services[i].getProperty(Constants.OBJECTCLASS);
            //We asume that the framework always returns a non-empty, non-null array.
            StringBuffer sb=new StringBuffer(objectClass[0]);
            for (int j = 1; j < objectClass.length; j++) {
                sb.append(";");
                sb.append(objectClass[j]);
            }
            sb.append(services[i].getProperty(Constants.SERVICE_ID));
            names[i]=sb.toString();
        }
        return names;
    }
    public static ExportedPackage[] getImportedPackages(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException {
        Vector imported = new Vector();
        Bundle[] allBundles = ac.getBundleContext().getBundles();
        for (int i=0; i<allBundles.length;i++) {
        	Bundle b=allBundles[i];
            ExportedPackage[] eps=ac.getPackageadmin()
            .getExportedPackages(b);
            if(eps==null) continue;
            exported: for (int j=0;j<eps.length;j++) {
            	ExportedPackage ep = eps[j];
                Bundle[] imp=ep.getImportingBundles();
                if(imp==null) continue;
                for (int k=0;k<imp.length;k++) {
                	Bundle b2 =imp[k];
                    if (b2.getBundleId() == bundle.getBundleId()) {
                        imported.add(ep);
                        continue exported;
                    }
                }
            }
        }
        if (imported.size() == 0)
            return null;
        else return (ExportedPackage[])imported.toArray(new ExportedPackage[imported.size()]);

    }
    public static Bundle[] getRequiringBundles(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException{
        if (bundle==null) throw new IllegalArgumentException("Bundle argument should not be null");
        if (bundle.getSymbolicName()==null) return null;
        RequiredBundle[] required=ac.getPackageadmin().getRequiredBundles(bundle.getSymbolicName());
        if (required==null) return null;
            RequiredBundle b=null;
            for (int i=0;i<required.length;i++) {
                if(bundle.getBundleId()==required[i].getBundle().getBundleId()) {
                    b=required[i];
                    break;
                }
            }
            if(b==null) {
                ac.error(InstrumentationSupport.class.getName()+": required bundle should not be null!!!!", new Exception());
                return null;
            }
            return b.getRequiringBundles();
    }
    public static Bundle[] getBundleDependencies(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException{
        Bundle[] all=ac.getBundleContext().getBundles();
        Vector required=new Vector();
        for (int i = 0; i < all.length; i++) {
            Bundle[] requiring=getRequiringBundles(all[i], ac);
            if (requiring==null) continue;
            for (int j = 0; j < requiring.length; j++) {
            	//If the solicited bundle is requiring the bundle all[i] we mark it as required
				if (requiring[j].getBundleId()==bundle.getBundleId()) required.add(all[i]);
			}
        }
        if (required.size()==0) return null;
        return (Bundle[])required.toArray(new Bundle[required.size()]);
    }
    public static boolean isBundleRequired(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException {
    	if (getRequiredBundle(bundle, ac)==null) return false;
    	return true;
    }
    public static boolean isRequiredBundleRemovalPending(Bundle bundle, AgentContext ac)throws ServiceNotAvailableException{
    	RequiredBundle r=getRequiredBundle(bundle, ac);
    	if (r==null) return false;
    	return r.isRemovalPending();
    }
    public static RequiredBundle getRequiredBundle(Bundle bundle, AgentContext ac)throws ServiceNotAvailableException{
        RequiredBundle[] required=ac.getPackageadmin().getRequiredBundles(bundle.getSymbolicName());
        if(required==null) return null;
        for (int i = 0; i < required.length; i++) {
			if (required[i].getBundle().getBundleId()==bundle.getBundleId()) return required[i];
		}
    	return null;
    }
	public static String getState(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		}
		return null;
	}
	public static boolean isBundlePersistentlyStarted(Bundle bundle, AgentContext ac)throws ServiceNotAvailableException{
            // BUG in KNOPFLERFISH: isPersistentlyStarted throws NPE if called
			// on System bundle
            // Workaround: system bundle should always be persistently started, return true
            if (bundle.getBundleId()==0) return true;
            else
            return ac.getStartLevel().isBundlePersistentlyStarted(bundle);
            // End workaround
	}

	public static int getBundleStartLevel(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException {
        return ac.getStartLevel().getBundleStartLevel(bundle);
	}

	public static ExportedPackage[] getExportedPackages(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException{
	        return ac.getPackageadmin().getExportedPackages(bundle);
	}

	public static boolean isFragment(Bundle bundle, AgentContext ac) throws ServiceNotAvailableException{
        if(ac.getPackageadmin().getBundleType(bundle)==PackageAdmin.BUNDLE_TYPE_FRAGMENT) return true;
    	return false;
	}
	public static Hashtable getHashtable(Dictionary dic){
			Hashtable ht = new Hashtable();
			for (Enumeration keys = dic.keys(); keys.hasMoreElements();) {
				Object key = keys.nextElement();
				// Not inmutable, but unlikely to change
				ht.put(key, dic.get(key));
			}
			return ht;
		}
    public static long getBundleId(String symbolicName, AgentContext ac) throws BundleNotAvailableException{
        if(symbolicName==null) throw new IllegalArgumentException("Symbolic name cannot be null");
        String  [] s=symbolicName.split(";");
        if(s==null||s.length==0) throw new BundleNotAvailableException("Could not find bundle identified by "+symbolicName);
    	Bundle[] bundles=ac.getBundleContext().getBundles();
        long id=-1;
        Vector candidates=new Vector();
        for(int i=0;i<bundles.length;i++) {
        	//First find all bundles with symbolicName
        	String name=bundles[i].getSymbolicName();
        	if(name==null) continue;
        	if (s[0].equals(name)){ 
            	candidates.add(new Long(bundles[i].getBundleId()));
            }
        }
        //now search the one that matches the version
        for(int i=0; i< candidates.size();i++){
        	long cId=((Long)candidates.elementAt(i)).longValue();
        	Bundle c=ac.getBundleContext().getBundle(cId);
        	String version=(String)c.getHeaders().get(Constants.BUNDLE_VERSION);
        	if(s.length==1){//no version available
        		ac.debug("no version available for "+symbolicName);
        		if(candidates.size()>1) throw new BundleNotAvailableException("could not distinguish among multiple candidates");
        		if(candidates.size()==1) {
        			id=cId;
        			break;
        		}
        	}
        	if (version.equals(s[1])) id=cId;
        }
        if(id==-1) throw new BundleNotAvailableException("No " + symbolicName+ "installed");
        return id;
    }


}
