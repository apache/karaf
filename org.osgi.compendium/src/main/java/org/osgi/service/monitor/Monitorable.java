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

package org.osgi.service.monitor;

/**
 * A <code>Monitorable</code> can provide information about itself in the form
 * of <code>StatusVariables</code>. Instances of this interface should register
 * themselves at the OSGi Service Registry. The <code>MonitorAdmin</code>
 * listens to the registration of <code>Monitorable</code> services, and makes
 * the information they provide available also through the Device Management
 * Tree (DMT) for remote access.
 * <p>
 * The monitorable service is identified by its PID string which must be a non-
 * <code>null</code>, non-empty string that conforms to the "symbolic-name"
 * definition in the OSGi core specification. This means that only the
 * characters [-_.a-zA-Z0-9] may be used. The length of the PID must not exceed
 * 20 characters.
 * <p>
 * A <code>Monitorable</code> may optionally support sending notifications when
 * the status of its <code>StatusVariables</code> change. Support for change
 * notifications can be defined per <code>StatusVariable</code>.
 * <p>
 * Publishing <code>StatusVariables</code> requires the presence of the
 * <code>MonitorPermission</code> with the <code>publish</code> action string.
 * This permission, however, is not checked during registration of the
 * <code>Monitorable</code> service. Instead, the <code>MonitorAdmin</code>
 * implemenatation must make sure that when a <code>StatusVariable</code> is
 * queried, it is shown only if the <code>Monitorable</code> is authorized to
 * publish the given <code>StatusVariable</code>.
 * 
 * @version $Revision: 5673 $
 */
public interface Monitorable {
    /**
     * Returns the list of <code>StatusVariable</code> identifiers published
     * by this <code>Monitorable</code>. A <code>StatusVariable</code> name
     * is unique within the scope of a <code>Monitorable</code>. The array
     * contains the elements in no particular order. The returned value must not
     * be <code>null</code>.
     * 
     * @return the <code>StatusVariable<code> identifiers published by this 
     *         object, or an empty array if none are published
     */
    public String[] getStatusVariableNames();
    
    /**
     * Returns the <code>StatusVariable</code> object addressed by its
     * identifier. The <code>StatusVariable</code> will hold the value taken
     * at the time of this method call.
     * <p>
     * The given identifier does not contain the Monitorable PID, i.e. it 
     * specifies the name and not the path of the Status Variable.
     * 
     * @param id the identifier of the <code>StatusVariable</code>, cannot be
     *        <code>null</code> 
     * @return the <code>StatusVariable</code> object
     * @throws java.lang.IllegalArgumentException if <code>id</code> points to a
     *         non-existing <code>StatusVariable</code>
     */
    public StatusVariable getStatusVariable(String id)
            throws IllegalArgumentException;

    /**
     * Tells whether the <code>StatusVariable</code> provider is able to send
     * instant notifications when the given <code>StatusVariable</code>
     * changes. If the <code>Monitorable</code> supports sending change
     * updates it must notify the <code>MonitorListener</code> when the value
     * of the <code>StatusVariable</code> changes. The
     * <code>Monitorable</code> finds the <code>MonitorListener</code>
     * service through the Service Registry.
     * <p>
     * The given identifier does not contain the Monitorable PID, i.e. it 
     * specifies the name and not the path of the Status Variable.
     * 
     * @param id the identifier of the <code>StatusVariable</code>, cannot be
     *        <code>null</code> 
     * @return <code>true</code> if the <code>Monitorable</code> can send
     *         notification when the given <code>StatusVariable</code>
     *         changes, <code>false</code> otherwise
     * @throws java.lang.IllegalArgumentException if <code>id</code> points to a
     *         non-existing <code>StatusVariable</code>
     */
    public boolean notifiesOnChange(String id) throws IllegalArgumentException;

    /**
     * Issues a request to reset a given <code>StatusVariable</code>.
     * Depending on the semantics of the actual Status Variable this call may or
     * may not succeed: it makes sense to reset a counter to its starting value,
     * but for example a <code>StatusVariable</code> of type <code>String</code>
     * might not have a meaningful default value. Note that for numeric
     * <code>StatusVariables</code> the starting value may not necessarily be
     * 0. Resetting a <code>StatusVariable</code> must trigger a monitor event.
     * <p>
     * The given identifier does not contain the Monitorable PID, i.e. it 
     * specifies the name and not the path of the Status Variable.
     * 
     * @param id the identifier of the <code>StatusVariable</code>, cannot be
     *        <code>null</code> 
     * @return <code>true</code> if the <code>Monitorable</code> could
     *         successfully reset the given <code>StatusVariable</code>,
     *         <code>false</code> otherwise
     * @throws java.lang.IllegalArgumentException if <code>id</code> points to a
     *         non-existing <code>StatusVariable</code>
     */
    public boolean resetStatusVariable(String id)
            throws IllegalArgumentException;
    
    /**
     * Returns a human readable description of a <code>StatusVariable</code>.
     * This can be used by management systems on their GUI. The 
     * <code>null</code> return value is allowed if there is no description for
     * the specified Status Variable.
     * <p>
     * The given identifier does not contain the Monitorable PID, i.e. it 
     * specifies the name and not the path of the Status Variable.
     * 
     * @param id the identifier of the <code>StatusVariable</code>, cannot be
     *        <code>null</code> 
     * @return the human readable description of this
     *         <code>StatusVariable</code> or <code>null</code> if it is not
     *         set
     * @throws java.lang.IllegalArgumentException if <code>id</code> points to a
     *         non-existing <code>StatusVariable</code>
     */
    public String getDescription(String id) throws IllegalArgumentException;
}
