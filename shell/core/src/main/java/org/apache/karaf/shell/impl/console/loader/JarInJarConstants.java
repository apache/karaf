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
package org.apache.karaf.shell.impl.console.loader;

public interface JarInJarConstants {
    String REDIRECTED_CLASS_PATH_MANIFEST_NAME  = "Bundle-ClassPath";
    String JAR_INTERNAL_URL_PROTOCOL_WITH_COLON = "jar:jinj:";
    String JAR_INTERNAL_SEPARATOR               = "!/";
    String INTERNAL_URL_PROTOCOL_WITH_COLON     = "jinj:";
    String INTERNAL_URL_PROTOCOL                = "jinj";
    String PATH_SEPARATOR                       = "/";
    String CURRENT_DIR                          = "./";
    String JAR_EXTENSION                        = ".jar";
}
