/*
 * Copyright (c) OSGi Alliance (2004, 2008). All Rights Reserved.
 * 
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
package info.dmtree.registry;

import info.dmtree.DmtAdmin;
import info.dmtree.notification.NotificationService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This class is the central access point for Device Management services.
 * Applications can use the static factory methods provided in this class to
 * obtain access to the different Device Management related services, such as
 * the DmtAdmin for manipulating the tree, or the Notification Service for
 * sending notifications to management servers.
 * <p>
 * These methods are not needed in an OSGi environment, clients should retrieve
 * the required service objects from the OSGi Service Registry.
 * 
 * @version $Revision: 5673 $
 */
public final class DmtServiceFactory {
    private static BundleContext context = null;
    
    /**
     * A private constructor to suppress the default public constructor.
     */
    private DmtServiceFactory() {}
    
    /**
     * This method is used to obtain access to <code>DmtAdmin</code>, which
     * enables applications to manipulate the Device Management Tree.
     * 
     * @return a DmtAdmin service object
     */
    public static DmtAdmin getDmtAdmin() {
        if(context == null)
            throw new IllegalStateException("Cannot retrieve Dmt Admin " +
                    "service, implementation bundle not started yet.");
        
        ServiceReference dmtAdminRef = 
            context.getServiceReference(DmtAdmin.class.getName());
        if(dmtAdminRef == null)
            throw new IllegalStateException("Dmt Admin service not found in " +
                    "service registry.");
        
        DmtAdmin dmtAdmin = (DmtAdmin) context.getService(dmtAdminRef);
        if(dmtAdmin == null)
            throw new IllegalStateException("Dmt Admin service not found in " +
                    "service registry.");
        
        return dmtAdmin;
    }

    /**
     * This method is used to obtain access to <code>NotificationService</code>,
     * which enables applications to send asynchronous notifications to
     * management servers.
     * 
     * @return a NotificationService service object
     */
    public static NotificationService getNotificationService() {
        if(context == null)
            throw new IllegalStateException("Cannot retrieve Notification " +
                    "service, implementation bundle not started yet.");
        
        ServiceReference notificationServiceRef = 
            context.getServiceReference(NotificationService.class.getName());
        if(notificationServiceRef == null)
            throw new IllegalStateException("Notification service not found " +
                    "in service registry.");
        
        NotificationService notificationService = 
            (NotificationService) context.getService(notificationServiceRef);
        if(notificationService == null)
            throw new IllegalStateException("Notification service not found " +
                    "in service registry.");
        
        return notificationService;
    }
}
