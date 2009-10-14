/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.fileinstall;

import java.io.File;

/**
 * Objects implementing this interface are able to directly
 * install and uninstall supported artifacts.  Artifacts that
 * are transformed into bundles should use the
 * {@link ArtifactTransformer} interface instead.
 *
 * Note that fileinstall does not keep track of those artifacts
 * across restarts, so this means that after a restart, existing
 * artifacts will be reported as new, while any deleted artifact
 * won't be reported as deleted.
 */
public interface ArtifactInstaller extends ArtifactListener
{

    /**
     * Install the artifact
     *
     * @param artifact the artifact to be installed
     */
    void install(File artifact) throws Exception;

    /**
     * Update the artifact
     *
     * @param artifact the artifact to be updated
     */
    void update(File artifact) throws Exception;

    /**
     * Uninstall the artifact
     * 
     * @param artifact the artifact to be uninstalled
     */
    void uninstall(File artifact) throws Exception;

}
