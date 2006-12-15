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
package org.apache.felix.ipojo.handlers.dependency;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.handlers.dependency.nullable.NullableObjectWriter;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;

/**
 * The dependency handler manages a list of dependencies.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class DependencyHandler extends Handler {

    /**
     * The instance manager using this handler.
     */
    private InstanceManager m_manager;

    /**
     * List of depednencies of the component.
     */
    private Dependency[] m_dependencies = new Dependency[0];

    /**
     * List of nullable class for optional dependencies.
     */
    private Class[] m_nullableClasses = new Class[0];

    /**
     * Classloader to use to load nullable classes.
     */
    private NullableClassloader m_classloader;

    /**
     * State of the handler.
     */
    private int m_state;

//  ===================== Fields getters & setters =====================

    /**
     * Add a dependency.
     * @param dep : the dependency to add
     */
    private void addDependency(Dependency dep) {
        for (int i = 0; (m_dependencies != null) && (i < m_dependencies.length); i++) {
            if (m_dependencies[i] == dep) {
                return;
            }
        }
        if (m_dependencies.length > 0) {
            Dependency[] newDep = new Dependency[m_dependencies.length + 1];
            System.arraycopy(m_dependencies, 0, newDep, 0, m_dependencies.length);
            newDep[m_dependencies.length] = dep;
            m_dependencies = newDep;
        }
        else {
            m_dependencies = new Dependency[] {dep};
        }
    }

    /**
     * Add a nullable class.
     * @param clazz : the class to add
     */
    private void addNullableClass(Class clazz) {
        for (int i = 0; (m_nullableClasses != null) && (i < m_nullableClasses.length); i++) {
            if (m_nullableClasses[i] == clazz) {
                return;
            }
        }
        if (m_nullableClasses.length > 0) {
            Class[] newClass = new Class[m_nullableClasses.length + 1];
            System.arraycopy(m_nullableClasses, 0, newClass, 0, m_nullableClasses.length);
            newClass[m_nullableClasses.length] = clazz;
            m_nullableClasses = newClass;
        }
        else {
            m_nullableClasses = new Class[] {clazz};
        }
    }

    /**
     * @return the dependency list
     */
    public Dependency[] getDependencies() { return m_dependencies; }

    /**
     * @return the instance manager
     */
    protected InstanceManager getInstanceManager() { return m_manager; }

