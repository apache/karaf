/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.exporter;

import org.apache.felix.gogo.commands.Action;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class Activator implements BundleActivator {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Override
    public void start(BundleContext context) throws Exception {
        ServiceTracker<Action, Action> tracker = new ServiceTracker<Action, Action>(context, Action.class, null) {

            @Override
            public Action addingService(ServiceReference<Action> reference) {
                Bundle userBundle = reference.getBundle();
                try {
                    Action action = context.getService(reference);
                    CommandExporter.export(userBundle.getBundleContext(), action);
                    logger.info("Service registered");
                } catch (Exception e) {
                    logger.warn("Error exporting action from service of bundle " 
                        + userBundle.getSymbolicName()
                        + "[" + userBundle.getBundleId() + "]", e);
                }
                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference<Action> reference, Action service) {
                // TODO implement removing of commands
                super.removedService(reference, service);
            }
            
        };
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
