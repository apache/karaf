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
package org.apache.felix.bundleplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import aQute.lib.osgi.Builder;
import aQute.lib.spring.XMLType;
import junit.framework.TestCase;
import org.apache.felix.bundleplugin.ManifestPlugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.osgi.framework.Constants;

public class BlueprintComponentTest extends TestCase {

    public void testBlueprint() throws Exception
    {

        MavenProjectStub project = new MavenProjectStub() {
            private final List resources = new ArrayList();
            @Override
            public void addResource(Resource resource) {
                resources.add(resource);
            }

            @Override
            public List getResources() {
                return resources;
            }
        };
        project.setGroupId( "group" );
        project.setArtifactId( "artifact" );
        project.setVersion( "1.1.0.0" );
        Resource r = new Resource();
        r.setDirectory(new File("src/test/resources").getAbsoluteFile().getCanonicalPath());
        r.setIncludes(Arrays.asList("**/*.*"));
        project.addResource(r);
        project.addCompileSourceRoot(new File("src/test/resources").getAbsoluteFile().getCanonicalPath());

        ManifestPlugin plugin = new ManifestPlugin();
        plugin.setBasedir(new File("target/tmp/basedir"));
        plugin.setBuildDirectory("target/tmp/basedir/target");
        plugin.setOutputDirectory(new File("target/tmp/basedir/target/classes"));

        Map instructions = new HashMap();
        instructions.put("Test", "Foo");

        instructions.put("nsh_interface", "foo.bar.Namespace");
        instructions.put("nsh_namespace", "ns");

        instructions.put("Export-Service", "p7.Foo;mk=mv");
        instructions.put("Import-Service", "org.osgi.service.cm.ConfigurationAdmin;availability:=optional");

        Properties props = new Properties();
        Builder builder = plugin.buildOSGiBundle(project, instructions, props, plugin.getClasspath(project));

        Manifest manifest = builder.getJar().getManifest();
        String expSvc = manifest.getMainAttributes().getValue(Constants.EXPORT_SERVICE);
        String impSvc = manifest.getMainAttributes().getValue(Constants.IMPORT_SERVICE);
        assertNotNull(expSvc);
        assertNotNull(impSvc);

        String impPkg = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        List<String> pkgs = Arrays.asList(impPkg.split(","));
        for (int i = 1; i <= 13; i++) {
            assertTrue(pkgs.contains("p" + i));
        }
    }

}
