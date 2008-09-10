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

package org.apache.felix.upnp.sample.binaryLight;

import java.util.Dictionary;

import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.upnp.UPnPDevice;

import org.apache.felix.upnp.sample.binaryLight.devices.LightDevice;


/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/

public class Activator implements BundleActivator {

	static BundleContext context;
	
	private ServiceRegistration serviceRegistration;
	private LightDevice light;
	private HttpService httpServ;
	
	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		Activator.context = context;
		doServiceRegistration();
		doServletRegistration();
	}


	private void doServiceRegistration() {
		light = new LightDevice(context);
		Dictionary dict = light.getDescriptions(null);
				
		serviceRegistration = context.registerService(
				UPnPDevice.class.getName(),
				light,
				dict
			);
	}
	
	private void doServletRegistration() {
        ServiceReference sr = context.getServiceReference(HttpService.class.getName());
        if (sr != null) {
			httpServ = (HttpService) context.getService(sr);

			try {
				Servlet presentationServlet = new PresentationServlet(light
						.getModel());
				httpServ.registerServlet("/upnp/binaryLight",
						presentationServlet, null, null);
			} catch (Exception e) {
				System.err.println("Exception registering presentationServlet:"
						+ e);
			}

			try {
				httpServ.registerResources("/upnp/binaryLight/images",
						"/org/apache/felix/upnp/sample/binaryLight/images",
						null);
			} catch (Exception e) {
				System.err.println("Exception registering /resource:" + e);
			}

		}		
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		serviceRegistration.unregister();
		light.close();
	}
}
