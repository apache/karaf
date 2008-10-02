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

import java.lang.reflect.Method;

/**
* Method interceptor.
* A class implementing this interface is able to be notified of method invocations (
* i.e. entries, exits, and errors).
* The listener needs to be register on the instance manager with the 
* {@link InstanceManager#register(org.apache.felix.ipojo.parser.MethodMetadata, MethodInterceptor)}
* method. 
* Events are sent before the method entry (onEntry), after the method returns (onExit), 
* when an error is thrown by the method (onError), and before the after either a returns or an error (onFinally)
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface MethodInterceptor {
    
    /**
     * This method is called when the execution enters in a method.
     * @param pojo the pojo on which the method is called.
     * @param method the invoked method.
     * @param args the arguments array.
     */
    void onEntry(Object pojo, Method method, Object[] args);

    /**
     * This method is called when the execution exits a method :
     * before a <code>return</code>.
     * If the given returned object is <code>null</code>, either the method is 
     * <code>void</code>, or it returns <code>null</code>.
     * You must not modified the returned object.
     * @param pojo the pojo on which the method exits.
     * @param method the exiting method.
     * @param returnedObj the the returned object (boxed for primitive type)
     */
    void onExit(Object pojo, Method method, Object returnedObj);
    
    /**
     * This method is called when the execution throws an exception in the given 
     * method.
     * @param pojo the pojo on which the method was accessed.
     * @param method the invoked method.
     * @param throwable the thrown exception
     */
    void onError(Object pojo, Method method, Throwable throwable);
    
    /**
     * This method is called when the execution of a method is going to terminate : 
     * just before to throw an exception or before to return.
     * (onError or onExit was already called).
     * @param pojo the pojo on which the method was accessed.
     * @param method the invoked method.
     */
    void onFinally(Object pojo, Method method);

}
