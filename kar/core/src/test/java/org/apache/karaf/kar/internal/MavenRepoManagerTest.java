package org.apache.karaf.kar.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MavenRepoManagerTest {

    @Test
    public void testGetRepos() throws IOException {
        String reposAttr = " \\\n" + 
        		"    http://repo1.maven.org/maven2@id=central, \\\n" + 
        		"    http://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases, \\\n" + 
        		"    https://oss.sonatype.org/content/repositories/ops4j-snapshots@id=ops4j.sonatype.snapshots.deploy@snapshots@noreleases, \\\n" + 
        		"    file:${karaf.home}/${karaf.default.repository}@id=systemrepo";

        MavenRepoManager repoManager = new MavenRepoManager(null);
        List<String> repos = repoManager.getRepos(reposAttr);

        Assert.assertEquals(4, repos.size());
        Assert.assertEquals("http://repo1.maven.org/maven2@id=central", repos.get(0));
        Assert.assertEquals("file:${karaf.home}/${karaf.default.repository}@id=systemrepo", repos.get(3));
    }
    
    @Test
    public void testGetReposAttrValue() throws IOException {
        List<String> repos = new ArrayList<String>();
        repos.add("http://repo1.maven.org/maven2@id=central");
        repos.add("http://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases");
        repos.add("file:${karaf.home}/${karaf.default.repository}@id=systemrepo");

        MavenRepoManager repoManager = new MavenRepoManager(null);
        String reposSt = repoManager.getReposAttrValue(repos);

        Assert.assertEquals("\\\n" + 
        		"http://repo1.maven.org/maven2@id=central, \\\n" + 
        		"http://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases, \\\n" + 
        		"file:${karaf.home}/${karaf.default.repository}@id=systemrepo", reposSt);
    }
}
