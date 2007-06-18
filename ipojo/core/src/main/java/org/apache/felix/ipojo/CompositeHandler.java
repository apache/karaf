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

import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Composite Handler Abstract Class. An composite handler need implements these
 * method to be notifed of lifecycle change...
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class CompositeHandler {

    /**
     * Configure the handler.
     * 
     * @param im : the instance manager
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     */
    public abstract void configure(CompositeManager im, Element metadata, Dictionary configuration);

    /**
     * Stop the handler : stop the management.
     */
    public abstract void stop();

    /**
     * Start the handler : start the management.
     */
    public abstract void start();

    /**
     * Is the actual state valid for this handler ?
     * 
     * @return true is the state seems valid for the handler
     */
    public boolean isValid() {
        return true;
    }

    /**
     * This method is called when the component state changed.
     * 
     * @param state : the new state
     */
    public void stateChanged(int state) {
    }

    /**
     * Return the description of the handler.
     * @return the description of the handler.
     */
    public HandlerDescription getDescription() {
        return new HandlerDescription(this.getClass().getName(), isValid());
    }

    /**
     * The instance is reconfiguring.
     * 
     * @param configuration : New instance configuration.
     */
    public void reconfigure(Dictionary configuration) {
    }

}
