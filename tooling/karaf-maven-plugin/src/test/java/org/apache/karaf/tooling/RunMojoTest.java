package org.apache.karaf.tooling;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import org.apache.karaf.features.FeaturesService;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import static org.easymock.EasyMock.*;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class RunMojoTest extends EasyMockSupport {

    @Test
    public void testAddFeatureRepositoriesWithNullRepoList() throws MojoExecutionException {
        FeaturesService featureService = mock(FeaturesService.class);
        replay(featureService);

        RunMojo mojo = new RunMojo();
        mojo.addFeatureRepositories(featureService);
        verify(featureService); // Non-nice easymock mock will fail on any call
    }

    @Test
    public void testAddFeatureRepositoriesWithEmptyRepoListAndNullFeatureService() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MojoExecutionException  {
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
    public void testAddFeatureRepositoriesWithEmptyRepoList() throws MojoExecutionException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
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
    public void testDeployWithDeployProjectArtifactFalse() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MojoExecutionException {
        BundleContext context = mock(BundleContext.class);
        RunMojo mojo = new RunMojo();
        setPrivateField(mojo, "deployProjectArtifact", false);
        mojo.deploy(context, null);
    }

    @Test
    public void testDeployWithNullArtifact() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setArtifact(artifact);
        setInheritedPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("Project artifact doesn't exist", e.getMessage());
        }
    }

    @Test
    public void testDeployWithNonExistingArtifact() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = mock(File.class);
        expect(artifact.getFile()).andReturn(artifactFile);
        replay(artifact);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setArtifact(artifact);
        setInheritedPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("Project artifact doesn't exist", e.getMessage());
        }
    }

    @Test
    public void testDeployWithExistingArtifactButProjectNotBundle() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = mock(File.class);
        expect(artifactFile.exists()).andReturn(true);
        replay(artifactFile);
        expect(artifact.getFile()).andReturn(artifactFile);
        replay(artifact);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setArtifact(artifact);
        setInheritedPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("Packaging jar is not supported", e.getMessage());
        }
    }

    @Test
    public void testDeployWithExistingArtifactFailsInInstall() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        BundleContext context = mock(BundleContext.class);
        Artifact artifact = mock(Artifact.class);
        File artifactFile = niceMock(File.class);
        expect(artifactFile.exists()).andReturn(true);
        replay(artifactFile);
        expect(artifact.getFile()).andReturn(artifactFile);
        replay(artifact);
        RunMojo mojo = new RunMojo();
        MavenProject project = new MavenProject();
        project.setPackaging("bundle");
        project.setArtifact(artifact);
        setInheritedPrivateField(mojo, "project", project);
        try {
            mojo.deploy(context, null);
            fail("Expected MojoExecutionException");
        } catch (MojoExecutionException e) {
            assertEquals("Can't deploy project artifact in container", e.getMessage());
        }
    }

    @Test
    public void testDeployWithExistingArtifact() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException, BundleException, MojoExecutionException {
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
            setInheritedPrivateField(mojo, "project", project);
            replay(bundle);
            mojo.deploy(context, null);
            verify(bundle);
        } finally {
            artifactFile.delete();
        }
    }

    @Test
    public void testDeployWithPomArtifactAndAttachedFeatureXmlNoFeatureService() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException, BundleException, MojoExecutionException {
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
            setInheritedPrivateField(mojo, "project", project);
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
            setInheritedPrivateField(mojo, "project", project);
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
            setInheritedPrivateField(mojo, "project", project);
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
            setInheritedPrivateField(mojo, "project", project);
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
    public void testAddFeaturesNullFeatureService() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
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

    private void setPrivateField(Object obj, String fieldName, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private void setInheritedPrivateField(Object obj, String fieldName, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
