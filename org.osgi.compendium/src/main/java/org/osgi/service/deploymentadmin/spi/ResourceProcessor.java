/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

package org.osgi.service.deploymentadmin.spi;

import java.io.InputStream;

/**
  * ResourceProcessor interface is implemented by processors handling resource files
  * in deployment packages. Resource Processors expose their services as standard OSGi services.
  * Bundles exporting the service may arrive in the deployment package (customizers) or may be 
  * preregistered (they are installed prevoiusly). Resource processors has to define the 
  * <code>service.pid</code> standard OSGi service property which should be a unique string.<p>
  * 
  * The order of the method calls on a particular Resource Processor in case of install/update 
  * session is the following:<p>
  * 
  * <ol>
  * 	<li>{@link #begin(DeploymentSession)}</li>
  * 	<li>{@link #process(String, InputStream)} calls till there are resources to process 
  * 		or {@link #rollback()} and the further steps are ignored</li>
  * 	<li>{@link #dropped(String)} calls till there are resources to drop
  * 	<li>{@link #prepare()}</li>
  * 	<li>{@link #commit()} or {@link #rollback()}</li>
  * </ol>
  * 
  * The order of the method calls on a particular Resource Processor in case of uninstall 
  * session is the following:<p>
  * 
  * <ol>
  * 	<li>{@link #begin(DeploymentSession)}</li>
  * 	<li>{@link #dropAllResources()}	or {@link #rollback()} and the further steps are ignored</li>
  * 	<li>{@link #prepare()}</li>
  * 	<li>{@link #commit()} or {@link #rollback()}</li>
  * </ol>
  */
public interface ResourceProcessor {

	/**
	  * Called when the Deployment Admin starts a new operation on the given deployment package, 
	  * and the resource processor is associated a resource within the package. Only one 
	  * deployment package can be processed at a time.
	  * 
	  * @param session object that represents the current session to the resource processor
	  * @see DeploymentSession
	  */
    void begin(DeploymentSession session);
  
    /**
     * Called when a resource is encountered in the deployment package for which this resource 
     * processor has been  selected to handle the processing of that resource.
     * 
     * @param name The name of the resource relative to the deployment package root directory. 
     * @param stream The stream for the resource. 
     * @throws ResourceProcessorException if the resource cannot be processed. Only 
     *         {@link ResourceProcessorException#CODE_RESOURCE_SHARING_VIOLATION} and 
     *         {@link ResourceProcessorException#CODE_OTHER_ERROR} error codes are allowed.
     */
    void process(String name, InputStream stream) throws ResourceProcessorException;

	/**
	  * Called when a resource, associated with a particular resource processor, had belonged to 
	  * an earlier version of a deployment package but is not present in the current version of 
	  * the deployment package.  This provides an opportunity for the processor to cleanup any 
	  * memory and persistent data being maintained for the particular resource.  
	  * This method will only be called during "update" deployment sessions.
	  * 
	  * @param resource the name of the resource to drop (it is the same as the value of the 
	  *        "Name" attribute in the deployment package's manifest)
	  * @throws ResourceProcessorException if the resource is not allowed to be dropped. Only the 
	  *         {@link ResourceProcessorException#CODE_OTHER_ERROR} error code is allowed
	  */
    void dropped(String resource) throws ResourceProcessorException;
    
    /**
     * This method is called during an "uninstall" deployment session.
     * This method will be called on all resource processors that are associated with resources 
     * in the deployment package being uninstalled. This provides an opportunity for the processor 
     * to cleanup any memory and persistent data being maintained for the deployment package.
     * 
     * @throws ResourceProcessorException if all resources could not be dropped. Only the 
     *         {@link ResourceProcessorException#CODE_OTHER_ERROR} is allowed.
     */
    void dropAllResources() throws ResourceProcessorException;
  
    /**
     * This method is called on the Resource Processor immediately before calling the 
     * <code>commit</code> method. The Resource Processor has to check whether it is able 
     * to commit the operations since the last <code>begin</code> method call. If it determines 
     * that it is not able to commit the changes, it has to raise a 
     * <code>ResourceProcessorException</code> with the {@link ResourceProcessorException#CODE_PREPARE} 
     * error code.
     * 
     * @throws ResourceProcessorException if the resource processor is able to determine it is 
     *         not able to commit. Only the {@link ResourceProcessorException#CODE_PREPARE} error 
     *         code is allowed.
     */
    void prepare() throws ResourceProcessorException;        
   
    /**
     * Called when the processing of the current deployment package is finished. 
     * This method is called if the processing of the current deployment package was successful, 
     * and the changes must be made permanent.
     */
    void commit();
   
     
    /**
     * Called when the processing of the current deployment package is finished. 
     * This method is called if the processing of the current deployment package was unsuccessful, 
     * and the changes made during the processing of the deployment package should be removed.  
     */
    void rollback();
    
    /**
     * Processing of a resource passed to the resource processor may take long. 
     * The <code>cancel()</code> method notifies the resource processor that it should 
     * interrupt the processing of the current resource. This method is called by the 
     * <code>DeploymentAdmin</code> implementation after the
     * <code>DeploymentAdmin.cancel()</code> method is called.
     */
    void cancel();

}
