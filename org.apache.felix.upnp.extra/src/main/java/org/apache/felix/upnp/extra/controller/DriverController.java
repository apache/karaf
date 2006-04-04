/*
 DomoWare UPnP Base Driver is an implementation of the OSGi UnP Device Spcifaction
 Copyright (C) 2004  Matteo Demuru, Francesco Furfari, Stefano "Kismet" Lenzi

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 You can contact us at:
 {matte-d, sygent, kismet-sl} [at] users.sourceforge.net
 */
package org.apache.felix.upnp.extra.controller;

//import java.net.InetAddress;

/**
 * @author Stefano "Kismet" Lenzi 
 * @author Francesco Furfari 
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
