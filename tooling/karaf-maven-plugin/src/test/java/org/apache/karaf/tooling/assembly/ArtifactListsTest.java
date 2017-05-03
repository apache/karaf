package org.apache.karaf.tooling.assembly;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtifactLists}.
 */
public class ArtifactListsTest {

    private ArtifactLists subject;

    private List<String> list;

    private String listItem;

    @Before
    public void setUp() throws Exception {
        subject = new ArtifactLists();
        list = new ArrayList<>();
        listItem = "list item";
        list.add(listItem);
    }

    @Test
    public void startupBundles() {
        subject.addStartupBundles(list);
        assertThat(subject.getStartupBundles()).containsOnly(listItem);
    }

    @Test
    public void bootBundles() {
        subject.addBootBundles(list);
        assertThat(subject.getBootBundles()).containsOnly(listItem);
    }

    @Test
    public void installedBundles() {
        subject.addInstalledBundles(list);
        assertThat(subject.getInstalledBundles()).containsOnly(listItem);
    }

    @Test
    public void startupRepositories() {
        subject.addStartupRepositories(list);
        assertThat(subject.getStartupRepositories()).containsOnly(listItem);
    }

    @Test
    public void bootRepositories() {
        subject.addBootRepositories(list);
        assertThat(subject.getBootRepositories()).containsOnly(listItem);
    }

    @Test
    public void installedRepositories() {
        subject.addInstalledRepositories(list);
        assertThat(subject.getInstalledRepositories()).containsOnly(listItem);
    }

    @Test
    public void startupKars() {
        subject.getStartupKars()
               .addAll(list);
        assertThat(subject.getStartupKars()).containsOnly(listItem);
    }

    @Test
    public void bootKars() {
        subject.getBootKars()
               .addAll(list);
        assertThat(subject.getBootKars()).containsOnly(listItem);
    }

    @Test
    public void installedKars() {
        subject.getInstalledKars()
               .addAll(list);
        assertThat(subject.getInstalledKars()).containsOnly(listItem);
    }

    @Test
    public void remoteStartupKars() {
        //given
        subject.getStartupKars()
               .addAll(list);
        //when
        subject.removeStartupKar(listItem);
        //then
        assertThat(subject.getStartupKars()).isEmpty();
    }

}
