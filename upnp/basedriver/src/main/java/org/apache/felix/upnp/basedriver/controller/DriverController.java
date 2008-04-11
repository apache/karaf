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

package org.apache.felix.upnp.basedriver.controller;

//import java.net.InetAddress;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface DriverController {
	/**
	 * String for searching all the device on UPnP Networks
	 */
    public final static String ALL_DEVICE = "ssdp:all";
    
    /**
     * String for searching only root device on UPnP Networks
     */
    public final static String ROOT_DEVICE = "upnp:rootdevice";
    /*
    public InetAddress[] getBindAddress();
    
    public void setBindAddress(InetAddress[] IPs);
    */
    
    /**
     * Set how much messages should be sent by UPnP Base Driver
     * for debugging purpose
     * 
     * @param n the level of log that you want to set (0-4)
     */
    public void setLogLevel(int n);
    
    /**
     * 
     * @return the actual value of log level
     */
    public int getLogLevel();
    
    /**
     * Set if the message of the UPnP Stack should be reported or not
     * 
     * @param b true if you want show messages from UPnP Stack false otherwise 
     */
    public void setCyberDebug(boolean b);
    
    /**
     * 
     * @return true if the reporting of UPnP Stack message is active false otherwise
     */
    public boolean getCyberDebug();
    
    /**
     * forces the UPnP base driver to send an M-SEARCH message on the UPnP Networks, and refresh the device 
     * found by UPnP Base Driver 
     * 
     * @param target The SSDP string used for the search
     */
    public void search(String target);


}
