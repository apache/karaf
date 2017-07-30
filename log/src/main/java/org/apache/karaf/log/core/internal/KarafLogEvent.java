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

import java.util.HashMap;
import java.util.Map;

import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLocationInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

@SuppressWarnings("rawtypes")
public class KarafLogEvent implements PaxLoggingEvent {
    
    private PaxLocationInfo locationInformation;
    private PaxLevel level;
    private String loggerName;
    private String message;
    private String threadName;
    private String[] throwableStrRep;
    private long timeStamp;
    private String fQNOfLoggerClass;
    private Map properties;

    @SuppressWarnings("unchecked")
    public KarafLogEvent(PaxLoggingEvent event) {
        this.level = event.getLevel();
        this.loggerName = event.getLoggerName();
        this.message = event.getMessage();
        this.threadName = event.getThreadName();
        this.throwableStrRep = event.getThrowableStrRep();
        this.timeStamp = event.getTimeStamp();
        this.fQNOfLoggerClass = event.getFQNOfLoggerClass();
        this.properties = new HashMap(event.getProperties());
        this.locationInformation = event.getLocationInformation();
    }

    @Override
    public PaxLocationInfo getLocationInformation() {
        return this.locationInformation;
    }

    @Override
    public PaxLevel getLevel() {
        return this.level;
    }

    @Override
    public String getLoggerName() {
        return this.loggerName;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String getRenderedMessage() {
        return this.message;
    }

    @Override
    public String getThreadName() {
        return this.threadName;
    }

    @Override
    public String[] getThrowableStrRep() {
        return this.throwableStrRep;
    }

    @Override
    public boolean locationInformationExists() {
        return this.locationInformation != null;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public String getFQNOfLoggerClass() {
        return this.fQNOfLoggerClass;
    }

    @Override
    public Map getProperties() {
        return this.properties;
    }

}
