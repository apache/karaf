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

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;

/**
 * The service managing KAR.
 */
public interface KarService {

    /**
     * Install KAR from a given URI
     * 
     * Resources will be copied to the karaf base dir
     * Repository contents will be copied to a subdir in the 
     * karaf data directory
     *
     * @param karUri Uri of the kar to be installed
     * @throws Exception in case of installation failure.
     */
    void install(URI karUri) throws Exception;
    
    /**
     * Install a kar with manually given repository and 
     * resource directories.
     * 
     * @param karUri Uri of the kar to be installed
     * @param repoDir destination for the repository contents of the kar
     * @param resourceDir destination for the resource contents of the kar
     * @throws Exception
     */
    void install(URI karUri, File repoDir, File resourceDir) throws Exception;

    /**
     * Uninstall the given KAR
     *
     * @param name the name of the KAR
     * @throws Exception in case of failure
     */
    void uninstall(String name) throws Exception;

    /**
     * List the KAR stored in the data folder.
     * 
     * @return the list of KAR stored.
     * @throws Exception in case of listing failure.
     */
    List<String> list() throws Exception;
    
    /**
     * Create a kar from the given feature and repo names.
     * Each named feature including all transitive deps will be added.
     * For each named repo all features in the repo and their transitive deps will be added.
     * 
     * @param repoName
     * @param features 
     * @param console
     */
    void create(String repoName, List<String> features, PrintStream console);

    
}
