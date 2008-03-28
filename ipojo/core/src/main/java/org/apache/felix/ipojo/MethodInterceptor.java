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
* A class implementing this interface is able to be notified of method invocation.
* The listener need to be register on the instance manager. 
* For event are send to the listener : before the method entry, after the method returns, 
* when an error is thrown by the method, and before the after either a returns or an error (finally) 
* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface MethodInterceptor {
    
    /**
     * This method is called when the execution enter in a method.
     * @param pojo : pojo on which the method is called.
     * @param method : method invoked.
     * @param args arguments array.
     */
    void onEntry(Object pojo, Method method, Object[] args);

    /**
     * This method is called when the execution exit a method (before a return or a throw).
     * If the given returned object is null, either the method is void, either it returns null.
     * You must not modified the returned object.
     * @param pojo : the pojo on which the method exits.
     * @param method : exiting method.
     * @param returnedObj : the returned object (boxed for primitive type)
     */
    void onExit(Object pojo, Method method, Object returnedObj);
    
    /**
     * This method is called when the execution throw an exception in the given method.
     * @param pojo : the pojo on which the method was accessed.
     * @param method : invoked method.
     * @param throwable : the thrown exception
     */
    void onError(Object pojo, Method method, Throwable throwable);
    
    /**
     * This method is called when the execution of a method will terminate : 
     * just before to throw an exception or before to return.
     * OnError or OnExit was already called.
     * @param pojo : the pojo on which the method was accessed.
     * @param method : invoked method.
     */
    void onFinally(Object pojo, Method method);

}
