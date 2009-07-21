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

package org.apache.felix.sigil.config;


public class BldAttr
{
    // Sigil attributes

    public static final String KIND_ATTRIBUTE = "kind";

    public static final String RESOLVE_ATTRIBUTE = "resolve";
    public static final String RESOLVE_AUTO = "auto";
    public static final String RESOLVE_COMPILE = "compile";
    public static final String RESOLVE_RUNTIME = "runtime";
    public static final String RESOLVE_IGNORE = "ignore";

    public static final String PUBLISH_ATTRIBUTE = "publish";
    public static final String PUBTYPE_ATTRIBUTE = "type";
    public static final String PATH_ATTRIBUTE = "path";
    public static final Object ZONE_ATTRIBUTE = "zone";

    // Sigil options

    public static final String OPTION_ADD_IMPORTS = "addMissingImports";
    public static final String OPTION_OMIT_IMPORTS = "omitUnusedImports";

    // OSGi attributes

    public static final String RESOLUTION_ATTRIBUTE = "resolution";
    public static final String RESOLUTION_OPTIONAL = "optional";

    public static final String VERSION_ATTRIBUTE = "version";

}
