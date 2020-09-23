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

import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.main.Main;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import static org.easymock.EasyMock.*;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class RunMojoTest extends EasyMockSupport {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testArgs() throws IllegalAccessException,
            MojoFailureException, MojoExecutionException, IOException {
        final AtomicReference<String[]> capturedArgs = new AtomicReference<>();
        final RunMojo mojo = newRunMojo(args -> {
            capturedArgs.set(args);
            throw new FastExit();
        });
        setPrivateField(mojo, "mainArgs", new String[]{"foo"});
        try {
            mojo.execute();
        } catch (final FastExit fe) {
            // expected
        }
        assertArrayEquals(new String[]{"foo"}, capturedArgs.get());
    }

    @Test
    public void testSystemProperties() throws IllegalAccessException,
            MojoFailureException, MojoExecutionException, IOException {
        final RunMojo mojo = newRunMojo(args -> {
            throw new FastExit();
        });
        setPrivateField(mojo, "systemProperties", singletonMap("RunMojoTest.testSystemProperties", "set"));
        try {
            mojo.execute();
        } catch (final FastExit fe) {
            // expected
        }
        assertEquals("set", System.clearProperty("RunMojoTest.testSystemProperties"));
    }

    @Test
    public void testConsoleLevel() throws IllegalAccessException,
            MojoFailureException, MojoExecutionException, IOException {
        final RunMojo mojo = newRunMojo(args -> {
            throw new FastExit();
        });
        setPrivateField(mojo, "consoleLogLevel", "INFO");
        try {
            mojo.execute();
        } catch (final FastExit fe) {
            // expected
        }
        assertEquals("INFO", System.clearProperty("karaf.log.console"));
    }

    @Test
    public void testAddFeatureRepositoriesWithNullRepoList() throws MojoExecutionException {
        FeaturesService featureService = mock(FeaturesService.class);
        replay(featureService);

        RunMojo mojo = new RunMojo();
        mojo.addFeatureRepositories(featureService);
        verify(featureService); // Non-nice easymock mock will fail on any call
    }

    @Test
    public void testAddFeatureRepositoriesWithEmptyRepoListAndNullFeatureService() throws SecurityException, IllegalArgumentException, IllegalAccessException, MojoExecutionException  {
        RunMojo mojo = new RunMojo();
        String[] empty = new String[0];
        setPrivateField(mojo, "featureRepositories", empty);
        try {
            mojo.addFeatureRepositories(null);
            fail("Expected MojoExecutionException to be thrown");
        } catch(MojoExecutionException e) {
            assertEquals("Failed to add feature repositories to karaf", e.getMessage());
        }
    }

    @Test
    public void testAddFeatureRepositoriesWithEmptyRepoList() throws MojoExecutionException, SecurityException, IllegalArgumentException, IllegalAccessException {
        FeaturesService featureService = mock(FeaturesService.class);
        replay(featureService);

        RunMojo mojo = new RunMojo();
        String[] empty = new String[0];
        setPrivateField(mojo, "featureRepositories", empty);
        mojo.addFeatureRepositories(featureService);
        verify(featureService); // Non-nice easymock mock will fail on any call
    }

    @Test
    public void testAddFeatureRepositories() throws Exception {
        FeaturesService featureService = niceMock(FeaturesService.class);
        featureService.addRepository(anyObject(URI.class));
        EasyMock.expectLastCall().once();
        replay(featureService);

        RunMojo mojo = new RunMojo();
        String[] features = { "liquibase-core", "ukelonn" };
        setPrivateField(mojo, "featureRepositories", features);
        mojo.addFeatureRepositories(featureService);
        verify(featureService);
    }

    @Test
    public void testDeployWithDeployProjectArtifactFalse() throws SecurityException, IllegalArgumentException, IllegalAccessException, MojoExecutionException {
        BundleContext context = mock(BundleContext.class);
        RunMojo mojo = new RunMojo();
        setPrivateField(mojo, "deployProjectArtifact", false);
        mojo.deploy(context, null);
    }

    @Test
    public void testDeployWithNullArtifact() throws SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setArtifact(artifact);
        setPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("No artifact to deploy", e.getMessage());
        }
    }

    @Test
    public void testDeployWithNonExistingArtifact() throws SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = mock(File.class);
        expect(artifact.getFile()).andReturn(artifactFile);
        expect(artifactFile.exists()).andReturn(false).times(2);
        replay(artifactFile);
        replay(artifact);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setArtifact(artifact);
        setPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("No artifact to deploy", e.getMessage());
        }
    }

    @Test
    public void testDeployWithExistingArtifactButProjectNotBundle() throws SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = mock(File.class);
        expect(artifactFile.exists()).andReturn(true).times(2);
        expect(artifactFile.getAbsolutePath()).andReturn("foo.jar").times(1);
        expect(artifactFile.toURI()).andReturn(URI.create("file:///foo.jar")).times(1);
        replay(artifactFile);
        expect(artifact.getFile()).andReturn(artifactFile);
        replay(artifact);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setArtifact(artifact);
        setPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("Can't deploy project artifact in container", e.getMessage());
        }
    }

    @Test
    public void testDeployWithExistingArtifactFailsInInstall() throws SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = niceMock(File.class);
        expect(artifactFile.exists()).andReturn(true).times(2);
        replay(artifactFile);
        expect(artifact.getFile()).andReturn(artifactFile);
        replay(artifact);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setPackaging("bundle");
        project.setArtifact(artifact);
        setPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("Can't deploy project artifact in container", e.getMessage());
        }
    }

    @Test
    public void testDeployWithExistingArtifact() throws SecurityException, IllegalArgumentException, IllegalAccessException, IOException, BundleException, MojoExecutionException {
        BundleContext context = niceMock(BundleContext.class);
        Bundle bundle = niceMock(Bundle.class);
        expect(context.installBundle(anyString())).andReturn(bundle);
        replay(context);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = File.createTempFile("fake-bundle", ".jar");
        try {
            expect(artifact.getFile()).andReturn(artifactFile);
            replay(artifact);
            RunMojo mojo = new RunMojo();
            MavenProject project = new MavenProject();
            project.setPackaging("bundle");
            project.setArtifact(artifact);
            setPrivateField(mojo, "project", project);
            replay(bundle);
            mojo.deploy(context, null);
            verify(bundle);
        } finally {
            artifactFile.delete();
        }
    }

    @Test
    public void testDeployWithPomArtifactAndAttachedFeatureXmlNoFeatureService() throws SecurityException, IllegalArgumentException, IllegalAccessException, IOException, BundleException, MojoExecutionException {
        File artifactFeaturesAttachmentFile = File.createTempFile("someproject-features", ".xml");
        try {
            BundleContext context = niceMock(BundleContext.class);
            Bundle bundle = niceMock(Bundle.class);
            expect(context.installBundle(anyString())).andReturn(bundle);
            replay(context);
            Artifact artifact = niceMock(Artifact.class);
            replay(artifact);
            Artifact artifactFeaturesAttachment = mock(Artifact.class);
            expect(artifactFeaturesAttachment.getFile()).andReturn(artifactFeaturesAttachmentFile);
            expect(artifactFeaturesAttachment.getClassifier()).andReturn("features");
            expect(artifactFeaturesAttachment.getType()).andReturn("xml");
            replay(artifactFeaturesAttachment);

            RunMojo mojo = new RunMojo();
            MavenProject project = new MavenProject();
            project.setPackaging("pom");
            project.setArtifact(artifact);
            project.addAttachedArtifact(artifactFeaturesAttachment);
            setPrivateField(mojo, "project", project);
            replay(bundle);
            try {
                mojo.deploy(context, null);
                fail("Expected MojoExecutionException");
            } catch (MojoExecutionException e) {
                assertEquals("Failed to find the FeatureService when adding a feature repository", e.getMessage());
            }
        } finally {
            artifactFeaturesAttachmentFile.delete();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeployWithPomArtifactAndAttachedFeatureXmlRepoRegistrationFails() throws Exception {
        File artifactFeaturesAttachmentFile = File.createTempFile("someproject-features", ".xml");
        try {
            FeaturesService featureService = niceMock(FeaturesService.class);
            featureService.addRepository(anyObject(URI.class));
            EasyMock.expectLastCall().andThrow(new Exception("Not a feature repository"));
            replay(featureService);
            ServiceReference<FeaturesService> ref = niceMock(ServiceReference.class);
            BundleContext context = niceMock(BundleContext.class);
            expect(context.getServiceReference(eq(FeaturesService.class))).andReturn(ref);
            expect(context.getService(eq(ref))).andReturn(featureService);
            replay(context);
            Artifact artifact = niceMock(Artifact.class);
            replay(artifact);
            Artifact artifactFeaturesAttachment = mock(Artifact.class);
            expect(artifactFeaturesAttachment.getFile()).andReturn(artifactFeaturesAttachmentFile);
            expect(artifactFeaturesAttachment.getClassifier()).andReturn("features");
            expect(artifactFeaturesAttachment.getType()).andReturn("xml");
            replay(artifactFeaturesAttachment);

            RunMojo mojo = new RunMojo();
            MavenProject project = new MavenProject();
            project.setPackaging("pom");
            project.setArtifact(artifact);
            project.addAttachedArtifact(artifactFeaturesAttachment);
            setPrivateField(mojo, "project", project);
            try {
                mojo.deploy(context, featureService);
                fail("Expected MojoExecutionException");
            } catch (MojoExecutionException e) {
                assertEquals("Failed to register attachment as feature repository", e.getMessage());
            }
        } finally {
            artifactFeaturesAttachmentFile.delete();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeployWithPomArtifactAndAttachedFeatureXmlAndNoFeatures() throws Exception {
        File artifactFeaturesAttachmentFile = File.createTempFile("someproject-features", ".xml");
        try {
            FeaturesService featureService = niceMock(FeaturesService.class);
            replay(featureService);
            ServiceReference<FeaturesService> ref = niceMock(ServiceReference.class);
            BundleContext context = niceMock(BundleContext.class);
            expect(context.getServiceReference(eq(FeaturesService.class))).andReturn(ref);
            expect(context.getService(eq(ref))).andReturn(featureService);
            replay(context);
            Artifact artifact = niceMock(Artifact.class);
            replay(artifact);
            Artifact artifactFeaturesAttachment = mock(Artifact.class);
            expect(artifactFeaturesAttachment.getFile()).andReturn(artifactFeaturesAttachmentFile);
            expect(artifactFeaturesAttachment.getClassifier()).andReturn("features");
            expect(artifactFeaturesAttachment.getType()).andReturn("xml");
            replay(artifactFeaturesAttachment);

            RunMojo mojo = new RunMojo();
            MavenProject project = new MavenProject();
            project.setPackaging("pom");
            project.setArtifact(artifact);
            project.addAttachedArtifact(artifactFeaturesAttachment);
            setPrivateField(mojo, "project", project);
            mojo.deploy(context, featureService);
            verify(featureService);
        } finally {
            artifactFeaturesAttachmentFile.delete();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeployWithPomArtifactAndAttachedFeatureXml() throws Exception {
        File artifactFeaturesAttachmentFile = File.createTempFile("someproject-features", ".xml");
        try {
            FeaturesService featureService = niceMock(FeaturesService.class);
            replay(featureService);
            ServiceReference<FeaturesService> ref = niceMock(ServiceReference.class);
            BundleContext context = niceMock(BundleContext.class);
            expect(context.getServiceReference(eq(FeaturesService.class))).andReturn(ref);
            expect(context.getService(eq(ref))).andReturn(featureService);
            replay(context);
            Artifact artifact = niceMock(Artifact.class);
            replay(artifact);
            Artifact artifactFeaturesAttachment = mock(Artifact.class);
            expect(artifactFeaturesAttachment.getFile()).andReturn(artifactFeaturesAttachmentFile);
            expect(artifactFeaturesAttachment.getClassifier()).andReturn("features");
            expect(artifactFeaturesAttachment.getType()).andReturn("xml");
            replay(artifactFeaturesAttachment);

            RunMojo mojo = new RunMojo();
            MavenProject project = new MavenProject();
            project.setPackaging("pom");
            project.setArtifact(artifact);
            project.addAttachedArtifact(artifactFeaturesAttachment);
            setPrivateField(mojo, "project", project);
            setPrivateField(mojo, "featuresToInstall", "liquibase-core, ukelonn-db-derby-test, ukelonn");
            String[] featureRepos = { "mvn:org.ops4j.pax.jdbc/pax-jdbc-features/LATEST/xml/features" };
            setPrivateField(mojo, "featureRepositories", featureRepos);
            mojo.deploy(context, featureService);
            verify(featureService);
        } finally {
            artifactFeaturesAttachmentFile.delete();
        }
    }

    /**
     * Just check the string split behaviour on various feature strings.
     */
    @Test
    public void testStringSplit() {
    	String[] split1 = "liquibase-core, ukelonn-db-derby-test, ukelonn".split(" *, *");
    	assertEquals(3, split1.length);
    	String[] split2 = "liquibase-core".split(" *, *");
    	assertEquals(1, split2.length);
    	String[] split3 = " ".split(" *, *");
    	assertEquals(1, split3.length);
    	String[] split4 = " , ".split(" *, *");
    	assertEquals(0, split4.length);
    	String[] split5 = "liquibase-core, ".split(" *, *");
    	assertEquals(1, split5.length);
    	String[] split6 = "liquibase-core, , ".split(" *, *");
    	assertEquals(1, split6.length);
    }

    @Test
    public void testAddFeaturesNullFeaturesToInstall() throws MojoExecutionException {
        FeaturesService featureService = mock(FeaturesService.class);
        replay(featureService);

        RunMojo mojo = new RunMojo();
        mojo.addFeatures(featureService);
        verify(featureService); // Non-nice easymock mock will fail on any call
    }

    @Test
    public void testAddFeaturesNullFeatureService() throws SecurityException, IllegalArgumentException, IllegalAccessException {
        RunMojo mojo = new RunMojo();
        setPrivateField(mojo, "featuresToInstall", "liquibase-core, ukelonn-db-derby-test, ukelonn");

        try {
            mojo.addFeatures(null);
        } catch (MojoExecutionException e) {
            assertEquals("Failed to add features to karaf", e.getMessage());
        }
    }

    @Test
    public void testAddFeatures() throws Exception {
        FeaturesService featureService = mock(FeaturesService.class);
        featureService.installFeature(anyString());
        EasyMock.expectLastCall().times(3);
        replay(featureService);

        RunMojo mojo = new RunMojo();
        setPrivateField(mojo, "featuresToInstall", "liquibase-core, ukelonn-db-derby-test, ukelonn");
        mojo.addFeatures(featureService);
        verify(featureService);
    }

    @Test
    public void testFindFeatureServiceNullServiceRef() {
        BundleContext context = niceMock(BundleContext.class);
        replay(context);

        RunMojo mojo = new RunMojo();
        Object service = mojo.findFeatureService(context);
        assertNull(service);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindFeatureService() {
        FeaturesService featureService = niceMock(FeaturesService.class);
        replay(featureService);
        ServiceReference<FeaturesService> ref = niceMock(ServiceReference.class);
        BundleContext context = niceMock(BundleContext.class);
        expect(context.getServiceReference(eq(FeaturesService.class))).andReturn(ref);
        expect(context.getService(eq(ref))).andReturn(featureService);
        replay(context);

        RunMojo mojo = new RunMojo();
        Object service = mojo.findFeatureService(context);
        assertNotNull(service);
    }

    private RunMojo newRunMojo(final Function<String[], Main> mainFactory)
            throws IllegalAccessException, IOException {
        final Path base = temporaryFolder.getRoot().toPath();
        Stream.of("config.properties", "jre.properties").forEach(etc -> {
            final Path configProperties = base.resolve("etc").resolve(etc);
            try {
                Files.createDirectories(configProperties.getParent());
                Files.copy(
                        Paths.get("../../main/src/test/resources/test-karaf-home/etc").resolve(etc),
                        configProperties);
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        });
        Files.createDirectories(base.resolve("system"));
        final RunMojo mojo = new RunMojo() {
            @Override
            protected Main newMain(final ClassLoader bootLoader, final String[] args) {
                if (mainFactory == null) {
                    return super.newMain(bootLoader, args);
                }
                return mainFactory.apply(args);
            }
        };
        setPrivateField(mojo, "karafDirectory", temporaryFolder.getRoot());
        return mojo;
    }

    private void setPrivateField(Object obj, String fieldName, Object value) throws SecurityException, IllegalArgumentException, IllegalAccessException {
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

    private static class FastExit extends RuntimeException {
    }
}