//  ===================== Handler implementation =====================

    /**
     * Check the validity of the dependencies.
     */
    protected void checkContext() {

        synchronized (this) {

            m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Check Context ...");

            // Store the initial state
            int initialState = m_state;

            // Check the component dependencies
            if (!validateComponentDependencies()) {
                // The dependencies are not valid
                if (initialState == InstanceManager.VALID) {
                    //There is a state change
                    m_state = InstanceManager.INVALID;
                    m_manager.checkInstanceState();
                }
                // Else do nothing, the component state stay UNRESOLVED
            }
            else {
                // The dependencies are valid
                if (initialState == InstanceManager.INVALID) {
                    //There is a state change
                    m_state = InstanceManager.VALID;
                    m_manager.checkInstanceState();
                }
                // Else do nothing, the component state stay VALID
            }

        }
    }

    /**
     * Check if the dependency given is valid in the sense that metadata are consistent.
     * @param dep : the dependency to check
     * @return true if the dependency is valid
     */
    private boolean checkDependency(Dependency dep, Element manipulation) {
        // Check the internal type of dependency
        String field = dep.getMetadata().getField();

        String type = null;
        for (int i = 0; i < manipulation.getElements("Field").length; i++) {
            if (field.equals(manipulation.getElements("Field")[i].getAttribute("name"))) {
                type = manipulation.getElements("Field")[i].getAttribute("type");
                break;
            }
        }

        if (type == null) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[DependencyHandler on " + m_manager.getClassName() + "] A declared dependency was not found in the class : " + dep.getMetadata().getField());
            return false;
        }

        if (type != null) {
            if (type.endsWith("[]")) {
                // Set the dependency to multiple
                dep.getMetadata().setMultiple();
                type = type.substring(0, type.length() - 2);
            }

            if (dep.getMetadata().getServiceSpecification() == null) { dep.getMetadata().setServiceSpecification(type); }

            if (!dep.getMetadata().getServiceSpecification().equals(type)) {
                m_manager.getFactory().getLogger().log(Logger.WARNING, "[DependencyHandler on " + m_manager.getClassName() + "] The field type [" + type + "] and the needed service interface [" + dep.getMetadata().getServiceSpecification() + "] are not the same");
                dep.getMetadata().setServiceSpecification(type);
            }
        }
        else {
            m_manager.getFactory().getLogger().log(Logger.WARNING, "[DependencyHandler on " + m_manager.getClassName() + "] The declared dependency " + dep.getMetadata().getField() + "  does not exist in the code");
        }
        return true;
    }


    /**
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element)
     */
    public void configure(InstanceManager im, Element componentMetadata, Dictionary configuration) {
        m_manager = im;
        m_dependencies = new Dependency[0];
        m_nullableClasses = new Class[0];

        // Create the dependency according to the component metadata
        Element[] deps = componentMetadata.getElements("Dependency");
        for (int i = 0; i < deps.length; i++) {
            // Create the dependency metadata
            String field = deps[i].getAttribute("field");
            String serviceSpecification = null;
            if (deps[i].containsAttribute("interface")) { serviceSpecification = deps[i].getAttribute("interface"); }
            String filter = "";
            if (deps[i].containsAttribute("filter")) { filter = deps[i].getAttribute("filter"); }
            boolean optional = false;
            if (deps[i].containsAttribute("optional") && deps[i].getAttribute("optional").equals("true")) { optional = true; }
            DependencyMetadata dm = new DependencyMetadata(field, serviceSpecification, filter, optional);


            Dependency dep = new Dependency(this, dm);
            // Check the dependency :
            Element manipulation = componentMetadata.getElements("Manipulation")[0];
            if (checkDependency(dep, manipulation)) { addDependency(dep); }
            else { m_manager.getFactory().getLogger().log(Logger.ERROR, "[DependencyHandler on " + m_manager.getClassName() + "] The dependency on " + dep.getMetadata().getField() + " is not valid"); }

            // Look for dependency callback :
            for (int j = 0; j < (deps[i].getElements("Callback", "")).length; j++) {
                String method = deps[i].getElements("Callback", "")[j].getAttribute("method");
                String type = deps[i].getElements("Callback", "")[j].getAttribute("type");
                int methodType = 0;
                if (type.equals("bind")) { methodType = DependencyCallback.BIND; }
                else { methodType = DependencyCallback.UNBIND; }
                boolean isStatic = false;
                if (deps[i].getElements("Callback", "")[j].containsAttribute("isStatic") && deps[i].getElements("Callback", "")[j].getAttribute("isStatic").equals("true")) { isStatic = true; }
                DependencyCallback dc = new DependencyCallback(dep, method, methodType, isStatic);
                dep.getMetadata().addDependencyCallback(dc);
            }
        }

        if (deps.length > 0) {
            String[] fields = new String[m_dependencies.length];
            for (int k = 0; k < m_dependencies.length; k++) {
                fields[k] = m_dependencies[k].getMetadata().getField();
            }
            m_manager.register(this, fields);

            // Create the nullable classloader
            // TODO why do not use the factory class loader ?
            m_classloader = new NullableClassloader(m_manager.getContext().getBundle());

        }
    }

    /**
     * Create a nullable class for the given dependency.
     * @param dep : the dependency
     */
    private void createNullableClass(Dependency dep) {
        m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Try to load the nullable class for " + dep.getMetadata().getServiceSpecification());
        // Try to load the nullable object :
        String[] segment = dep.getMetadata().getServiceSpecification().split("[.]");
        String className = "org/apache/felix/ipojo/" + segment[segment.length - 1] + "Nullable";

        String resource = dep.getMetadata().getServiceSpecification().replace('.', '/') + ".class";
        URL url =  m_manager.getContext().getBundle().getResource(resource);

        try {
            byte[] b = NullableObjectWriter.dump(url,  dep.getMetadata().getServiceSpecification());

//          // DEBUG :
//          try {
//          File file = new File("P:\\workspace\\iPOJO\\adapted\\" + className.replace('/', '.') + ".class");
//          file.createNewFile();
//          FileOutputStream fos = new FileOutputStream(file);
//          fos.write(b);
//          fos.close();
//          } catch (Exception e3) {
//          System.err.println("Problem to write the adapted class on the file system  : " + e3.getMessage());

//          }

            addNullableClass(m_classloader.defineClass(className.replace('/', '.'), b, null));
            m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Nullable class created for " + dep.getMetadata().getServiceSpecification());

        } catch (IOException e1) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[DependencyHandler on " + m_manager.getClassName() + "] Cannot open a stream of an interface to generate the nullable class for " + dep.getMetadata().getServiceSpecification(), e1);
        } catch (Exception e2) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[DependencyHandler on " + m_manager.getClassName() + "] Cannot load the nullable class for  " + dep.getMetadata().getServiceSpecification(), e2);
        }
    }

    /**
     * Return the nullable class corresponding to the given name.
     * @param name the needed type
     * @return the class correspondig to the name, or null if the class does not exist.
     */
    protected Class getNullableClass(String name) {
        for (int i = 0; i < m_nullableClasses.length; i++) {
            Class c = m_nullableClasses[i];
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String, java.lang.Object)
     */
    public Object getterCallback(String fieldName, Object value) {
        //TODO : non effiecent
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            if (dep.getMetadata().getField().equals(fieldName)) {
                // The field name is a dependency, return the get
                return dep.get();
            }
        }
        // Else return the value
        return value;
    }

    /**
     * @see org.apache.felix.ipojo.Handler#isValid()
     */
    public boolean isValid() {
        return (m_state == InstanceManager.VALID);
    }

    /**
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Start the dependency handler");

        // Start the dependencies, for optional dependencies create Nullable class
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            if (dep.getMetadata().isOptional() && !dep.getMetadata().isMultiple()) { createNullableClass(dep); }
            dep.start();
        }
        // Check the state
        m_state = m_manager.getState();
        checkContext();
    }

    /**
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) { m_state = state; }

    /**
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_dependencies.length; i++) { m_dependencies[i].stop(); }
    }

    /**
     * @see org.apache.felix.ipojo.Handler#createInstance(java.lang.Object)
     */
    public void createInstance(Object instance) {
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i].callBindMethod(instance);
        }
    }

    /**
     * @return true if all dependencies are resolved.
     */
    private boolean validateComponentDependencies() {
        boolean valide = true;
        for (int i = 0; i < m_dependencies.length; i++) {
            Dependency dep = m_dependencies[i];
            valide = valide & dep.isSatisfied();
            if (!valide) {
                m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Component Dependencies are not valid : " + dep.getMetadata().getServiceSpecification());
                return false;
            }
        }
        m_manager.getFactory().getLogger().log(Logger.INFO, "[DependencyHandler on " + m_manager.getClassName() + "] Component Dependencies are valid");
        return valide;
    }

}
