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

import org.apache.felix.useradmin.CredentialAuthenticator;
import org.apache.felix.useradmin.UserAdminRepositoryManager;
import org.apache.felix.useradmin.UserAdminEventDispatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * <p>
 * This <tt>UserAdminServiceImpl</tt> class implementing a contract UserAdmin. It represents UserAdmin service is
 * exposed as a OSGi service in the ServiceRegistry.
 * </p>
 * <p>
 * Its used to manage a database of named Role objects, which can be used for authentication and authorization purposes.
 * This version of the User Admin service defines two types of Role objects: User and Group. Each type of role is
 * represented by an int constant and an interface. The range of positive integers is reserved for new types of roles
 * that may be added in the future. When defining proprietary role types, negative constant values must be used. Every
 * role has a name and a type. A User object can be configured with credentials (e.g., a password) and properties (e.g.,
 * a street address, phone number, etc.). A Group object represents an aggregation of User and Group objects. In other
 * words, the members of a Group object are roles themselves. Every User Admin service manages and maintains its own
 * namespace of Role objects, in which each Role object has a unique name.
 * </p>
 * 
 * @see org.osgi.service.useradmin.UserAdmin
 * @see org.osgi.framework.ServiceFactory
 * @see org.apache.felix.useradmin.UserAdminRepositoryManager
 * @see org.apache.felix.useradmin.UserAdminEventDispatcher
 * 
 * @version $Rev$ $Date$
 */
public class UserAdminServiceImpl implements UserAdmin, ServiceFactory
{
    private BundleContext bc;
    /**
     * This variable represents admin name permission for using UserAdmin.
     */
    private UserAdminPermission userAdminPermission;
    private UserAdminEventDispatcher eventDispatcher;
    /**
     * This variable represents state of a service. if is alive is set to true if not false.
     */
    private boolean alive;
    /**
     * This variable represents ServicReference for this service. Its needed for sending events.
     */
    private ServiceReference serviceRef;
    /**
     * This variable represents repository manager.
     */
    private UserAdminRepositoryManager repositoryManager;
    private Logger logger;
    private CredentialAuthenticator authenticator;

    /**
     * This constructor is creating new UserAdmin service.
     * 
     * @param bc BundleContext of a bundle which creating this service instance.
     * @param repositoryManager repository manager.
     * @param logger Logger instance.
     * @param dispatcher UserAdmin event dispatcher instance.
     */
    public UserAdminServiceImpl(BundleContext bc, UserAdminRepositoryManager repositoryManager, Logger logger,
        UserAdminEventDispatcher dispatcher)
    {
        this.bc = bc;
        this.userAdminPermission = new UserAdminPermission(UserAdminPermission.ADMIN, null);
        this.eventDispatcher = dispatcher;
        this.eventDispatcher.start();
        this.repositoryManager = repositoryManager;
        this.logger = logger;
        this.alive = true;
        this.authenticator = new CredentialAuthenticatorImpl();

    }

    /**
     * @see org.osgi.service.useradmin.UserAdmin#createRole(String, int)
     */
    public Role createRole(String name, int type)
    {
        checkPermission(userAdminPermission);
        Role role = repositoryManager.save(name, type, this);
        if (role != null)
        {
            // dispatching an event about created role.
            eventDispatcher.dispatchEventAsynchronusly(new UserAdminEvent(serviceRef, UserAdminEvent.ROLE_CREATED, role));
        }
        return role;
    }

