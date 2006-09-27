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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.felix.jmood.AgentContext;
import org.apache.felix.jmood.core.instrumentation.BundleInfo;
import org.apache.felix.jmood.core.instrumentation.FrameworkSnapshot;
import org.apache.felix.jmood.core.instrumentation.PackageInfo;
import org.apache.felix.jmood.core.instrumentation.ServiceInfo;
import org.apache.felix.jmood.utils.InstrumentationSupport;
import org.apache.felix.jmood.utils.ObjectNames;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
public class Framework implements FrameworkMBean, MBeanRegistration {
	private MBeanRegistrator registrator;

	private AgentContext ac;

	private FrameworkSnapshot snapshot;

	public Framework(AgentContext ac) {
		super();
		this.ac = ac;
		snapshot = new FrameworkSnapshot(ac);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.FrameworkMBean#getBundles()
	 */
	public BundleInfo[] getBundles() {
		snapshot.refreshSnapshot();
		return snapshot.getAllBundles();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.FrameworkMBean#getServiceInfo()
	 */
	public ServiceInfo[] getServiceInfo() throws InvalidSyntaxException {
		snapshot.refreshSnapshot();
		return snapshot.getAllServiceInfo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.felix.jmood.core.FrameworkMBean#getPackageInfo()
	 */
	public PackageInfo[] getPackageInfo() throws ServiceNotAvailableException {
		snapshot.refreshSnapshot();
		return snapshot.getAllPackageInfo();
	}

	public Hashtable getProperties() {
		Hashtable props = new Hashtable();
		// We do not cache it just in case some value changes (although
		// unlikely)
		props.put(Constants.FRAMEWORK_VERSION, ac.getBundleContext()
				.getProperty(Constants.FRAMEWORK_VERSION));
		props.put(Constants.FRAMEWORK_VENDOR, ac.getBundleContext()
				.getProperty(Constants.FRAMEWORK_VENDOR));
		props.put(Constants.FRAMEWORK_LANGUAGE, ac.getBundleContext()
				.getProperty(Constants.FRAMEWORK_LANGUAGE));
		props.put(Constants.FRAMEWORK_OS_NAME, ac.getBundleContext()
				.getProperty(Constants.FRAMEWORK_OS_NAME));
		props.put(Constants.FRAMEWORK_OS_VERSION, ac.getBundleContext()
				.getProperty(Constants.FRAMEWORK_OS_VERSION));
		props.put(Constants.FRAMEWORK_PROCESSOR, ac.getBundleContext()
				.getProperty(Constants.FRAMEWORK_PROCESSOR));
		String bootdel = ac.getBundleContext().getProperty(
				Constants.FRAMEWORK_BOOTDELEGATION);
		if (bootdel != null)
			props.put(Constants.FRAMEWORK_BOOTDELEGATION, bootdel);
		String execenv = ac.getBundleContext().getProperty(
				Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
		if (execenv != null)
			props.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, execenv);
		String syspkgs = ac.getBundleContext().getProperty(
				Constants.FRAMEWORK_SYSTEMPACKAGES);
		if (syspkgs != null)
			props.put(Constants.FRAMEWORK_SYSTEMPACKAGES, syspkgs);
		return props;
	}

	public String getProperty(String key) {
		return ac.getBundleContext().getProperty(key);
	}

	public ObjectName preRegister(MBeanServer server, ObjectName name)
			throws Exception {
		registrator = new MBeanRegistrator(server, ac);
		return name;
	}

	public void postRegister(Boolean registrationDone) {
		registrator.init();
	}

	public void preDeregister() throws Exception {
		registrator.dispose();

	}

	public void postDeregister() {
	}
}
	class MBeanRegistrator {
		private MBeanServer server;
		
		private AgentContext ac;

		private FrameworkListener fl;

		private BundleListener bl;

		private ServiceListener sl;

		protected MBeanRegistrator(MBeanServer server, AgentContext ac) {
			this.server = server;
			this.ac=ac;
			
			fl = new FrameworkListener() {
				public void frameworkEvent(FrameworkEvent event) {
					if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
						// We cannot know which are the affected packages, just refresh all
						//However, registering mbeans is expensive, as it involves reflection
						//TODO improve this
						unregisterPackageMBeans();
						registerPackageMBeans();
						unregisterBundleMBeans();
						registerBundleMBeans();
					}
				}
			};
			bl = new BundleListener() {
				public void bundleChanged(BundleEvent event) {
					switch (event.getType()){
					case BundleEvent.INSTALLED:
						registerBundleMBean(event.getBundle());
						break;
					case BundleEvent.UNINSTALLED:
						//Ignore: uninstalled bundles are not removed until refreshed
						break;
					case BundleEvent.UPDATED:
						//Ignore Update  
						break;
					default: //nothing todo for the rest of the events
						break;
					}
				}
			};
			sl = new ServiceListener() {
				public void serviceChanged(ServiceEvent event) {
					switch (event.getType()) {
					case ServiceEvent.UNREGISTERING:
						unregisterServiceMBean(event.getServiceReference());
						break;
					case ServiceEvent.REGISTERED:
						registerServiceMBean(event.getServiceReference());
						break;
					default://nothing todo if MODIFIED
						break;
					}
				}
			};
		}
		protected void init(){
			registerBundleMBeans();
			registerServiceMBeans();
			registerPackageMBeans();
			ac.getBundleContext().addFrameworkListener(fl);
			ac.getBundleContext().addBundleListener(bl);
			ac.getBundleContext().addServiceListener(sl);
		}
		protected void dispose() {
			ac.getBundleContext().removeFrameworkListener(fl);
			ac.getBundleContext().removeBundleListener(bl);
			ac.getBundleContext().removeServiceListener(sl);
			unregisterBundleMBeans();
			unregisterServiceMBeans();
			unregisterPackageMBeans();
		}
		private void unregisterBundleMBeans(){
			try {
			Set bundles = server.queryNames(new ObjectName(
					ObjectNames.ALLBUNDLES), null);
			for (Iterator iter = bundles.iterator(); iter.hasNext();) {
				ObjectName oname = (ObjectName) iter.next();
				server.unregisterMBean(oname);
			}
			
			} catch (MalformedObjectNameException mone) {
			ac.error("unexpected error:", mone);
		} catch (NullPointerException npe) {
			//No registered bundle mbeans
		} catch (InstanceNotFoundException infe) {
			ac.error("unexpected error:", infe);
		} catch (MBeanRegistrationException mre) {
			ac.error("unexpected error:", mre);
		}
		}

		private void unregisterServiceMBeans(){
			try {
				Set services = server.queryNames(new ObjectName(
						ObjectNames.ALLSERVICES), null);
				for (Iterator iter = services.iterator(); iter.hasNext();) {
					ObjectName oname = (ObjectName) iter.next();
					server.unregisterMBean(oname);
				}
			} catch (MalformedObjectNameException mone) {
				ac.error("unexpected error:", mone);
			} catch (NullPointerException npe) {
				//No registered service mbeans
			} catch (InstanceNotFoundException infe) {
				ac.error("unexpected error:", infe);
			} catch (MBeanRegistrationException mre) {
				ac.error("unexpected error:", mre);
			}
				
		}
		private void unregisterPackageMBeans(){
			try {
				Set pkgs = server.queryNames(new ObjectName(
						ObjectNames.ALLPACKAGES), null);
				for (Iterator iter = pkgs.iterator(); iter.hasNext();) {
					ObjectName oname = (ObjectName) iter.next();
					server.unregisterMBean(oname);
				}
			} catch (MalformedObjectNameException mone) {
				ac.error("unexpected error:", mone);
			} catch (NullPointerException npe) {
				//No registered package mbeans
			} catch (InstanceNotFoundException infe) {
				ac.error("unexpected error:", infe);
			} catch (MBeanRegistrationException mre) {
				ac.error("unexpected error:", mre);
			}
			
		}
		private void unregisterServiceMBean(ServiceReference service){
			try {
				server.unregisterMBean(new ObjectName(ObjectNames.SERVICE+service.getProperty(Constants.SERVICE_ID)));
			} catch (InstanceNotFoundException infe) {
				ac.error("Unexpected error", infe);
			} catch (MBeanRegistrationException mre) {
				ac.error("Unexpected error", mre);
			} catch (MalformedObjectNameException mone) {
				ac.error("Unexpected error", mone);
			}
		}
		private void registerBundleMBean(Bundle bundle){
			try {
				server.registerMBean(new ManagedBundle(bundle, ac),
						new ObjectName(ObjectNames.BUNDLE
								+ InstrumentationSupport
										.getSymbolicName(bundle)));
				ac.debug("registered mbean for "
						+ bundle.getSymbolicName());

			} catch (InstanceAlreadyExistsException iaee) {
				ac.error("unexpected error:", iaee);
			} catch (MBeanRegistrationException mre) {
				ac.error("unexpected error:", mre);
			} catch (NotCompliantMBeanException ncme) {
				ac.error("unexpected error:", ncme);
			} catch (MalformedObjectNameException mone) {
				ac.error("unexpected error:", mone);
			} catch (NullPointerException npe) {
				ac.error("unexpected error:", npe);
			}
		}
		private void registerBundleMBeans(){
				Bundle[] bundles = ac.getBundleContext().getBundles();
				for (int i = 0; i < bundles.length; i++) {
					registerBundleMBean(bundles[i]);
				}
			}
		private void registerServiceMBean(ServiceReference service){
			try {
				server.registerMBean(new ManagedService(service),
						new ObjectName(ObjectNames.SERVICE
								+ service.getProperty(
										Constants.SERVICE_ID)));
				ac.debug("registed mbean for "
						+ service.getProperty(
								Constants.SERVICE_ID));
			} catch (InstanceAlreadyExistsException iaee) {
				ac.error("unexpected error:", iaee);
			} catch (MBeanRegistrationException mre) {
				ac.error("unexpected error:", mre);
			} catch (NotCompliantMBeanException ncme) {
				ac.error("unexpected error:", ncme);
			} catch (MalformedObjectNameException mone) {
				ac.error("unexpected error:", mone);
			} catch (NullPointerException npe) {
				ac.error("unexpected error:", npe);
			}
			
		}
		private void registerServiceMBeans(){
			try {
				ServiceReference[] services=ac.getBundleContext().getServiceReferences(null, null);
				if (services != null) {
					for (int i = 0; i < services.length; i++) {
						registerServiceMBean(services[i]);
					}
				} else
					ac.debug("no services found");
			} catch (InvalidSyntaxException ie) {
				ac.error("unexpected error:", ie);
			}
		}
		private void registerPackageMBeans(){
			try {
				ExportedPackage[] pkgs=ac.getPackageadmin().getExportedPackages((Bundle)null);
				if (pkgs != null) {
					for (int i = 0; i < pkgs.length; i++) {
						try {
							server.registerMBean(new ManagedPackage(pkgs[i]),
									new ObjectName(ObjectNames.PACKAGE
											+ InstrumentationSupport.getPackageName(pkgs[i])));
							ac.debug("registed mbean for " + InstrumentationSupport.getPackageName(pkgs[i]));

						} catch (InstanceAlreadyExistsException iaee) {
							ac.error("unexpected error:", iaee);
						} catch (MBeanRegistrationException mre) {
							ac.error("unexpected error:", mre);
						} catch (NotCompliantMBeanException ncme) {
							ac.error("unexpected error:", ncme);
						} catch (MalformedObjectNameException mone) {
							ac.error("unexpected error:", mone);
						} catch (NullPointerException npe) {
							ac.error("unexpected error:", npe);
						}
					}
				} else
					ac.debug("no packages found");
			} catch (ServiceNotAvailableException se) {
				ac.error("No package admin available", se);
			}
		}
	}


