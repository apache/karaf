/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.rmi;

import java.util.Map;
import java.util.ArrayList;

import javax.management.remote.TargetedNotification;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.DefaultRemoteNotificationServerHandler;
import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.MX4JRemoteUtils;

import org.osgi.service.log.LogService;
import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;

/**
 *
 * @version $Revision: 1.1.1.1 $
 */
class RMIRemoteNotificationServerHandler extends DefaultRemoteNotificationServerHandler
{
   RMIRemoteNotificationServerHandler(Map environment)
   {
      super(environment);
   }

   protected TargetedNotification[] filterNotifications(TargetedNotification[] notifications)
   {
      ArrayList list = new ArrayList();
      for (int i = 0; i < notifications.length; ++i)
      {
         TargetedNotification notification = notifications[i];
         if (MX4JRemoteUtils.isTrulySerializable(notification))
         {
            list.add(notification);
         }
         else
         {
            RmiConnectorActivator.log(LogService.LOG_INFO,"Cannot send notification " + notification + " to the client: it is not serializable", null);
         }
      }
      return (TargetedNotification[])list.toArray(new TargetedNotification[list.size()]);
   }
}
