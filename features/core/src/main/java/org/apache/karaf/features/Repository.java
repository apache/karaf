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

import java.net.URI;

/**
 * <p>A repository of features. A runtime representation of JAXB model read from feature XML files.</p>
 *
 * <p>Original model may be subject to further processing (e.g., blacklisting)</p>
 */
public interface Repository extends Blacklisting {

    /**
     * Logical name of the {@link Repository}
     * @return
     */
    String getName();

    /**
     * Original URI of the {@link Repository}, where feature declarations were loaded from
     * @return
     */
    URI getURI();

    /**
     * An array of referenced repository URIs (<code>/features/repository</code>)
     * @return
     */
    URI[] getRepositories();

    /**
     * An array of referenced resource repository URIs (<code>/features/resource-repository</code>)
     * @return
     */
    URI[] getResourceRepositories();

    /**
     * An array of {@link Feature features} in this {@link Repository} after possible processing.
     * @return
     */
    Feature[] getFeatures();

}
