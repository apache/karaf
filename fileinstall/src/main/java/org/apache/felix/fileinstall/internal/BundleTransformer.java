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
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

import org.apache.felix.fileinstall.ArtifactTransformer;

/**
 * ArtifactTransformer for plain bundles.
 */
public class BundleTransformer implements ArtifactTransformer
{
    public boolean canHandle(File artifact)
    {
        JarFile jar = null;
        try
        {
            // Handle OSGi bundles with the default deployer
            String name = artifact.getName();
            if (!artifact.canRead()  
                || name.endsWith(".txt") || name.endsWith(".xml")
                || name.endsWith(".properties") || name.endsWith(".cfg"))
            {
                // that's file type which is not supported as bundle and avoid
                // exception in the log
                return false;
            }
            jar = new JarFile(artifact);
            Manifest m = jar.getManifest();
            if (m.getMainAttributes().getValue(new Attributes.Name("Bundle-SymbolicName")) != null
                && m.getMainAttributes().getValue(new Attributes.Name("Bundle-Version")) != null)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            // Ignore
        }
        finally
        {
            if (jar != null)
            {
                try
                {
                    jar.close();
                }
                catch (IOException e)
                {
                    // Ignore
                }
            }
        }
        return false;
    }

    public File transform(File artifact, File tmpDir) {
        return artifact;
    }

}
