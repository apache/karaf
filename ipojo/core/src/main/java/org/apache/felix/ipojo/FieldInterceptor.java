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

/**
* Field interceptor.
* A class implementing this interface is able to be notified of field accesses.
* The listener need to be register on the instance manager. 
* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public interface FieldInterceptor {
    
    /**
     * This method is called when a PUTFIELD operation is detected.
     * @param pojo : the pojo object setting the value
     * @param fieldName : the field name
     * @param value : the value passed to the field
     */
    void onSet(Object pojo, String fieldName, Object value);

    /**
     * This method is called when a GETFIELD operation is detected.
     * @param pojo : the pojo object getting the value
     * @param fieldName : the field name
     * @param value : the value passed to the field (by the previous call)
     * @return : the managed value of the field
     */
    Object onGet(Object pojo, String fieldName, Object value);

}
