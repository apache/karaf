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
package org.apache.felix.dm.samples.annotation;

import java.util.Dictionary;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.PropertyMetaData;
import org.apache.felix.dm.annotation.api.dependency.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.dependency.ServiceDependency;
import org.osgi.service.log.LogService;

/**
 * This aspect applies to all available Dictionary Services, and checks some custom words, configurable from
 * config admin.
 */
@AspectService( ranking = 10 )
public class DictionaryAspect implements DictionaryService
{
    /**
     * This is the service this aspect is applying to.
     */
    private volatile DictionaryService m_originalDictionary;

    /**
     * We store all configured words in a thread-safe data structure, because ConfigAdmin
     * may invoke our updated method at any time.
     */
    private CopyOnWriteArrayList<String> m_words = new CopyOnWriteArrayList<String>();
    
    /**
     * We'll use the OSGi log service for logging. If no log service is available, then we'll use a NullObject.
     */
    @ServiceDependency(required = false)
    private LogService m_log;

    /**
     * Defines a configuration dependency for retrieving our custo words (by default, our PID is our full class name).
     * This annotation actually provides a ManagedService into the registry, and you can specify meta information regarding
     * all the configuration properties (required by webconsole).
     */
    @ConfigurationDependency(
        heading="Aspect Dictionary", 
        description="Declare here some words to check",
        propagate=false,
        metadata={
            @PropertyMetaData(
                heading="Dictionary aspect words",
                description="Declare here the list of words supported by this dictionary aspect. ",
                defaults={"aspect"},
                id="words",
                cardinality=Integer.MAX_VALUE)
        }
    )
    protected void updated(Dictionary<String, ?> config) {
        m_words.clear();
        String[] words = (String[]) config.get("words");
        for (String word : words) {
            m_words.add(word);
        }
    }

    /**
     * Checks if a word is found from our custom word list. if not, delegate to the decorated
     * dictionary.
     */
    public boolean checkWord(String word)
    {
        m_log.log(LogService.LOG_DEBUG, "DictionaryAspect: checking word " + word);
        if ("aspect".equals(word)) {
            return true;
        }
        return m_originalDictionary.checkWord(word);
    }
}
