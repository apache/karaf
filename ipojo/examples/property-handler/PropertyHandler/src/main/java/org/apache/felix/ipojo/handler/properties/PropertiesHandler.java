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
package org.apache.felix.ipojo.handler.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

/**
 * This handler load a properties file containing property value. The handler injects this values inside fields. When stopping the handler stores
 * updated value inside the file. The properties file contains [field-name: field-value] (field-value are strings) Metadata :
 * &lt;example.handler.properties:properties file="file-path"&gt; Instances can override file locations by setting the properties.file property.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PropertiesHandler extends PrimitiveHandler {
    
    /** The Handler NAMESPACE. */
    private final String NAMESPACE = "example.handler.properties";

    /** Properties file. */
    private Properties m_properties = new Properties();

    /** File location. */
    private String m_file;

    /**
     * This method is the first to be invoked. This method aims to configure the handler. It receives the component type metadata and the instance
     * description. The method parses given metadata and register field to listen. Step 3 : when the instance configuration contains the
     * properties.file property, it overrides the properties file location.
     * @param metadata : component type metadata
     * @param configuration : instance description
     * @throws ConfigurationException : the configuration of the handler has failed.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("unchecked")
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        // Parse metadata to get <properties file="$file"/>

        Element[] elem = metadata.getElements("properties", NAMESPACE); // Get all example.handler.properties:properties element

        switch (elem.length) {
            case 0:
                // No matching element in metadata, throw a configuration error.
                throw new ConfigurationException("No properties found");
            case 1:
                // One 'properties' found, get attributes.
                m_file = elem[0].getAttribute("file");
                if (m_file == null) {
                    // if file is null, throw a configuration error.
                    throw new ConfigurationException("Malformed properties element : file attribute must be set");
                }
                break;
            default:
                // To simplify we handle only one properties element.
                throw new ConfigurationException("Only one properties element is supported");
        }

        // Look if the instance overrides file location :
        String instanceFile = (String) configuration.get("properties.file");
        if (instanceFile != null) {
            m_file = instanceFile;
        }

        // Load properties
        try {
            loadProperties();
        } catch (IOException e) {
            throw new ConfigurationException("Error when reading the " + m_file + " file : " + e.getMessage());
        }

        // Register fields
        // By convention, properties file entry are field name, so look for each property to get field list.

        //First get Pojo Metadata metadata :
        PojoMetadata pojoMeta = getPojoMetadata();
        Enumeration e = m_properties.keys();
        while (e.hasMoreElements()) {
            String field = (String) e.nextElement();
            FieldMetadata fm = pojoMeta.getField(field);

            if (fm == null) { // The field does not exist
                throw new ConfigurationException("The field " + field + " is declared in the properties file but does not exist in the pojo");
            }

            // Then check that the field is a String field
            if (!fm.getFieldType().equals(String.class.getName())) {
                throw new ConfigurationException("The field " + field + " exists in the pojo, but is not a String");
            }

            // All checks are ok, register the interceptor.
            getInstanceManager().register(fm, this);
        }

        // Finally register the field to listen 
    }

    /**
     * This method is called when the instance start (after the configure method). We just print stored properties.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        // The properties are already loaded (in the configure method), just print values.
        m_properties.list(System.out);
    }

    /**
     * This method is called when the instance stops. We save the properties to not lost the instance state and clear the stored properties.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        try {
            saveProperties();
        } catch (IOException e) {
            error("Cannot read the file : " + m_file, e); // Log an error message by using the iPOJO logger
        }
        m_properties = null;
    }

    /**
     * This method is called at each time the pojo 'get' a listened field. The method return the stored value.
     * @param pojo : pojo object getting the field
     * @param field : field name.
     * @param o : previous value.
     * @return the stored value.
     * @see org.apache.felix.ipojo.PrimitiveHandler#getterCallback(java.lang.String, java.lang.Object)
     */
    public Object onGet(Object pojo, String field, Object o) {
        // When the pojo requires a value for a managed field, this method is invoked.
        // So, we have just to return the stored value.
        return m_properties.get(field);
    }

    /**
     * This method is called at each time the pojo 'set' a listened field. This method updates the local properties.
     * @param pojo : pojo object setting the field
     * @param field : field name
     * @param newvalue : new value
     * @see org.apache.felix.ipojo.PrimitiveHandler#setterCallback(java.lang.String, java.lang.Object)
     */
    public void onSet(Object pojo, String field, Object newvalue) {
        // When the pojo set a value to a managed field, this method is invoked.
        // So, we update the stored value.
        m_properties.put(field, newvalue);
    }

    /**
     * Step 2 : state properties when the instance becomes invalid.
     * @param newState : the instance state
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int newState) {
        // This method is invoked each times that the instance state changed.

        // If the new state is invalid, save the properties.
        if (newState == ComponentInstance.INVALID) {
            // Reload properties
            try {
                saveProperties();
            } catch (IOException e) {
                error("Cannot read the file : " + m_file, e); // Log an error message by using the iPOJO logger
            }
            return;
        }
    }

    /**
     * Step 5 : dynamic reconfiguration. This method is call when the instance is reconfigured externally. The given property contains property value.
     * @param dict : new properties
     * @see org.apache.felix.ipojo.Handler#reconfigure(java.util.Dictionary)
     */
    @SuppressWarnings("unchecked")
    public synchronized void reconfigure(Dictionary dict) {
        // For each property, look if a new value is contained in the new configuration.
        Enumeration e = m_properties.keys();
        while (e.hasMoreElements()) {
            String field = (String) e.nextElement();
            String value = (String) dict.get(field);
            // If the dictionary contains a value, update the stored value.
            if (value != null) {
                m_properties.put(field, value);
            }
        }
    }

    /**
     * Returns handler description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        return new Description(this);
    }

    /**
     * Helper method just loading the properties.
     * @throws IOException : the file cannot be read.
     */
    private void loadProperties() throws IOException {
        // Load the properties file from file system
        File file = new File(m_file);
        InputStream is = new FileInputStream(file);
        m_properties.load(is);
    }

    /**
     * Helper method writing properties.
     * @throws IOException : the file cannot be written.
     */
    private void saveProperties() throws IOException {
        // Store the file, modified the last modification date.
        File file = new File(m_file);
        OutputStream os = new FileOutputStream(file);
        m_properties.store(os, "");
    }

    /**
     * Step 3 : The handler will participate to the instance architecture. 
     * This class describing the handler.
     */
    private class Description extends HandlerDescription {

        /**
         * Instantiates a new description.
         * @param h the h
         */
        public Description(PrimitiveHandler h) {
            super(h);
        }

        /**
         * This method must return the Element describing the handler. The description of this handler contains the list of properties with attached
         * value.
         * @return the description of the handler.
         * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
         */
        @SuppressWarnings("unchecked")
        public Element getHandlerInfo() {
            Element elem = super.getHandlerInfo(); // This method must be called to get the root description element.
            Enumeration e = m_properties.keys();
            while (e.hasMoreElements()) {
                String field = (String) e.nextElement();
                Element prop = new Element("property", ""); // Create an element for the actual property.
                // Add two attribute (the field and the value).
                prop.addAttribute(new Attribute("field", field));
                prop.addAttribute(new Attribute("value", (String) m_properties.get(field)));
                elem.addElement(prop); // Attach the current element to the root element.
            }
            return elem;
        }

    }
}
