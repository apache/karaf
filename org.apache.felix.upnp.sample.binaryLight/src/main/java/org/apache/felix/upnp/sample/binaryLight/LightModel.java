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

package org.apache.felix.upnp.sample.binaryLight;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/* 
* @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
*/

public class LightModel implements EventSource{
	
	private boolean status = false;
	private boolean target = false;
	private boolean failure = false;
	private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	public LightModel(){	
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener){
		propertySupport.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener){
		propertySupport.removePropertyChangeListener(listener);
	}
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName,listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName,listener);
	}
	
	public void doSwitch(boolean value){
		if (value) switchOn();
		else switchOff();
	}
	
	public void switchOn(){
		target = true;
		if(! failure) {
			boolean oldStatus = status;
			status = true;
			propertySupport.firePropertyChange("Status",oldStatus,status);
		}
	}
	
	public void switchOff(){
		target = false;
		if(! failure) {
			boolean oldStatus = status;
			status = false;
			propertySupport.firePropertyChange("Status",oldStatus,status);
		}
	}
	
	public void setFailure(boolean value){
		boolean oldFailure = failure;
		boolean oldStatus = status;
		failure = value;
		if (failure){
			status = false;
		}
		else if (target){
			status = true;
		}
		propertySupport.firePropertyChange("Status",oldStatus,status);
		propertySupport.firePropertyChange("Failure",oldFailure,failure);
	}
	

	/**
	 * @return
	 */
	public boolean getTarget() {
		return target;
	}

	/**
	 * @return
	 */
	public boolean getStatus() {
		return status;
	}



}
