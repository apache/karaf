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
package org.apache.felix.jmood.compendium;
import java.util.Hashtable;

public interface ConfigurationDelegateMBean {
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#getPid()
	 */
	public abstract String getPid() throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#getProperties()
	 */
	public abstract Hashtable getProperties() throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#update(java.util.Dictionary)
	 *hashtable is a dictionary!
	 */
	public abstract void update(Hashtable properties) throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#delete()
	 */
	public abstract void delete() throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#getFactoryPid()
	 */
	public abstract String getFactoryPid() throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#update()
	 */
	public abstract void update() throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#setBundleLocation(java.lang.String)
	 */
	public abstract void setBundleLocation(String bundleLocation) throws Exception;
	/* (no Javadoc)
	 * @see org.osgi.service.cm.Configuration#getBundleLocation()
	 */
	public abstract String getBundleLocation() throws Exception;
	/* (no Javadoc)
	 * @see org.apache.felix.jmood.modules.configadmin.ConfigurationDelegateMXBean#getProperty(java.lang.String)
	 */
	public abstract String getProperty(String key) throws Exception;
	/* (no Javadoc)
	 * @see org.apache.felix.jmood.modules.configadmin.ConfigurationDelegateMXBean#setProperty(java.lang.String, java.lang.String)
	 */
	public abstract void setProperty(String key, String value, String type)
		throws Exception;
	public abstract void deleteProperty(String key) throws Exception;
}