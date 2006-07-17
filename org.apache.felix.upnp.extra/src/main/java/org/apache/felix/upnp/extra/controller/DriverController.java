/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.upnp.extra.controller;

//import java.net.InetAddress;

/* 
* @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
*/
public interface DriverController {
	/**
	 * String for searching all the device on UPnP Network
	 */
    public final static String ALL_DEVICE = "ssdp:all";
    
    /**
     * String for searching only root device on UPnP Network
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
     * @param n the level of log that you want to set
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
     * Sent a search message on the UPnP Network, and refresh the device 
     * founded by UPnP Base Driver 
     * 
     * @param target The SSDP string used for the search
     */
    public void search(String target);


}
