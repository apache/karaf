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
package org.apache.felix.eventadmin.impl.handler;

import org.apache.felix.eventadmin.impl.util.CacheMap;
import org.osgi.service.event.EventConstants;

/**
 * The factory for <tt>EventHandler</tt> filters based on a certain topic. This
 * implementation uses a cache to speed-up filter creation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CacheTopicHandlerFilters implements TopicHandlerFilters
{
    // The cache
    private final CacheMap m_cache;
    
    private final char[] m_keyChars = EventConstants.EVENT_TOPIC.toCharArray();
    
    private final char[] m_filterStart;
    
    /**
     * The constructor of the filter factory. 
     * 
     * @param cache The cache to use in order to speed-up filter creation.
     * 
     * @param requireTopic Include handlers that do not provide a topic
     */
    public CacheTopicHandlerFilters(final CacheMap cache, final boolean requireTopic)
    {
        if(null == cache)
        {
            throw new NullPointerException("Cache may not be null");
        }
        
        m_cache = cache;
        
        m_filterStart = ("(|" + 
            ((requireTopic) ? "" : "(!(" + new String(m_keyChars) + "=*))") +
            "(" + new String(m_keyChars) + "=\\*)(" + new String(m_keyChars) + 
            "=").toCharArray();
    }
    
    /**
     * Create a filter that will match all <tt>EventHandler</tt> services that match
     * the given topic.
     * 
     * @param topic The topic to match
     * 
     * @return A filter that will match all <tt>EventHandler</tt> services for 
     *      the given topic.
     *      
     * @see org.apache.felix.eventadmin.impl.handler.TopicHandlerFilters#createFilterForTopic(java.lang.String)
     */
    public String createFilterForTopic(String topic)
    {
        // build the ldap-query - as a simple example: 
        // topic=org/apache/felix/TEST
        // result = (|(topic=\*)(topic=org/\*)(topic=org/apache/\*)
        //            (topic=org/apache/felix/\*)(topic=org/apache/felix/TEST))
        String result = (String) m_cache.get(topic);
        
        if(null == result)
        {
            char[] topicChars = topic.toCharArray();

            final StringBuffer filter = new StringBuffer(topicChars.length
                * topicChars.length);

            filter.append(m_filterStart);

            for (int i = 0; i < topicChars.length; i++)
            {
                if ('/' == topicChars[i])
                {
                    filter.append('/').append('\\').append('*').append(')');

                    filter.append('(').append(m_keyChars).append('=').append(
                        topicChars, 0, i + 1);
                }
                else
                {
                    filter.append(topicChars[i]);
                }
            }

            filter.append(')').append(')');

            result = filter.toString();
            
            m_cache.add(topic, result);
        }
        
        return result;
    }
}
