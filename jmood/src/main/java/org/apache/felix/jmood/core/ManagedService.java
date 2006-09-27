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

import org.apache.felix.jmood.core.instrumentation.ServiceInfo;
import org.apache.felix.jmood.utils.InstrumentationSupport;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ManagedService implements ManagedServiceMBean {
    private ServiceReference svc;
    public ManagedService(ServiceReference svc) {
        super();
        this.svc=svc;
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedServiceMBean#getBundle()
     */
    public String getBundle() {
        return InstrumentationSupport.getSymbolicName(svc.getBundle());
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedServiceMBean#getProperties()
     */
    public Hashtable getProperties() {
    	String[] keys=svc.getPropertyKeys();
    	Hashtable ht=new Hashtable();
    	for (int i = 0; i < keys.length; i++) {
			ht.put(keys[i], svc.getProperty(keys[i]));
		}
        return ht;
    }
    /* (non-Javadoc)
     * @see org.apache.felix.jmood.core.ManagedServiceMBean#getUsingBundles()
     */
    public String[] getUsingBundles() {
        return InstrumentationSupport.getSymbolicNames(svc.getUsingBundles());
    }
    public String[] getServiceInterfaces(){
    	return (String[]) svc.getProperty(Constants.OBJECTCLASS);
    }
}
