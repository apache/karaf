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
package org.apache.felix.ipojo.task;

import java.io.File;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * iPOJO Ant Task. This Ant task manipulates an input bundle.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class IPojoTask extends Task {
    
    /** Metadata file. */
    private File m_metadata;
    
    /** Input bundle. */
    private File m_input;
    
    /** Output bundle. */
    private File m_output;

    /** Flag describing if we need to ignore annotation of not. */
    private boolean m_ignoreAnnotations = false;
    
    /**
     * Set the metadata file.
     * @param meta : the metadata file.
     */
    public void setMetadata(File meta) {
        m_metadata = meta;
    }
    
    /**
     * Set the input bundle.
     * @param in : the input bundle
     */
    public void setInput(File in) {
        m_input = in;
    }
    
    /**
     * Set the output bundle.
     * @param out : the output bundle
     */
    public void setOutput(File out) {
        m_output = out;
    }
    
    /**
     * Set if we need to ignore annotations or not.
     * @param flag : true if we need to ignore annotations.
     */
    public void setIgnoreAnnotations(boolean flag) {
        m_ignoreAnnotations = flag;
    }
    
    /**
     * Execute the Ant Task.
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() {
        
        if (m_input == null) {
            throw new BuildException("No input bundle specified");
        }
        if (!m_input.exists()) {
            throw new BuildException("The input bundle " + m_input.getAbsolutePath() + " does not exist");
        }
        
        log("Input Bundle File : " + m_input.getAbsolutePath());
        
        // Get metadata file
        if (m_metadata == null) {
            m_metadata = new File("./metadata.xml");
            if (!m_metadata.exists()) {
             // Verify if annotations are ignored
                if (m_ignoreAnnotations) {
                    log("No metadata file found & ignore annotations : nothing to do");
                    return;
                } else {
                    log("No metadata file found - try to use only annotations");
                    m_metadata = null;
                }
            } else {
                log("Metadata File : " + m_metadata.getAbsolutePath());
            }
        } else {
            // Metadata file is specified, check existence
            if (!m_metadata.exists()) {
                throw new BuildException("No metadata file found - the file " + m_metadata.getAbsolutePath() + " does not exist");
            } else {
                log("Metadata File : " + m_metadata.getAbsolutePath());
            }
        }

        log("Start bundle manipulation");
        
        if (m_output == null) {
            m_output = new File("./_out.jar");
        }
        
        if (m_output.exists()) {
            boolean r = m_output.delete();
            if (!r) { throw new BuildException("The file " + m_output.getAbsolutePath() + " cannot be deleted"); }
        }
        
        Pojoization pojo = new Pojoization();
        if (! m_ignoreAnnotations) {
            pojo.setAnnotationProcessing();
        }
        pojo.pojoization(m_input, m_output, m_metadata);
        for (int i = 0; i < pojo.getWarnings().size(); i++) {
            log((String) pojo.getWarnings().get(i));
        }
        if (pojo.getErrors().size() > 0) { throw new BuildException((String) pojo.getErrors().get(0)); }
        
        String out;
        if (m_output.getName().equals("_out.jar")) {
            m_input.delete();
            m_output.renameTo(m_input);
            out = m_input.getAbsolutePath();
        } else {
            out = m_output.getAbsolutePath();
        }
        
        log("Bundle manipulation - SUCCESS");
        log("Output File : " + out);
        
    }
    
    

}

