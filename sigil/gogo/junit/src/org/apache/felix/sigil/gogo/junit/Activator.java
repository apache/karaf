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
package org.apache.felix.sigil.gogo.junit;

import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.sigil.junit.server.JUnitService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.command.CommandProcessor;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator
{

    public void start( final BundleContext ctx ) throws Exception
    {
        final Hashtable props = new Hashtable();
        props.put(CommandProcessor.COMMAND_SCOPE, "sigil");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "junit" });
        
        ServiceTracker tracker = new ServiceTracker(ctx, JUnitService.class.getName(), null) {
            private Map<ServiceReference, ServiceRegistration> regs;

            @Override
            public Object addingService( ServiceReference reference )
            {
                JUnitService svc = ( JUnitService ) super.addingService( reference );
                ServiceRegistration reg = ctx.registerService( SigilJunit.class.getName(), new SigilJunit(svc), props );
                regs.put(reference, reg);
                return svc;
            }

            @Override
            public void removedService( ServiceReference reference, Object service )
            {
                ServiceRegistration reg = regs.remove( reference );
                reg.unregister();
                super.removedService( reference, service );
            }
            
        };
        tracker.open();
    }

    public void stop( BundleContext ctx ) throws Exception
    {
    }

}
