/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.log.core.internal;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.log.core.LogMBean;
import org.apache.karaf.log.core.LogService;

import java.util.Map;

/**
 * Implementation of the LogMBean.
 */
public class LogMBeanImpl extends StandardMBean implements LogMBean {

    private final LogService logService;

    public LogMBeanImpl(LogService logService) throws NotCompliantMBeanException {
        super(LogMBean.class);
        this.logService = logService;
    }

    @Override
    public String getLevel() {
        return logService.getLevel();
    }

    @Override
    public Map<String, String> getLevel(String logger) {
        return logService.getLevel(logger);
    }

    @Override
    public void setLevel(String level) {
        this.logService.setLevel(level);
    }

    @Override
    public void setLevel(String logger, String level) {
        this.logService.setLevel(logger, level);
    }

}
