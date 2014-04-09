/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.core;

import java.io.OutputStream;

/**
 * Destination for created dumps.
 */
public interface DumpDestination {

    /**
     * Creates new entry in dump destination.
     * 
     * Destination does not close returned output stream by default, dump
     * provider should do this after completing write operation.
     * 
     * @param name Name of file in destination.
     * @return Output stream ready to write.
     * @throws Exception When entry cannot be added.
     */
    OutputStream add(String name) throws Exception;

    /**
     * Complete creation of the dump.
     */
    void save() throws Exception;

}
