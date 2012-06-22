package org.apache.karaf.kar.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows to add or delete repo URIs from the pax url mvn config
 */
public class MavenRepoManager {

    static final String ATTR_REPOS = "org.ops4j.pax.url.mvn.repositories";
    static final String PAX_URL_MVN_CONFIG_PID = "org.ops4j.pax.url.mvn";
    private final File configFile;

    public MavenRepoManager(String karafBase) {
        File etcFile = new File(karafBase, "etc");
        configFile = new File(etcFile, PAX_URL_MVN_CONFIG_PID + ".cfg");
        if (!configFile.exists() && configFile.isFile()) {
            throw new RuntimeException("Cannot find config " + configFile);
        }
    }

    public void addRepo(URI repo) throws IOException {
        NoSubstProperties props = new NoSubstProperties(configFile);
        List<String> repos = getRepos((String)props.get(ATTR_REPOS));
        String repoSt = repo.toString();
        if (!repos.contains(repoSt)) {
            repos.add(repoSt);
        }
        props.put(ATTR_REPOS, getReposAttrValue(repos));
        props.save();
    }

    public void removeRepo(URI repo) throws IOException {
        NoSubstProperties props = new NoSubstProperties(configFile);
        List<String> repos = getRepos((String)props.get(ATTR_REPOS));
        List<String> outRepos = new ArrayList<String>();
        String repoSt = repo.toString();
        for (String currentRepo : repos) {
            String temp = new String(currentRepo);
            if (!repoSt.equals(temp)) {
                outRepos.add(currentRepo);
            }
        }
        props.put(ATTR_REPOS, getReposAttrValue(outRepos));
        props.save();
    }

    List<String> getRepos(String repos) throws IOException {
        repos = repos.replaceAll("\\s*\\\\\\r?\\n", "");
        String[] origRepoAr = repos.split("\\s*,\\s*\\\\?");
        List<String> repoList = new ArrayList<String>();
        for (String repo : origRepoAr) {
            repo = repo.trim();
            if (repo != null && !"".equals(repo)) {
                repoList.add(repo);
            }
        }
        return repoList;
    }

    String getReposAttrValue(List<String> repos) {
        StringBuilder repoAttr = new StringBuilder();
        repoAttr.append("\\\n");
        for (int c=0; c<repos.size(); c++) {
            repoAttr.append(repos.get(c));
            if (c+1<repos.size()) {
                repoAttr.append(", \\\n");
            }
        }
        return repoAttr.toString();
    }
}
