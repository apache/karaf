/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo;

import java.util.Dictionary;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Handler Abstract Class.
 * An handler need implements tese method to be notifed of lifecycle change, getfield operation and putfield operation
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public abstract class Handler {

    /**
     * Configure the handler.
     * @param im : the instance manager
     * @param metadata : the metadata of the component
     */
    public abstract void configure(InstanceManager im, Element metadata, Dictionary configuration);

    /**
     * Stop the handler : stop the management.
     */
    public abstract void stop();

    /**
     * Start the handler : start the management.
     */
    public abstract void start();

    /**
     * This method is called when a PUTFIELD operation is detected.
     * @param fieldName : the field name
     * @param value : the value passed to the field
     */
    public void setterCallback(String fieldName, Object value) { }

    /**
     * This method is called when a GETFIELD operation is detected.
     * @param fieldName : the field name
     * @param value : the value passed to the field (by the previous handler)
     * @return : the managed value of the field
     */
    public Object getterCallback(String fieldName, Object value) { return value; }

    /**
     * Is the actual state valid for this handler ?
     * @return true is the state seems valid for the handler
     */
    public boolean isValid() { return true; }

    /**
     * This method is called when the component state changed.
     * @param state : the new state
     */
    public void stateChanged(int state) { }

    /**
     * This method is called when an instance of the component is created, but before someone can use it.
     * @param instance : the created instance
     */
    public void createInstance(Object instance) { }
}
