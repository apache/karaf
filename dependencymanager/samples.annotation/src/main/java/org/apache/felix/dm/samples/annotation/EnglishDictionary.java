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

import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.PropertyMetaData;
import org.apache.felix.dm.annotation.api.Service;

/**
 * An English Dictionary Service. We provide here our Properties MetaData in order to let webconsole configure us.
 * You must configure the PID that corresponds to this class through web console in order to activate this service.
 */
@Service(properties={@Property(name="language", value="en")})
public class EnglishDictionary implements DictionaryService
{
    /**
     * The id of our Configuration Admin property key.
     */
    public final static String WORDS = "words";
    
    /**
     * We store all configured words in a thread-safe data structure, because ConfigAdmin
     * may invoke our updated method at any time.
     */
    private CopyOnWriteArrayList<String> m_words = new CopyOnWriteArrayList<String>();
    
    /**
     * Our service will be initialized from ConfigAdmin, so we define here a configuration dependency
     * (by default, our PID is our full class name).
     * @param config The configuration where we'll lookup our words list (key="words").
     */
    @ConfigurationDependency(
        heading="English Dictionary", 
        description = "Configuration for the EnglishDictionary Service",
        metadata={
            @PropertyMetaData(
                heading="English Words",
                description="Declare here some valid english words",
                defaults={"hello", "world"},
                id=EnglishDictionary.WORDS,
                cardinality=Integer.MAX_VALUE)
        }
    )
    protected void updated(Dictionary<String, ?> config) {
        m_words.clear();
        String[] words = (String[]) config.get(WORDS);
        for (String word : words) {
            m_words.add(word);
        }
    }
           
    /**
     * Check if a word exists if the list of words we have been configured from ConfigAdmin/WebConsole.
     */
    public boolean checkWord(String word)
    {
        return m_words.contains(word);
    }
}
