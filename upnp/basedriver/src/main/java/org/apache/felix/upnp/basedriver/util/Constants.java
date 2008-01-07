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

package org.apache.felix.upnp.basedriver.util;
/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface Constants {
	/**
	 * If this property is set on a UPnP Device means that the 
	 * device service is been created by UPnP base Driver. <br>
	 * The value of the does not carry any mean. <br>
	 * The name of the property is "UPnP.device.import".
	 * 
	 * @since 0.1
	 */
	public final static String UPNP_IMPORT = "UPnP.device.imported"; 
	
	
	
	/**
	 * Set the verbosity of the logging message of the bundle
	 * 
	 * @since 0.1 
	 */
	public final static String BASEDRIVER_LOG_PROP = "felix.upnpbase.log";
	
	/**
	 * If equal to "true" enables the CyberDomo UPnP SDK debugging messages
	 * 
	 * @since 0.1
	 */
	public final static String CYBERDOMO_LOG_PROP = "felix.upnpbase.cyberdomo.log";
	
	
	
	/**
	 * If equal to "true" enables Exporter facility of the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	public final static String EXPORTER_ENABLED_PROP = "felix.upnpbase.exporter.enabled";
	
	/**
	 * If equal to "true" enables Importer facility of the UPnP Base Driver
	 * 
	 * @since 0.3
	 */
	public final static String IMPORTER_ENABLED_PROP = "felix.upnpbase.importer.enabled";
	
	
	
	/**
	 * If equal to "true" enables the use of NICs with IPv4 configured and only IPv4 addresses will be used
	 * 
	 * @since 0.3 
	 */
	public final static String NET_ONLY_IPV4_PROP = "felix.upnpbase.cyberdomo.net.onlyIPV4";
	
	/**
	 * If equal to "true" enables the use of NICs with IPv6 configured and only IPv6 addresses will be used<br>
	 * <b>NOTE:</b>This property is used only on JDK 1.4 or greater  
	 * @since 0.3
	 */
	public final static String NET_ONLY_IPV6_PROP = "felix.upnpbase.cyberdomo.net.onlyIPV6";
	
	/**
	 * If equal to "true" enables the use of Loopback addresses, either IPv6 and IPv4
	 * 
	 * @since 0.3 
	 */
	public final static String NET_USE_LOOPBACK_PROP = "felix.upnpbase.cyberdomo.net.loopback";

}