    /**
     * Checking permission with security manager. If the caller thread doesn't have permission it throwing
     * SecurityException.
     * 
     * @see java.lang.SecurityManager#checkPermission(java.security.Permission)
     * @param permission
     *            UserAdminPermission for which check will e performed.
     */
    public void checkPermission(UserAdminPermission permission)
    {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null)
        {
            securityManager.checkPermission(permission);
        }
    }

    /**
     * @see org.osgi.service.useradmin.UserAdmin#getAuthorization(User)
     */
    public Authorization getAuthorization(User user)
    {
        return new AuthorizationImpl(user, this);
    }

    /**
     * @see org.osgi.service.useradmin.UserAdmin#getRole(String)
     */
    public Role getRole(String name)
    {
        return repositoryManager.findRoleByName(name);
    }

    /**
     * @see org.osgi.service.useradmin.UserAdmin#getRoles(String)
     * @see org.osgi.framework.Filter
     */
    public Role[] getRoles(String filter) throws InvalidSyntaxException
    {
        // osgi filter
        Filter ofilter = null;
        if (filter != null)
        {
            // creating a OSGi Filter for provided filter criteria
            // filter is used for finding matching Roles.
            ofilter = bc.createFilter(filter);
        }

        return repositoryManager.findRolesByFilter(ofilter);

    }

    /**
     * @see org.osgi.service.useradmin.UserAdmin#getUser(String, String)
     */
    public User getUser(String key, String value)
    {
        return (User) repositoryManager.findRoleByTypeAndKeyValue(Role.USER, key, value);

    }

    /**
     * @see org.osgi.service.useradmin.UserAdmin#removeRole(String)
     */
    public boolean removeRole(String name)
    {
        checkPermission(userAdminPermission);
        if (Role.USER_ANYONE.equals(name))
        {
            return false;
        }
        Role role = repositoryManager.remove(name);
        if (role != null)
        {
            eventDispatcher.dispatchEventAsynchronusly(new UserAdminEvent(serviceRef, UserAdminEvent.ROLE_REMOVED, role));
            return true;
        }
        return false;
    }

    /**
     * @see org.osgi.framework.ServiceFactory#ungetService(Bundle, ServiceRegistration, Object)
     */
    public Object getService(Bundle bundle, ServiceRegistration reg)
    {
        return this;
    }

    /**
     * @see org.osgi.framework.ServiceFactory#ungetService(Bundle, ServiceRegistration, Object)
     */
    public void ungetService(Bundle bundle, ServiceRegistration reg, Object obj)
    {
        // not used
    }

    /**
     * <p>
     * This method is closing UserAdmin resources. Should be used when UserAdmin service is unregistred. Alive flag is
     * set to true, eventDispacther is closed and ServiceReference is set to null.
     *</p>
     */
    public void destroy()
    {
        logger.log(LogService.LOG_DEBUG, "Closing UserAdmin service");
        alive = false;
        eventDispatcher.close();
        serviceRef = null;
        authenticator = null;
    }

    /**
     * Checks if UserAdmin service is alive.
     * 
     * @return true if service is alive or false if not.
     */
    public boolean isAlive()
    {
        return alive;
    }

    /**
     * This method is used for setting ServiceReference of this service.
     * 
     * @param serviceRef ServiceReference of this service.
     */
    public void setServiceRef(ServiceReference serviceRef)
    {
        this.serviceRef = serviceRef;
    }

    /**
     * This method returns ServiceReference for this service needed for UserAdminEvent.
     * 
     * @return ServiceReference for this service.
     */
    public ServiceReference getServiceRef()
    {
        return serviceRef;
    }

    /**
     * This method returns UserAdminPermission with name admin.
     * 
     * @return UserAdmingPermission with name admin.
     */
    public UserAdminPermission getUserAdminPermission()
    {
        return userAdminPermission;
    }

    /**
     * This method returns UserAdminEvent dispatcher.
     * 
     * @return UserAdminEventDispatcher
     * @see org.apache.felix.useradmin.UserAdminEventDispatcher
     */
    public UserAdminEventDispatcher getEventAdminDispatcher()
    {
        return eventDispatcher;
    }

    /**
     * This method returns repository manager instance.
     * 
     * @return repository manager instance.
     */
    public UserAdminRepositoryManager getRepositoryManager()
    {
        return repositoryManager;
    }

    /**
     * This method returns CredentialAuthenticator instance.
     * @return CredentialAuthenticator instance.
     */
    public CredentialAuthenticator getAuthenticator()
    {
        return this.authenticator;
    }
}
