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

import org.apache.felix.eventadmin.impl.util.CacheMap;

/**
 * An implementation of the <tt>TopicPermissions</tt> factory that uses a given
 * cache in order to speed-up topic permission creation. Note that a 
 * <tt>java.lang.Object</tt> is returned in case creating a new TopicPermission 
 * fails. This assumes that Bundle.hasPermission is used in order to evaluate the
 * created Permission which in turn will return true if security is not supported
 * by the framework. Otherwise, it will return false due to receiving something that
 * is not a subclass of <tt>java.lang.SecurityPermission</tt> hence, this combination
 * ensures that access is granted in case a topic permission could not be created due
 * to missing security support by the framework. 
 *  
 * @see org.apache.felix.eventadmin.impl.security.TopicPermissions
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CacheTopicPermissions implements TopicPermissions
{
    // The cache used
    private final CacheMap m_cache;
    
    // The type of the permissions created
    private final String m_type;
    
    /**
     * The constructor of this permission factory. The given cache will be used to
     * speed-up permission creation and the created permissions will be of the given
     * type (i.e., PUBLISH or SUBSCRIBE).
     * 
     * @param cache The cache to be used
     * @param type The type that created permissions will be of (i.e, PUBLISH or
     *      SUBSCRIBE)
     *      
     * @see org.apache.felix.eventadmin.impl.security.TopicPermissions
     * @see org.osgi.service.event.TopicPermission#PUBLISH
     * @see org.osgi.service.event.TopicPermission#SUBSCRIBE
     */
    public CacheTopicPermissions(final CacheMap cache, final String type)
    {
        checkNull(cache, "CacheMap");
        checkNull(type, "Type");
       
        if(!org.osgi.service.event.TopicPermission.PUBLISH.equals(type) && 
            !org.osgi.service.event.TopicPermission.SUBSCRIBE.equals(type))
        {
            throw new IllegalArgumentException(
                "Type must be either PUBLISH or SUBSCRIBE");
        }
        
        m_cache = cache;
        
        m_type = type;
    }

    /**
     * Returns the type of the permissions created by this factory.
     * 
     * @return The type of the permissions created by this factory
     * 
     * @see org.apache.felix.eventadmin.impl.security.TopicPermissions#getType()
     * @see org.osgi.service.event.TopicPermission#PUBLISH
     * @see org.osgi.service.event.TopicPermission#SUBSCRIBE
     */
    public String getType()
    {
        return m_type;
    }

    /**
     * Creates a <tt>TopicPermission</tt> for the given topic and the type of this
     * factory (i.e., PUBLISH or SUBSCRIBE). Note that a 
     * <tt>java.lang.Object</tt> is returned in case creating a new TopicPermission 
     * fails. This assumes that Bundle.hasPermission is used in order to evaluate the
     * created Permission which in turn will return true if security is not supported
     * by the framework. Otherwise, it will return false due to receiving something 
     * that is not a subclass of <tt>java.lang.SecurityPermission</tt> hence, this 
     * combination ensures that access is granted in case a topic permission could 
     * not be created due to missing security support by the framework.
     * 
     * @param topic The target topic
     * 
     * @return The created permission or a <tt>java.lang.Object</tt> in case the
     *      permission could not be created.
     *      
     * @see org.apache.felix.eventadmin.impl.security.TopicPermissions#createTopicPermission(String)
     * @see org.osgi.service.event.TopicPermission
     */
    public Object createTopicPermission(final String topic)
    {
        Object result = m_cache.get(topic);
        
        if(null == result)
        {
            try
            {
                result = new org.osgi.service.event.TopicPermission(topic, m_type);
            } catch (Throwable t)
            {
                // This might happen in case security is not supported
                // Bundle.hasPermission will return true in this case
                // hence topicPermission = new Object() is o.k.
                
                result = new Object();
            }
            
            m_cache.add(topic, result);
        }
        
        return result;
    }
    
    /*
     * This is a utility method that will throw a <tt>NullPointerException</tt>
     * in case that the given object is null. The message will be of the form name +
     * may not be null.
     */
    private void checkNull(final Object object, final String name)
    {
        if(null == object)
        {
            throw new NullPointerException(name + " may not be null");
        }
    }
}
