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
package org.apache.felix.ipojo.handlers.architecture;

import java.util.Dictionary;

import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Architecture Handler : do reflection on your component.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ArchitectureHandler extends PrimitiveHandler implements Architecture {

    /**
     * Name of the component.
     */
    private String m_name;

    /**
     * Configure the handler.
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) {
        m_name = (String) configuration.get("instance.name");
    }

    /**
     * Stop method.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        // Nothing do do when stopping.
    }

    /**
     * Start method.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        info("Start architecture handler with " + m_name + " name");
    }

    /**
     * Get the instance description.
     * @return the instance description
     * @see org.apache.felix.ipojo.architecture.Architecture#getDescription()
     */
    public InstanceDescription getInstanceDescription() {
        return getInstanceManager().getInstanceDescription();
    }
}
