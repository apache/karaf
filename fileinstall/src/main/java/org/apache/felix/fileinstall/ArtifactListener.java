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
 * Interface representing a custom deployment mechanism.
 * 
 * Classes must implement one of its sub-interface, either
 * {@link ArtifactTransformer} or
 * {@link ArtifactInstaller}.
 *
 */
public interface ArtifactListener
{
	
	/**
     * Returns true if the listener can process the given artifact.
     *
     * Error occuring when checking the artifact should be catched
     * and not be thrown.
     *
     * @param artifact the artifact to check
     * @return <code>true</code> if this listener supports
     *         the given artifact, <code>false</code> otherwise
     */
    boolean canHandle(File artifact);

}
