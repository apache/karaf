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
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
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
     * No policy.
     */
    public static final int NO_POLICY = 0;
    /**
     * Uses a nullable object.
     */
    public static final int NULLABLE = 1;
    /**
     * Uses a default-implementation object.
     */
    public static final int DEFAULT_IMPLEMENTATION = 2;
    /**
     * Uses an empty array.
     */
    public static final int EMPTY = 3;
    /**
     * Uses {@code null}. 
     */
    public static final int NULL = 4;
    
    /**
     * The handler namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handler.temporal";
    
    /**
     * The list of managed dependencies.
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
     * Stop method. Stops managed dependencies.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_dependencies.size(); i++) {
            ((TemporalDependency) m_dependencies.get(i)).stop();
        }
        m_dependencies.clear();
    }

    /**
     * Configure method. Creates managed dependencies.
     * @param meta the component type metadata.
     * @param dictionary the instance configuration.
     * @throws ConfigurationException if the dependency is not configured correctly
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
            boolean collection = false;
            String spec = fieldmeta.getFieldType();
            if (spec.endsWith("[]")) {
                agg = true;
                spec = spec.substring(0, spec.length() - 2);
            } else if (Collection.class.getName().equals(spec)) {
                agg = true;
                collection = true;
                // Collection detected. Check for the specification attribute
                spec = deps[i].getAttribute("specification");
                if (spec == null) {
                    error("A dependency injected inside a Collection must contain the 'specification' attribute");
                }
            }
            
            String prox = deps[i].getAttribute("proxy");
            boolean proxy = prox != null && prox.equals("true");
            if (proxy && agg) {
                if (! collection) {
                    error("Proxied aggregate temporal dependencies cannot be an array. Only collections are supported");
                }
            }
            
            long timeout = DEFAULT_TIMEOUT;
            if (deps[i].containsAttribute("timeout")) {
                String to = deps[i].getAttribute("timeout");
                if (to.equalsIgnoreCase("infinite")  || to.equalsIgnoreCase("-1")) {
                    timeout = Long.MAX_VALUE; // Infinite wait time ...
                } else {
                    timeout = new Long(deps[i].getAttribute("timeout")).longValue();
                }
            }
            
            int policy = NO_POLICY;
            String di = null;
            String onTimeout = deps[i].getAttribute("onTimeout");
            if (onTimeout != null) {
                if (onTimeout.equalsIgnoreCase("nullable")) {
                    policy = NULLABLE;
                } else if (onTimeout.equalsIgnoreCase("empty-array")  || onTimeout.equalsIgnoreCase("empty")) {
                    policy = EMPTY;
                    if (! agg) {
                        // The empty array policy can only be used on aggregate dependencies
                        error("Cannot use the empty array policy for " + field + " : non aggregate dependency.");
                    }
                } else if (onTimeout.equalsIgnoreCase("null")) {
                    policy = NULL;
                } else if (onTimeout.length() > 0) {
                    di = onTimeout;
                    policy = DEFAULT_IMPLEMENTATION;
                }
            }
         
            Class specification = DependencyModel.loadSpecification(spec, getInstanceManager().getContext());
            TemporalDependency dep = new TemporalDependency(specification, agg, collection, proxy, filter, getInstanceManager().getContext(), timeout, policy, di, this);
            m_dependencies.add(dep);
            
            if (! proxy) { // Register method interceptor only if are not a proxy
                MethodMetadata[] methods = manipulation.getMethods();
                for (int k = 0; k < methods.length; k++) {
                    getInstanceManager().register(methods[k], dep);
                }
            }
            
            getInstanceManager().register(fieldmeta, dep);
        }        
    }

    /**
     * Nothing to do.
     * A temporal dependency is always valid.
     * @param dependencymodel dependency.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#invalidate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void invalidate(DependencyModel dependencymodel) {    }

    /**
     * Nothing to do.
     * A temporal dependency is always valid.
     * @param dependencymodel dependency.
     * @see org.apache.felix.ipojo.util.DependencyStateListener#validate(org.apache.felix.ipojo.util.DependencyModel)
     */
    public void validate(DependencyModel dependencymodel) {    }
    

}
