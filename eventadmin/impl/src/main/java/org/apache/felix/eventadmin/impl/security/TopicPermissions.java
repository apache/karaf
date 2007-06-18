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
package org.apache.felix.eventadmin.impl.security;

/**
 * A <tt>TopicPermission</tt> factory. The factory is bound to a specific type (i.e.,
 * either PUBLISH or SUBSCRIBE) and subsequently allows to create new permission
 * objects by providing the topic. Note that the created permission objects most 
 * likely will be cached and that in case that a permission can not be created due
 * to missing security support by the framework (i.e, security is not supported at 
 * all) an instance of <tt>java.lang.Object</tt> will be returned. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface TopicPermissions
{
    /**
     * Get the type (i.e., PUBLISH or SUBSCRIBE) of the permission objects that this
     * factory will create.
     * 
     * @return The type of the permission objects that this factory will create.
     * 
     * @see org.osgi.service.event.TopicPermission#PUBLISH
     * @see org.osgi.service.event.TopicPermission#SUBSCRIBE
     */
    public String getType();
    
    /**
     * This method returns a <tt>TopicPermission</tt> object for the given topic and
     * the type (i.e., PUBLISH or SUBSCRIBE) of this factory. Note that this methods
     * returns an instance of <tt>java.lang.Object</tt> in case that a permission 
     * could not be created due to missing security support by the framework.
     *  
     * @param topic The targeted topic.
     * 
     * @return A <tt>TopicPermission</tt> for the given topic and the type of this
     *      factory or a <tt>java.lang.Object</tt> in case that the permission could 
     *      not be created due to missing security support by the framework.
     *
     * @see org.osgi.service.event.TopicPermission
     * @see org.osgi.service.event.TopicPermission#PUBLISH
     * @see org.osgi.service.event.TopicPermission#SUBSCRIBE
     */
    public Object createTopicPermission(final String topic);
}
