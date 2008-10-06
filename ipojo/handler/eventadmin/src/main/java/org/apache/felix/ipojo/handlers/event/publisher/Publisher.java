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
package org.apache.felix.ipojo.handlers.event.publisher;

import java.util.Dictionary;

/**
 * An Publisher is the interface between the EventAdminPublisherHandler and a
 * component instance. The POJO can send event through the handler by calling a
 * {@code send} method.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Publisher {

    /**
     * Sends an event with the specified content.
     * 
     * @param content the content of the event
     */
    void send(Dictionary content);

    /**
     * Sends a data event.
     * 
     * @param o the data to send
     */
    void sendData(Object o);
}
