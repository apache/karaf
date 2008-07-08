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

package org.apache.felix.upnp.sample.tv;
import java.beans.PropertyChangeEvent;

import org.osgi.service.upnp.UPnPLocalStateVariable;

import org.apache.felix.upnp.extra.util.UPnPEventNotifier;

public class PowerStateVariable implements UPnPLocalStateVariable {
	
	final private String NAME = "Power";
	final private Boolean DEFAULT_VALUE = Boolean.FALSE;
	private UPnPEventNotifier notifier;
	private Boolean power = Boolean.FALSE;
	
	public PowerStateVariable(){
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getName()
	 */
	public String getName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getJavaDataType()
	 */
	public Class getJavaDataType() {
		return Boolean.class;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getUPnPDataType()
	 */
	public String getUPnPDataType() {
		return TYPE_BOOLEAN;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getDefaultValue()
	 */
	public Object getDefaultValue() {
		return DEFAULT_VALUE;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getAllowedValues()
	 */
	public String[] getAllowedValues() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getMinimum()
	 */
	public Number getMinimum() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getMaximum()
	 */
	public Number getMaximum() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#getStep()
	 */
	public Number getStep() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPStateVariable#sendsEvents()
	 */
	public boolean sendsEvents() {
		return true;
	}
	
	public Boolean getCurrentPower(){
		return power;
	}
	
	public void setPower(Boolean value){
		if (!value.equals(power)) {
			Boolean oldValue = power;
			power = value;
			if (notifier != null)
			notifier.propertyChange(new PropertyChangeEvent(this,"Power",oldValue,value));
		}
	}

	public void setNotifier(UPnPEventNotifier notifier){
		this.notifier = notifier;
	}

    public Object getCurrentValue() {
         return power;
    }
}
