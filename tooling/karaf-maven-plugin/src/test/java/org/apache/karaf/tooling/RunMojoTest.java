package org.apache.karaf.tooling;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import static org.easymock.EasyMock.*;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class RunMojoTest extends EasyMockSupport {

    @Test
    public void testDeployWithDeployProjectArtifactFalse() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MojoExecutionException {
        BundleContext context = mock(BundleContext.class);
        RunMojo mojo = new RunMojo();
        setPrivateField(mojo, "deployProjectArtifact", false);
        mojo.deploy(context);
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
            mojo.deploy(context);
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
            mojo.deploy(context);
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
            mojo.deploy(context);
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
            mojo.deploy(context);
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
            mojo.deploy(context);
            verify(bundle);
        } finally {
            artifactFile.delete();
        }
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
