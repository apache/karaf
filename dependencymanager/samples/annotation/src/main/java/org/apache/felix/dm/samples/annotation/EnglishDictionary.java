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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.Service;

/**
 * An English Dictionary Service. We'll be configured using OSGi Config Admin.
 */
@Service(properties={@Param(name="language", value="en")})
public class EnglishDictionary implements DictionaryService
{
    private CopyOnWriteArrayList<String> m_words = new CopyOnWriteArrayList<String>();
    
    /**
     * Our service will be initialized from ConfigAdmin, so we define here a configuration dependency
     * (by default, our PID is our full class name).
     * @param config The configuration where we'll lookup our words list (key="words").
     */
    @ConfigurationDependency
    protected void updated(Dictionary<String, ?> config) {
        m_words.clear();
        List<String> words = (List<String>) config.get("words");
        for (String word : words) {
            m_words.add(word);
        }
    }
           
    /**
     * Check if a word exists if the list of words we have been configured from ConfigAdmin.
     */
    public boolean checkWord(String word)
    {
        return m_words.contains(word);
    }
}
