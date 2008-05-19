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
package org.apache.felix.ipojo.handler.temporal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.DependencyStateListener;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
* Temporal dependency handler.
* A temporal dependency waits (block) for the availability of the service.
* If no provider arrives in the specified among of time, a runtime exception is thrown.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class TemporalHandler extends PrimitiveHandler implements DependencyStateListener {
    
    /**
     * Default timeout if not specified.
     */
    public static final int DEFAULT_TIMEOUT = 3000;
    
    /**
     * Handler namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handler.temporal";
    
    /**
     * List of managed dependencies.
     */
    private List/*<deps>*/ m_dependencies = new ArrayList(1);

    /**
     * Start method. Starts managed dependencies.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        for (int i = 0; i < m_dependencies.size(); i++) {
            ((TemporalDependency) m_dependencies.get(i)).start();
        }
    }
    
    /**
     * Stop  method. Stops managed dependencies.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_dependencies.size(); i++) {
            ((TemporalDependency) m_dependencies.get(i)).stop();
        }
        m_dependencies.clear();
    }

    /**
     * Configure method.
     * Create managed dependencies.
     * @param meta : component type metadata.
     * @param dictionary : instance configuration.
     * @throws ConfigurationException : the dependency is not configured correctly
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element meta, Dictionary dictionary) throws ConfigurationException {
        PojoMetadata manipulation = getFactory().getPojoMetadata();
        Element[] deps = meta.getElements("requires", NAMESPACE);
        for (int i = 0; i < deps.length; i++) {
            if (!deps[i].containsAttribute("field") || m_dependencies.contains(deps[i].getAttribute("field"))) {
                error("One temporal dependency must be attached to a field or the field is already used");
                return;
            }
            String field = deps[i].getAttribute("field");

            FieldMetadata fieldmeta = manipulation.getField(field);
            if (fieldmeta == null) {
                error("The field " + field + " does not exist in the class " + getInstanceManager().getClassName());
                return;
            }             
                        
            String fil = deps[i].getAttribute("filter");
            Filter filter = null; 
            if (fil != null) {
                try {
                    filter = getInstanceManager().getContext().createFilter(fil);
                } catch (InvalidSyntaxException e) {
                    error("Cannot create the field from " + fil + ": " + e.getMessage());
                    return;
                }
            }
            
            boolean agg = false;
            String aggregate = meta.getAttribute("aggregate"); 
            if (aggregate != null) {
                agg = aggregate.equalsIgnoreCase("true");
            }

            String spec = fieldmeta.getFieldType();
            if (spec.endsWith("[]")) {
                agg = true;
                spec = spec.substring(0, spec.length() - 2);
            }
            
            long timeout = DEFAULT_TIMEOUT;
            if (deps[i].containsAttribute("timeout")) {
                timeout = new Long(deps[i].getAttribute("timeout")).longValue();
            }
            
            Class specification = DependencyModel.loadSpecification(spec, getInstanceManager().getContext());
            TemporalDependency dep = new TemporalDependency(specification, agg, filter, getInstanceManager().getContext(), timeout, this);
            m_dependencies.add(dep);
            
            getInstanceManager().register(fieldmeta, dep);
        }        
    }

    /**
     * Nothing to do.
     * A temporal dependency is always valid.
     * @param dependencymodel : dependency.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#invalidate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void invalidate(DependencyModel dependencymodel) {    }

    /**
     * Nothing to do.
     * A temporal dependency is always valid.
     * @param dependencymodel : dependency.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#validate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void validate(DependencyModel dependencymodel) {    }
    

}
