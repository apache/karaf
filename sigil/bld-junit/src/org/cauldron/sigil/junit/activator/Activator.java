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

package org.cauldron.sigil.junit.activator;

import org.cauldron.sigil.junit.server.JUnitService;
import org.cauldron.sigil.junit.server.impl.JUnitServiceFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author dave
 */
public class Activator implements BundleActivator {
	private ServiceRegistration reg;
	private JUnitServiceFactory service;

	public void start(final BundleContext ctx) {
		service = new JUnitServiceFactory();
		service.start(ctx);
		reg = ctx.registerService(JUnitService.class.getName(), service, null);
    }

    public void stop(BundleContext ctx) {
    	reg.unregister();
    	reg = null;
    	service.stop(ctx);
    	service = null;
    }
}
