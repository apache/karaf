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

import javax.management.MBeanRegistration;

import org.apache.felix.jmood.core.instrumentation.BundleInfo;
import org.apache.felix.jmood.core.instrumentation.PackageInfo;
import org.apache.felix.jmood.core.instrumentation.ServiceInfo;
import org.osgi.framework.InvalidSyntaxException;


/**
 * This mbean provides access to the basic framework information:
 * <ul>
 * <li><code>BundleInfo</code></li>
 * <li><code>PackageInfo</code></li>
 * <li><code>ServiceInfo</code></li>
 * </ul>
 * 
 * The idea underlying this is to provide both a reasonably fast way to access
 * all the framework information, avoiding too much message-passing overhead, while at the same time provide console-friendly
 * information. 
 * <p>
 * Internally, implementations of Info objects include references to each other,
 * so that with getBundleInfo() you get information, indirectly, of all the services
 * and packages.
 * </p>
 * <p>
 * This mbean also dynamically registers mbeans representing those objects
 * to ease-up direct usage by generic JMX mgmt consoles (such as Jconsole)
 * that are not aware of the data types used. While CompositeDataTypes could
 * be used (as in former JMood implementations), they are too cumbersome to use
 * and place too much dependency on JMX. 
 * </p>
 * 
 * @see org.apache.felix.jmood.core.instrumentation.BundleInfo
 * @see org.apache.felix.jmood.core.instrumentation.PackageInfo
 * @see org.apache.felix.jmood.core.instrumentation.ServiceInfo
 *
 */
public interface FrameworkMBean {

    public abstract BundleInfo[] getBundles();

    public abstract ServiceInfo[] getServiceInfo()
            throws InvalidSyntaxException;

    public abstract PackageInfo[] getPackageInfo()
            throws ServiceNotAvailableException;
    /**
     * This hashtable contains the framework properties:
     * <ul>
     * <li><code>FRAMEWORK_VERSION</code></li>
     * <li><code>FRAMEWORK_VENDOR</code></li>
     * <li><code>FRAMEWORK_LANGUAGE</code></li>
     * <li><code>FRAMEWORK_OS_NAME</code></li>
     * <li><code>FRAMEWORK_OS_VERSION</code></li>
     * <li><code>FRAMEWORK_PROCESSOR</code></li>
     * </ul><p>Plus, if available:</p><ul>
     * <li><code>FRAMEWORK_BOOTDELEGATION</code></li>
     * <li><code>FRAMEWORK_EXECUTIONENVIRONMENT</code></li>
     * <li><code>FRAMEWORK_SYSTEMPACKAGES</code></li>
     * </ul>
     * For any other properties, we suggest to use the getProperty(String key)
     * @return
     * @see org.osgi.framework.Constants
     */
    public abstract Hashtable getProperties();
    /**
     * 
     * @param key
     * @return return the property value or null if undefined. System properties are also
     * searched for if the property is not found in the framework properties 
     */
    public String getProperty(String key);

}