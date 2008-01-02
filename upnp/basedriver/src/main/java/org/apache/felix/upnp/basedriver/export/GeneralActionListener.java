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

package org.apache.felix.upnp.basedriver.export;


import java.util.Dictionary;
import java.util.Properties;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.UPnPStatus;
import org.cybergarage.upnp.control.ActionListener;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPException;
import org.osgi.service.upnp.UPnPService;

import org.apache.felix.upnp.basedriver.Activator;
import org.apache.felix.upnp.basedriver.util.Converter;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class GeneralActionListener implements ServiceListener,ActionListener {

	private ServiceReference dev;
	private String id;
	private boolean open;
		
	/**
	 * @param osgiServ
	 */
	public GeneralActionListener(ServiceReference sr, String serviceId) {
		try {
			Activator.bc.addServiceListener(this,
				"("+Constants.SERVICE_ID+"="+sr.getProperty(Constants.SERVICE_ID)+")");
		} catch (InvalidSyntaxException ingnored) {}
		this.dev=sr;
		this.id=serviceId;
		this.open=true;
	}

	/**
	 * @see org.cybergarage.upnp.control.ActionListener#actionControlReceived(org.cybergarage.upnp.Action)
	 */
	public synchronized boolean actionControlReceived(Action upnpAct) {
		if(!open) return false;			
		UPnPService osgiServ=null;
		try{
			osgiServ=((UPnPDevice) Activator.bc.getService(dev)).getService(id);	
		}catch(Exception ignored){}
		
		if(osgiServ==null)
			return exiting(false);
		
		UPnPAction osgiAct = osgiServ.getAction(upnpAct.getName());
		Properties inArgs = null;
		ArgumentList alIn = upnpAct.getInputArgumentList();
		ArgumentList alOut = upnpAct.getOutputArgumentList();
		String[] inArg = osgiAct.getInputArgumentNames();
		boolean invalidAction=false;
		if(inArg!=null){
			inArgs = new Properties();
			Argument arg;
			for (int j = 0; j < inArg.length; j++) {
				arg=alIn.getArgument(inArg[j]);
				try {
					inArgs.put(
							inArg[j],
							Converter.parseString(
									arg.getValue(),
									arg.getRelatedStateVariable().getDataType()
									/*osgiServ.getStateVariable(arg.getRelatedStateVariableName()).getUPnPDataType()*/
							)
					);
				} catch (Exception e) {
					invalidAction=true;
					break;
				}
			}
		}
		Dictionary outArgs=null;
		try {
			outArgs=osgiAct.invoke(inArgs);
		} catch (UPnPException e) {
			//TODO Activator.logger.log()
			upnpAct.setStatus(e.getUPnPError_Code(),e.getMessage());
			invalidAction=true;
		} catch (Exception e){
			//TODO Activator.logger.log()
			upnpAct.setStatus(UPnPStatus.ACTION_FAILED);
			invalidAction=true;
		}		
		if(invalidAction)
			return exiting(false);
		String[] outArg = osgiAct.getOutputArgumentNames();
		if(outArg!=null){
			Argument arg;
			for (int j = 0; j < outArg.length; j++) {
				arg = alOut.getArgument(outArg[j]);								
				try {
					arg.setValue(
						Converter.toString(
								outArgs.get(outArg[j]),
								arg.getRelatedStateVariable().getDataType()
								/*osgiServ.getStateVariable(arg.getRelatedStateVariableName()).getUPnPDataType()*/
						)
					);
				} catch (Exception e) {
					e.printStackTrace();
					return exiting(false);
				}
			}
		}
		return exiting(true);
	}

	/**
	 * @param b
	 * @return
	 */
	private boolean exiting(boolean b) {
		Activator.bc.ungetService(dev);
		return b;
	}

	/**
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent e) {
		if(e.getType()==ServiceEvent.UNREGISTERING){
			Activator.bc.removeServiceListener(this);
		}			
	}				
}
