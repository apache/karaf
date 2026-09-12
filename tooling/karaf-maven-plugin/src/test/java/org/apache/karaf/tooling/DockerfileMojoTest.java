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
package org.apache.karaf.tooling;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DockerfileMojoTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testDefaultDockerfileGeneration() throws Exception {
        DockerfileMojo mojo = new DockerfileMojo();
        File destDir = temporaryFolder.newFolder("target");
        File assembly = new File(destDir, "assembly");

        setPrivateField(mojo, "destDir", destDir);
        setPrivateField(mojo, "assembly", assembly);
        setPrivateField(mojo, "command", "[\"karaf\", \"run\"]");
        setPrivateField(mojo, "image", "eclipse-temurin:11-jre");

        mojo.execute();

        File dockerfile = new File(destDir, "Dockerfile");
        assertTrue("Dockerfile should be created", dockerfile.exists());

        List<String> lines = Files.readAllLines(dockerfile.toPath());
        assertEquals("FROM eclipse-temurin:11-jre", lines.get(0));
        assertEquals("ENV KARAF_INSTALL_PATH /opt", lines.get(1));
        assertEquals("ENV KARAF_HOME $KARAF_INSTALL_PATH/apache-karaf", lines.get(2));
        assertEquals("ENV KARAF_EXEC exec", lines.get(3));
        assertEquals("ENV PATH $PATH:$KARAF_HOME/bin", lines.get(4));
        assertEquals("COPY assembly $KARAF_HOME", lines.get(5));
        assertEquals("EXPOSE 8101 1099 44444 8181", lines.get(6));
        assertEquals("CMD [\"karaf\", \"run\"]", lines.get(7));
    }

    @Test
    public void testCustomImageAndCommand() throws Exception {
        DockerfileMojo mojo = new DockerfileMojo();
        File destDir = temporaryFolder.newFolder("target-custom");
        File assembly = new File(destDir, "custom-dist");

        setPrivateField(mojo, "destDir", destDir);
        setPrivateField(mojo, "assembly", assembly);
        setPrivateField(mojo, "command", "[\"karaf\", \"server\"]");
        setPrivateField(mojo, "image", "eclipse-temurin:17-jre");

        mojo.execute();

        File dockerfile = new File(destDir, "Dockerfile");
        assertTrue("Dockerfile should be created", dockerfile.exists());

        List<String> lines = Files.readAllLines(dockerfile.toPath());
        assertEquals("FROM eclipse-temurin:17-jre", lines.get(0));
        assertEquals("COPY custom-dist $KARAF_HOME", lines.get(5));
        assertEquals("CMD [\"karaf\", \"server\"]", lines.get(7));
    }

    @Test
    public void testBlankOrNullImageFallsBackToDefault() throws Exception {
        String[] blankImages = new String[]{null, "", "   "};
        for (int i = 0; i < blankImages.length; i++) {
            DockerfileMojo mojo = new DockerfileMojo();
            File destDir = temporaryFolder.newFolder("target-blank-image-" + i);
            File assembly = new File(destDir, "assembly");

            setPrivateField(mojo, "destDir", destDir);
            setPrivateField(mojo, "assembly", assembly);
            setPrivateField(mojo, "command", "[\"karaf\", \"run\"]");
            setPrivateField(mojo, "image", blankImages[i]);

            mojo.execute();

            File dockerfile = new File(destDir, "Dockerfile");
            assertTrue("Dockerfile should be created", dockerfile.exists());

            List<String> lines = Files.readAllLines(dockerfile.toPath());
            assertEquals("FROM eclipse-temurin:11-jre", lines.get(0));
        }
    }

    @Test
    public void testBlankOrNullCommandFallsBackToDefault() throws Exception {
        String[] blankCommands = new String[]{null, "", "   "};
        for (int i = 0; i < blankCommands.length; i++) {
            DockerfileMojo mojo = new DockerfileMojo();
            File destDir = temporaryFolder.newFolder("target-blank-cmd-" + i);
            File assembly = new File(destDir, "assembly");

            setPrivateField(mojo, "destDir", destDir);
            setPrivateField(mojo, "assembly", assembly);
            setPrivateField(mojo, "command", blankCommands[i]);
            setPrivateField(mojo, "image", "eclipse-temurin:11-jre");

            mojo.execute();

            File dockerfile = new File(destDir, "Dockerfile");
            assertTrue("Dockerfile should be created", dockerfile.exists());

            List<String> lines = Files.readAllLines(dockerfile.toPath());
            assertEquals("CMD [\"karaf\", \"run\"]", lines.get(7));
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testImageWithNewlineThrowsException() throws Exception {
        DockerfileMojo mojo = new DockerfileMojo();
        File destDir = temporaryFolder.newFolder("target-nl-image");
        File assembly = new File(destDir, "assembly");

        setPrivateField(mojo, "destDir", destDir);
        setPrivateField(mojo, "assembly", assembly);
        setPrivateField(mojo, "command", "[\"karaf\", \"run\"]");
        setPrivateField(mojo, "image", "eclipse-temurin:11-jre\nRUN rm -rf /");

        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void testCommandWithNewlineThrowsException() throws Exception {
        DockerfileMojo mojo = new DockerfileMojo();
        File destDir = temporaryFolder.newFolder("target-nl-cmd");
        File assembly = new File(destDir, "assembly");

        setPrivateField(mojo, "destDir", destDir);
        setPrivateField(mojo, "assembly", assembly);
        setPrivateField(mojo, "command", "[\"karaf\", \"run\"]\nRUN rm -rf /");
        setPrivateField(mojo, "image", "eclipse-temurin:11-jre");

        mojo.execute();
    }

    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Class<?> aClass = obj.getClass();
        while (aClass != null) {
            try {
                Field field = aClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                return;
            } catch (final NoSuchFieldException nsfe) {
                aClass = aClass.getSuperclass();
            }
        }
        fail("cant set " + fieldName);
    }
}
