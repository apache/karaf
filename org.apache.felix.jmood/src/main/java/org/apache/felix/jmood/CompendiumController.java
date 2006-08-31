/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.felix.jmood;

import java.util.Iterator;
import java.util.Vector;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.felix.jmood.compendium.ConfigAdminManager;
import org.apache.felix.jmood.compendium.ConfigAdminManagerMBean;
import org.apache.felix.jmood.compendium.LogManager;
import org.apache.felix.jmood.compendium.LogManagerMBean;
import org.apache.felix.jmood.compendium.UserManager;
import org.apache.felix.jmood.compendium.UserManagerMBean;
import org.apache.felix.jmood.utils.ObjectNames;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.UserAdmin;


/**
 * This class will control the life-cycle of MBeans related to OSGi Compendium
 * Services
 * 
 * 
 */
public class CompendiumController {

    private AgentContext ac;

    private ServiceListener sl;

    private Vector svcs;

    private MBeanServer server;

    public CompendiumController(MBeanServer server, AgentContext ac) {
        super();
        this.ac = ac;
        this.server = server;
        svcs = new Vector();
        svcs.add(ConfigurationAdmin.class.getName());
        svcs.add(UserAdmin.class.getName());
        svcs.add(LogService.class.getName());
        sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                processServiceEvent(event);
            }
        };
    }

    public void initController() {
        try {
            if (ac.getConfigurationAdmin() != null) {
                ConfigAdminManagerMBean ca = new ConfigAdminManager(ac);
                server
                        .registerMBean(ca, new ObjectName(
                                ObjectNames.CM_SERVICE));
            }
            if (ac.getLogservice() != null) {
                LogManagerMBean lm = new LogManager(ac);
                server.registerMBean(lm,
                        new ObjectName(ObjectNames.LOG_SERVICE));
            }
            if (ac.getUserAdmin() != null) {
                UserManagerMBean um = new UserManager(ac);
                server
                        .registerMBean(um, new ObjectName(
                                ObjectNames.UA_SERVICE));
            }
        } catch (InstanceAlreadyExistsException e) {
            ac.error("Unexpected error", e);
        } catch (MBeanRegistrationException e) {
            ac.error("Unexpected error", e);
        } catch (NotCompliantMBeanException e) {
            ac.error("Unexpected error", e);
        } catch (MalformedObjectNameException e) {
            ac.error("Unexpected error", e);
        } catch (NullPointerException e) {
            ac.error("Unexpected error", e);
        }
        ac.getBundleContext().addServiceListener(sl);

    }

    public void dispose() {
        ac.getBundleContext().removeServiceListener(sl);
        try {
            Iterator it = server.queryNames(
                    new ObjectName(ObjectNames.COMPENDIUM + ":*"), null)
                    .iterator();
            while (it.hasNext())
                server.unregisterMBean((ObjectName) it.next());

        } catch (MalformedObjectNameException e) {
            ac.error("Unexpected error", e);
        } catch (NullPointerException e) {
            ac.error("Unexpected error", e);
        } catch (InstanceNotFoundException e) {
            ac.error("Unexpected error", e);
        } catch (MBeanRegistrationException e) {
            ac.error("Unexpected error", e);
        }
    }

    private void processServiceEvent(ServiceEvent event) {
        String[] svs = (String[]) event.getServiceReference().getProperty(
                Constants.OBJECTCLASS);
        // Check if this event comes from an OSGi compendium service
        for (int i = 0; i < svs.length; i++) {
            if (svcs.contains(svs[i])) {
                if (event.getType() == ServiceEvent.REGISTERED
                        || event.getType() == ServiceEvent.UNREGISTERING)
                    handleEvent(event.getServiceReference(), svs[i], event
                            .getType());
            }
        }

    }

    private void handleEvent(ServiceReference serviceReference,
            String iService, int eType) {
        try {

            if (iService.equals(ConfigurationAdmin.class.getName())) {
                switch (eType) {
                case ServiceEvent.REGISTERED:
                    ConfigAdminManagerMBean ca = new ConfigAdminManager(ac);
                    server.registerMBean(ca, new ObjectName(
                            ObjectNames.CM_SERVICE));
                    break;
                case ServiceEvent.UNREGISTERING:
                    server.unregisterMBean(new ObjectName(
                            ObjectNames.CM_SERVICE));
                    break;
                default:
                    break;
                }
            }

            if (iService.equals(LogService.class.getName())) {
                switch (eType) {
                case ServiceEvent.REGISTERED:
                    LogManagerMBean lm = new LogManager(ac);
                    server.registerMBean(lm, new ObjectName(
                            ObjectNames.LOG_SERVICE));
                    break;
                case ServiceEvent.UNREGISTERING:
                    server.unregisterMBean(new ObjectName(
                            ObjectNames.LOG_SERVICE));
                    break;
                default:
                    break;
                }
            }

            if (iService.equals(UserAdmin.class.getName())) {
                switch (eType) {
                case ServiceEvent.REGISTERED:
                    UserManagerMBean um = new UserManager(ac);
                    server.registerMBean(um, new ObjectName(
                            ObjectNames.UA_SERVICE));
                    break;
                case ServiceEvent.UNREGISTERING:
                    server.unregisterMBean(new ObjectName(
                            ObjectNames.UA_SERVICE));
                    break;
                default:
                    break;
                }
            }
        } catch (InstanceAlreadyExistsException e) {
            ac.error("Unexpected error", e);
        } catch (MBeanRegistrationException e) {
            ac.error("Unexpected error", e);
        } catch (NotCompliantMBeanException e) {
            ac.error("Unexpected error", e);
        } catch (MalformedObjectNameException e) {
            ac.error("Unexpected error", e);
        } catch (NullPointerException e) {
            ac.error("Unexpected error", e);
        } catch (InstanceNotFoundException e) {
            ac.error("Unexpected error", e);
        }
    }

}
