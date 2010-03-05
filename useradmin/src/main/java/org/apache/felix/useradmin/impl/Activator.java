/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl;

import java.util.Properties;

import org.apache.felix.useradmin.UserAdminEventDispatcher;
import org.apache.felix.useradmin.UserAdminRepository;
import org.apache.felix.useradmin.UserAdminRepositoryManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.UserAdmin;

/**
 * <p>
 * This <tt>Activator</tt> represents activator for UserAdmin service bundle.
 * Its registering UserAdmin service in the ServiceRegistry and
 * manage all its dependencies.</p>
 * 
 * @version $Rev$ $Date$
 */
public class Activator implements BundleActivator
{
    private static final String PID = "org.apache.felix.useradmin";
    private Logger logger;
    private ServiceRegistration registration;
    private UserAdminServiceImpl userAdmin;

    /**
     * @see org.osgi.framework.BundleActivator#start(BundleContext)
     */
    public void start(BundleContext context) throws Exception
    {
        //Starting logger
        logger = new Logger(context);
        logger.open();
        //Creating repository of roles for UserAdmin service
        UserAdminRepository repository = new UserAdminRepositoryImpl(logger, context);
        //Creating manager for roles repository
        UserAdminRepositoryManager repositoryManager = new UserAdminRepositoryManagerImpl(logger, repository);
        //Creating dispatcher for UserAdmin events
        UserAdminEventDispatcher eventDispatcher = new UserAdminEventDispatcherImpl(context);
        userAdmin = new UserAdminServiceImpl(context, repositoryManager, logger, eventDispatcher);
        repositoryManager.initialize(userAdmin);

        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, PID);
        registration = context.registerService(UserAdmin.class.getName(), userAdmin, props);
        userAdmin.setServiceRef(registration.getReference());
    }

    /**
     * <p>This method is unregistering UserAdmin service,closing logger and
     * closing resources kept by UserAdmin.</p>
     * 
     * @see org.osgi.framework.BundleActivator#stop(BundleContext)
     */
    public void stop(BundleContext context) throws Exception
    {
        if (registration != null)
        {
            registration.unregister();
        }
        if (userAdmin != null)
        {
            userAdmin.destroy();
        }
        if (logger != null)
        {
            logger.close();
        }
    }
}
