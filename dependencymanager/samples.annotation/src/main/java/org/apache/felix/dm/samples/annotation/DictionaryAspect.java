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

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * This aspect wraps a Dictionary and checks the "aspect" word on behalf of it.
 */
@AspectService(
    filter="(!(" + Constants.SERVICE_RANKING + "=*))", 
    properties={@Param(name=Constants.SERVICE_RANKING, value="1")}
)
public class DictionaryAspect implements DictionaryService
{
    /**
     * This is the service this aspect is applying to.
     */
    private volatile DictionaryService m_originalDictionary;

    /**
     * We'll use the OSGi log service for logging. If no log service is available, then we'll use a NullObject.
     */
    @ServiceDependency(required = false)
    private LogService m_log;

    public boolean checkWord(String word)
    {
        m_log.log(LogService.LOG_DEBUG, "DictionaryAspect: checking word " + word);
        if (word.equals("aspect")) {
            return true;
        }
        return m_originalDictionary.checkWord(word);
    }
}
