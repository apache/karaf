/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.ipojo;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Handler Interface.
 * An handler need implements tese method to be notifed of lifecycle change, getfield operation and putfield operation
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface Handler {

	/**
	 * Configure the handler.
	 * @param cm : the component manager
	 * @param metadata : the metadata of the component
	 */
	void configure(ComponentManager cm, Element metadata);

	/**
	 * Stop the handler : stop the management.
	 */
	void stop();

	/**
	 * Start the handler : start the management.
	 */
	void start();

	/**
	 * This method is called when a PUTFIELD operation is detected.
	 * @param fieldName : the field name
	 * @param value : the value passed to the field
	 */
	void setterCallback(String fieldName, Object value);

	/**
	 * This method is called when a GETFIELD operation is detected.
	 * @param fieldName : the field name
	 * @param value : the value passed to the field (by the previous handler)
	 * @return : the managed value of the field
	 */
	Object getterCallback(String fieldName, Object value);

	/**
	 * Is the actual state valid for this handler ?
	 * @return true is the state seems valid for the handler
	 */
	boolean isValid();

	/**
	 * This method is called when the component state changed.
	 * @param state : the new state
	 */
	void stateChanged(int state);

}
