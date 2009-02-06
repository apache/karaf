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
package org.apache.felix.scr.impl;


/**
 * The <code>Logger</code> interface defines a simple API to enable some logging
 * in the XML Parser and ComponentMetadata handling classes and at the same
 * time not be too intrusive for the unit tests.
 */
interface Logger
{

    /**
     * Writes a messages for the given <code>ComponentMetadata</code>.
     * 
     * @param level The log level of the messages. This corresponds to the log
     *          levels defined by the OSGi LogService.
     * @param message The message to print
     * @param metadata The {@link ComponentMetadata} whose processing caused
     *          the message. This may be <code>null</code> if the component
     *          metadata is not known or applicable.
     * @param ex The <code>Throwable</code> causing the message to be logged.
     *          This may be <code>null</code>.
     */
    void log( int level, String message, ComponentMetadata metadata, Throwable ex );

}
