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
package info.dmtree.notification;

import info.dmtree.DmtData;
import info.dmtree.Uri;

/**
 * Immutable data structure carried in an alert (client initiated notification).
 * The <code>AlertItem</code> describes details of various notifications that
 * can be sent by the client, for example as alerts in the OMA DM protocol. The
 * use cases include the client sending a session request to the server (alert
 * 1201), the client notifying the server of completion of a software update
 * operation (alert 1226) or sending back results in response to an asynchronous
 * EXEC command.
 * <p>
 * The data syntax and semantics varies widely between various alerts, so does
 * the optionality of particular parameters of an alert item. If an item, such
 * as source or type, is not defined, the corresponding getter method returns
 * <code>null</code>. For example, for alert 1201 (client-initiated session) all
 * elements will be <code>null</code>.
 * <P>
 * The syntax used in <code>AlertItem</code> class corresponds to the OMA DM
 * alert format. {@link NotificationService} implementations on other management
 * protocols should map these constructs to the underlying protocol.
 * 
 * @version $Revision: 5673 $
 */
public class AlertItem {

    private final String source;

    private final String type;

    private final String mark;

    private final DmtData data;

    /**
     * Create an instance of the alert item. The constructor takes all possible
     * data entries as parameters. Any of these parameters can be
     * <code>null</code>. The semantics of the parameters may be refined by
     * the definition of a specific alert, identified by its alert code (see
     * {@link NotificationService#sendNotification}). In case of Generic Alerts
     * for example (code 1226), the <code>mark</code> parameter contains a
     * severity string.
     * 
     * @param source the URI of the node which is the source of the alert item
     * @param type a MIME type or a URN that identifies the type of the data in
     *        the alert item
     * @param data a <code>DmtData</code> object that contains the format and
     *        value of the data in the alert item
     * @param mark the mark parameter of the alert item
     */
    public AlertItem(String source, String type, String mark, DmtData data) {
        this.source = source;
        this.type = type;
        this.mark = mark;
        this.data = data;
    }

    /**
     * Create an instance of the alert item, specifying the source node URI as
     * an array of path segments. The constructor takes all possible data
     * entries as parameters. Any of these parameters can be <code>null</code>.
     * The semantics of the parameters may be refined by the definition of a
     * specific alert, identified by its alert code (see
     * {@link NotificationService#sendNotification}). In case of Generic Alerts
     * for example (code 1226), the <code>mark</code> parameter contains a
     * severity string.
     * 
     * @param source the path of the node which is the source of the alert item
     * @param type a MIME type or a URN that identifies the type of the data in
     *        the alert item
     * @param data a <code>DmtData</code> object that contains the format and
     *        value of the data in the alert item
     * @param mark the mark parameter of the alert item
     */
    public AlertItem(String[] source, String type, String mark, DmtData data) {
        if ((null == source)) {
            this.source = null;
        } else {
            this.source = Uri.toUri(source);
        }
        this.type = type;
        this.mark = mark;
        this.data = data;
    }

    /**
     * Get the node which is the source of the alert. There might be no source
     * associated with the alert item.
     * 
     * @return the URI of the node which is the source of this alert, or
     *         <code>null</code> if there is no source
     */
    public String getSource() {
        return source;
    }

    /**
     * Get the type associated with the alert item. The type string is a MIME
     * type or a URN that identifies the type of the data in the alert item
     * (returned by {@link #getData}). There might be no type associated with
     * the alert item.
     * 
     * @return the type type associated with the alert item, or
     *         <code>null</code> if there is no type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the mark parameter associated with the alert item. The interpretation
     * of the mark parameter depends on the alert being sent, as identified by
     * the alert code in {@link NotificationService#sendNotification}. There
     * might be no mark associated with the alert item.
     * 
     * @return the mark associated with the alert item, or <code>null</code>
     *         if there is no mark
     */
    public String getMark() {
        return mark;
    }

    /**
     * Get the data associated with the alert item. The returned
     * <code>DmtData</code> object contains the format and the value of the
     * data in the alert item. There might be no data associated with the alert
     * item.
     * 
     * @return the data associated with the alert item, or <code>null</code>
     *         if there is no data
     */
    public DmtData getData() {
        return data;
    }

    /**
     * Returns the string representation of this alert item. The returned string
     * includes all parameters of the alert item, and has the following format:
     * 
     * <pre>
     *   AlertItem(&lt;source&gt;, &lt;type&gt;, &lt;mark&gt;, &lt;data&gt;)
     * </pre>
     * 
     * The last parameter is the string representation of the data value. The
     * format of the data is not explicitly included.
     * 
     * @return the string representation of this alert item
     */
    public String toString() {
        return "AlertItem(" + source + ", " + type + ", " + mark + ", " + data
                + ")";
    }
}
