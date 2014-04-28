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
package org.apache.karaf.features;

/**
 * Constants for EventAdmin events
 */
public final class EventConstants {

    public static final String TYPE = "type";
    public static final String EVENT = "event";
    public static final String TIMESTAMP = "timestamp";

    public static final String FEATURE_NAME = "name";
    public static final String FEATURE_VERSION = "version";

    public static final String REPOSITORY_NAME = "name";
    public static final String REPOSITORY_URI = "uri";

    public static final String TOPIC_EVENTS = "org/apache/karaf/features";
    public static final String TOPIC_FEATURES_INSTALLED = TOPIC_EVENTS + "/features/INSTALLED";
    public static final String TOPIC_FEATURES_UNINSTALLED = TOPIC_EVENTS + "/features/UNINSTALLED";
    public static final String TOPIC_REPOSITORY_ADDED = TOPIC_EVENTS + "/repositories/ADDED";
    public static final String TOPIC_REPOSITORY_REMOVED = TOPIC_EVENTS + "/repositories/REMOVED";

    private EventConstants() {
        // non-instantiable class
    }


}
