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

package info.dmtree.notification.spi;

import info.dmtree.notification.AlertItem;

/**
 * The RemoteAlertSender can be used to send notifications to (remote) entities
 * identified by principal names. This service is provided by Protocol Adapters,
 * and is used by the {@link info.dmtree.notification.NotificationService} when
 * sending alerts. Implementations of this interface have to be able to connect
 * and send alerts to one or more management servers in a protocol specific way.
 * <p>
 * The properties of the service registration should specify a list of
 * destinations (principals) where the service is capable of sending alerts.
 * This can be done by providing a <code>String</code> array of principal names
 * in the <code>principals</code> registration property. If this property is not
 * registered, the service will be treated as the default sender. The default
 * alert sender is only used when a more specific alert sender cannot be found.
 * <p>
 * The <code>principals</code> registration property is used when the
 * {@link info.dmtree.notification.NotificationService#sendNotification} method
 * is called, to find the proper <code>RemoteAlertSender</code> for the given
 * destination. If the caller does not specify a principal, the alert is only
 * sent if the Notification Sender finds a default alert sender, or if the
 * choice is unambiguous for some other reason (for example if only one alert
 * sender is registered).
 * 
 * @version $Revision: 5673 $
 */
public interface RemoteAlertSender {
    /**
     * Sends an alert to a server identified by its principal name. In case the
     * alert is sent in response to a previous
     * {@link info.dmtree.DmtSession#execute(String, String, String) execute}
     * command, a correlation identifier can be specified to provide the
     * association between the execute and the alert.
     * <p>
     * The <code>principal</code> parameter specifies which server the alert
     * should be sent to. This parameter can be <code>null</code> if the
     * client does not know the name of the destination. The alert should still
     * be delivered if possible; for example if the alert sender is only
     * connected to one destination.
     * <p>
     * Any exception thrown on this method will be propagated to the original
     * sender of the event, wrapped in a <code>DmtException</code> with the
     * code <code>REMOTE_ERROR</code>.
     * <p>
     * Since sending the alert and receiving acknowledgment for it is
     * potentially a very time-consuming operation, alerts are sent
     * asynchronously. This method should attempt to ensure that the alert can
     * be sent successfully, and should throw an exception if it detects any
     * problems. If the method returns without error, the alert is accepted for
     * sending and the implementation must make a best-effort attempt to deliver
     * it.
     * 
     * @param principal the name identifying the server where the alert should
     *        be sent, can be <code>null</code>
     * @param code the alert code, can be 0 if not needed
     * @param correlator the correlation identifier of an associated EXEC
     *        command, or <code>null</code> if there is no associated EXEC
     * @param items the data of the alert items carried in this alert, can be
     *        empty or <code>null</code> if no alert items are needed
     * @throws Exception if the alert can not be sent to the server
     */
    void sendAlert(String principal, int code, String correlator,
            AlertItem[] items) throws Exception;
}
