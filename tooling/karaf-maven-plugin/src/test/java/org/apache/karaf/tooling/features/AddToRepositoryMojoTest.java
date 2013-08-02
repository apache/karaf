package org.apache.karaf.tooling.features;

import org.apache.karaf.tooling.features.model.Repository;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: heathkesler
 * Date: 8/14/12
 * Time: 9:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class AddToRepositoryMojoTest extends AddToRepositoryMojo {
    @SuppressWarnings("rawtypes")
	public AddToRepositoryMojoTest() throws NoSuchFieldException, IllegalAccessException {
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
        Repository repo = new Repository(in.toURI());

        String[] repos = repo.getDefinedRepositories();

        assert repos.length == 1;
        assert repos[0].equals("http://foo.org");
    }
}
