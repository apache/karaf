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

package org.apache.felix.wireadmin;

import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireConstants;
import org.osgi.service.wireadmin.WireAdminListener;
import org.osgi.service.wireadmin.WireAdminEvent;
/**
 * The activator
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator implements BundleActivator {

	private final static String WIREADMIN_PID="org.apache.felix.wireadmin";
	private ServiceRegistration m_reg=null;
	private WireAdminImpl m_wai=null;
	
	/**
	 * Called upon starting of the bundle.
	 *
	 * @param   context  The bundle context passed by the framework
	 * @exception   Exception
	 */
	public void start(BundleContext bundleContext) throws BundleException {
		
        m_wai= new WireAdminImpl(bundleContext);

        // Register the service
        Dictionary properties=new Properties();
		properties.put(WireConstants.WIREADMIN_PID,WIREADMIN_PID);
		m_reg = bundleContext.registerService(WireAdmin.class.getName(),m_wai,properties);

        // Event dispatching does not start until the reference is set        
        m_wai.setServiceReference(m_reg.getReference());

        if(bundleContext.getProperty("fr.imag.adele.wireadmin.traceEvt") != null)
        {
            String value = bundleContext.getProperty("fr.imag.adele.wireadmin.traceEvt");
            if(value.equals("true"))
            {
                Dictionary props=new Properties();
                props.put(WireConstants.WIREADMIN_EVENTS,new Integer(0x80|0x40|0x20|0x10|0x08|0x04|0x02|0x01));
                properties.put(WireConstants.WIREADMIN_PID,WIREADMIN_PID);
                bundleContext.registerService(WireAdminListener.class.getName(),new eventTracer(),props);
            }
        }
	}

    /**
     * Called upon stopping the bundle.
     *
     * @param   context  The bundle context passed by the framework
     * @exception   Exception
     */
	public void stop(BundleContext bundleContext) throws BundleException 
    {   
        m_wai.releaseAll();
        m_wai = null;
	}

    class eventTracer implements WireAdminListener
    {
        public void wireAdminEvent(WireAdminEvent evt)
        {
            int type = evt.getType();
            if((type & WireAdminEvent.WIRE_CREATED)!=0)
            {
                WireAdminImpl.traceln("Received event WIRE_CREATED");
            }
            if((type & WireAdminEvent.WIRE_CONNECTED)!=0)
            {
                WireAdminImpl.traceln("Received event WIRE_CONNECTED");
            }
            if((type & WireAdminEvent.WIRE_UPDATED)!=0)
            {
                WireAdminImpl.traceln("Received event WIRE_UPDATED");
            }
            if((type & WireAdminEvent.WIRE_TRACE)!=0)
            {
                WireAdminImpl.traceln("Received event WIRE_TRACE");
            }
            if((type & WireAdminEvent.WIRE_DISCONNECTED)!=0)
            {
                WireAdminImpl.traceln("Received event WIRE_DISCONNECTED");
            }
            if((type & WireAdminEvent.WIRE_DELETED)!=0)
            {
                WireAdminImpl.traceln("Received event WIRE_DELETED");
            }
            if((type & WireAdminEvent.PRODUCER_EXCEPTION)!=0)
            {
                WireAdminImpl.traceln("Received event PRODUCER_EXCEPTION");
                evt.getThrowable().printStackTrace();
            }
            if((type & WireAdminEvent.CONSUMER_EXCEPTION)!=0)
            {
                WireAdminImpl.traceln("Received event CONSUMER_EXCEPTION");
                evt.getThrowable().printStackTrace();
            }
        }
    }
}
