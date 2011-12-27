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
package org.apache.karaf.kar;

import java.net.URI;
import java.util.List;

/**
 * The service managing KAR.
 */
public interface KarService {

    /**
     * Install KAR from a given URL.
     *
     * @param url the KAR URL.
     * @throws Exception in case of installation failure.
     */
    void install(URI url) throws Exception;

    /**
     * Uninstall the given KAR.
     * NB: the system folder is not cleaned.
     *
     * @param name the name of the KAR.
     * @throws Exception in case of uninstall failure.
     */
    void uninstall(String name) throws Exception;

    /**
     * Uninstall the given KAR and, eventually, cleanup the repository from the KAR content.
     *
     * @param name the name of the KAR.
     * @param clean true to cleanup the repository folder, false else.
     * @throws Exception in case of uninstall failure.
     */
    void uninstall(String name, boolean clean) throws Exception;

    /**
     * List the KAR stored in the data folder.
     * 
     * @return the list of KAR stored.
     * @throws Exception in case of listing failure.
     */
    List<String> list() throws Exception;
    
}
