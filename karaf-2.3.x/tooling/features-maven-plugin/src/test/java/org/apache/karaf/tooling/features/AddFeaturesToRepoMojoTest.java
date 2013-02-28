package org.apache.karaf.tooling.features;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;

public class AddFeaturesToRepoMojoTest extends MojoSupport {

    public AddFeaturesToRepoMojoTest() throws NoSuchFieldException, IllegalAccessException {
        factory = new DefaultArtifactFactory();
        ArtifactHandlerManager artifactHandlerManager = new DefaultArtifactHandlerManager();
        Field f = factory.getClass().getDeclaredField("artifactHandlerManager");
        f.setAccessible(true);
        f.set(factory, artifactHandlerManager);
        f.setAccessible(false);

        f = artifactHandlerManager.getClass().getDeclaredField("artifactHandlers");
        f.setAccessible(true);
        f.set(artifactHandlerManager, new HashMap());
        f.setAccessible(false);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
    }

    @Test
    public void testSimpleURL() throws Exception {
        URL in = getClass().getClassLoader().getResource("input-repository.xml");
        AddFeaturesToRepoMojo.Repository repo = new AddFeaturesToRepoMojo.Repository(in.toURI());

        String[] repos = repo.getDefinedRepositories();

        assert repos.length == 1;
        assert repos[0].equals("http://foo.org");
    }
}
