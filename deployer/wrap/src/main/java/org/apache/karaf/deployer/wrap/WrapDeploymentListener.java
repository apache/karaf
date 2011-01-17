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
package org.apache.karaf.deployer.wrap;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;

import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * <p>
 * A deployment listener that listens for non OSGi jar deployements.
 * </p>
 *
 * @author jbonofre
 */
public class WrapDeploymentListener implements ArtifactUrlTransformer {

    public boolean canHandle(File artifact) {
        try {
            JarFile jar = new JarFile(artifact);
            // only handle non OSGi jar
            Manifest manifest = jar.getManifest();
            if (manifest != null &&
                manifest.getMainAttributes().getValue(new Attributes.Name("Bundle-SymbolicName")) != null &&
                manifest.getMainAttributes().getValue(new Attributes.Name("Bundle-Version")) != null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public URL transform(URL artifact) throws Exception {
        String path = artifact.getPath();
        String protocol = artifact.getProtocol();

        // TODO add Bundle-SymbolicName and Bundle-Version to the wrap URL
        return new URL("wrap", null, protocol + ":" + path);
    }

}
