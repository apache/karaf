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
package org.apache.karaf.features.internal.resolver;

import org.slf4j.Logger;

/**
 */
public class Slf4jResolverLog extends org.apache.felix.resolver.Logger {

    private final Logger logger;

    public Slf4jResolverLog(Logger logger) {
        super(LOG_DEBUG);
        this.logger = logger;
    }

    @Override
    protected void doLog(int level, String msg, Throwable throwable) {
        switch (level) {
            case LOG_ERROR:
                logger.error(msg, throwable);
                break;
            case LOG_WARNING:
                logger.warn(msg, throwable);
                break;
            case LOG_INFO:
                logger.info(msg, throwable);
                break;
            default:
                logger.debug(msg, throwable);
                break;
        }
    }
}