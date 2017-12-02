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
package org.apache.karaf.profile.impl;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfilesTest {

    public static Logger LOG = LoggerFactory.getLogger(ProfilesTest.class);

    @Test
    public void testProfilesApi() throws IOException {
        ProfileBuilder builder = ProfileBuilder.Factory.create("my-simple-profile");
        builder.addParents(Collections.emptyList());
        builder.addAttribute("attr1", "val1");
        builder.addBundle("mvn:commons-everything/commons-everything/42");
        builder.addConfiguration("my.pid", "a1", "v1${profile:my.pid2/a2}");
        builder.addConfiguration("my.pid", "a2", "v1${profile:my.pid2/a3}");
        builder.addFeature("feature1");
        builder.addFileConfiguration("my.pid2.txt", "hello!".getBytes("UTF-8"));
        builder.addFileConfiguration("my.pid2.cfg", "a2=v2".getBytes("UTF-8"));
        builder.addRepository("mvn:my/repository/1/xml/features");
        builder.setOptionals(Arrays.asList("mvn:g/a/1", "mvn:g/a/2"));
        builder.setOverrides(Arrays.asList("mvn:g/a/4", "mvn:g/a/3"));
        Profile profile = builder.getProfile();
        LOG.info("Profile: {}", profile.toString());
        LOG.info("Config: {}", profile.getConfig());
        LOG.info("Libraries: {}", profile.getLibraries());
        LOG.info("System: {}", profile.getSystem());
        LOG.info("Configurations: {}", profile.getConfigurations());
        LOG.info("ConfigurationFileNames: {}", profile.getConfigurationFileNames());
        LOG.info("FileConfigurations: {}", profile.getFileConfigurations().keySet());

        Profile effectiveProfile1 = Profiles.getEffective(profile, false);
        Profile effectiveProfile2 = Profiles.getEffective(profile, true);
        Map<String, Profile> profiles = new HashMap<>();
        profiles.put("x", profile);
        Profile overlayProfile = Profiles.getOverlay(profile, profiles);
        Profiles.writeProfile(Paths.get("target/p-" + UUID.randomUUID().toString()), profile);
        Profiles.writeProfile(Paths.get("target/ep1-" + UUID.randomUUID().toString()), effectiveProfile1);
        Profiles.writeProfile(Paths.get("target/ep2-" + UUID.randomUUID().toString()), effectiveProfile2);
        Profiles.writeProfile(Paths.get("target/op-" + UUID.randomUUID().toString()), overlayProfile);
    }

    @Test
    public void testProfilePlaceholderResolver() {
        Profile profile = ProfileBuilder.Factory.create("test")
                .addConfiguration("pid1", "foo", "b${profile:pid2/bar}")
                .addConfiguration("pid2", "bar", "a${rep}")
                .addConfiguration("pid2", "rep", "h")
                .getProfile();

        Profile effective = Profiles.getEffective(profile);

        assertEquals("bah", effective.getConfiguration("pid1").get("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProfilePlaceholderResolverWithCycle() {
        Profile profile = ProfileBuilder.Factory.create("test")
                .addConfiguration("pid1", "foo", "b${profile:pid2/bar}")
                .addConfiguration("pid2", "bar", "a${rep}")
                .addConfiguration("pid2", "rep", "h${profile:pid1/foo}")
                .getProfile();

        Profile effective = Profiles.getEffective(profile);

        effective.getConfiguration("pid1").get("foo");
        // Should throw an exception
    }

    @Test
    public void testNonSubstitution() {
        Profile profile = ProfileBuilder.Factory.create("test")
                .addConfiguration("pid1", "key", "${foo}/${bar}")
                .getProfile();

        Profile effective = Profiles.getEffective(profile, false);

        assertEquals("${foo}/${bar}", effective.getConfiguration("pid1").get("key"));
    }

    @Test
    public void testProfilesOverlayComments() {
        String pid1 = "# My comment\nfoo = bar\n";

        Profile parent = ProfileBuilder.Factory.create("parent")
                .addFileConfiguration("pid1.cfg", pid1.getBytes())
                .getProfile();

        Profile profile = ProfileBuilder.Factory.create("test")
                .addConfiguration("pid1", "foo", "bar2")
                .addParent("parent")
                .getProfile();

        Map<String, Profile> profiles = new HashMap<>();
        profiles.put(parent.getId(), parent);
        profiles.put(profile.getId(), profile);

        Profile overlay = Profiles.getOverlay(profile, profiles);

        String outPid1 = new String(overlay.getFileConfiguration("pid1.cfg"));
        assertEquals(String.format("%1$s%n%2$s%n","# My comment","foo = bar2"), outPid1);
    }

    @Test
    public void overlayProfiles() {
        Profile p1 = ProfileBuilder.Factory.create("p1")
                .addAttribute("p1a1", "p1v1")
                .addConfiguration("p1p1", "p1p1p1", "p1p1v1")
                .addConfiguration("pp1", "pp1p1", "p1p1v1")
                .getProfile();
        Profile p2 = ProfileBuilder.Factory.create("p2")
                .addAttribute("p2a1", "p2v1")
                .addConfiguration("p2p1", "p2p1p1", "p2p1v1")
                .addConfiguration("pp1", "pp1p1", "p2p1v1")
                .getProfile();

        Profile c1 = ProfileBuilder.Factory.create("c2")
                .addParents(Arrays.asList("p1", "p2"))
                .getProfile();

        assertThat(c1.getAttributes().get("p1a1"), nullValue());
        assertThat(c1.getAttributes().get("p2a1"), nullValue());
        assertThat(c1.getConfigurations().size(), equalTo(1));
        assertTrue(c1.getConfigurations().containsKey("profile"));

        Map<String, Profile> parents = new LinkedHashMap<>();
        parents.put("p1", p1);
        parents.put("p2", p2);
        Profile oc1 = Profiles.getOverlay(c1, parents);
        assertThat(oc1.getAttributes().get("p1a1"), equalTo("p1v1"));
        assertThat(oc1.getAttributes().get("p2a1"), equalTo("p2v1"));
        assertThat(oc1.getConfigurations().size(), equalTo(4));
        assertTrue(oc1.getConfigurations().containsKey("p1p1"));
        assertTrue(oc1.getConfigurations().containsKey("p2p1"));
        assertTrue(oc1.getConfigurations().containsKey("pp1"));
        assertTrue(oc1.getConfigurations().containsKey("profile"));
    }

    @Test
    public void inheritanceOrder() {
        Profile gp1 = ProfileBuilder.Factory.create("gp1")
                .addAttribute("a", "1")
                .addFileConfiguration("f", new byte[] { 0x01 })
                .addAttribute("b", "1")
                .addAttribute("c", "1")
                .addConfiguration("p", "p", "1")
                .addConfiguration("p", "px", "1")
                .getProfile();
        Profile gp2 = ProfileBuilder.Factory.create("gp2")
                .addAttribute("a", "2")
                .addAttribute("c", "2")
                .addFileConfiguration("f", new byte[] { 0x02 })
                .addConfiguration("p", "p", "2")
                .getProfile();
        Profile p1 = ProfileBuilder.Factory.create("p1")
                .addParents(Arrays.asList("gp1", "gp2"))
                .addAttribute("a", "3")
                .addFileConfiguration("f", new byte[] { 0x03 })
                .addConfiguration("p", "p", "3")
                .getProfile();
        Profile p2 = ProfileBuilder.Factory.create("p2")
                .addAttribute("a", "4")
                .addAttribute("b", "4")
                .addFileConfiguration("f", new byte[] { 0x04 })
                .addConfiguration("p", "p", "4")
                .getProfile();
        Profile c = ProfileBuilder.Factory.create("p2")
                .addParents(Arrays.asList("p1", "p2"))
                .addAttribute("a", "5")
                .addFileConfiguration("f", new byte[] { 0x05 })
                .addConfiguration("p", "p", "5")
                .getProfile();

        Map<String, Profile> parents = new LinkedHashMap<>();
        parents.put("gp1", gp1);
        parents.put("gp2", gp2);
        parents.put("p1", p1);
        parents.put("p2", p2);

        Profile overlay = Profiles.getOverlay(c, parents);
        assertThat(overlay.getAttributes().get("a"), equalTo("5"));
        assertThat(overlay.getAttributes().get("b"), equalTo("4"));
        assertThat(overlay.getAttributes().get("c"), equalTo("2"));
        assertThat(overlay.getConfiguration("p").get("p"), equalTo("5"));
        assertThat(overlay.getConfiguration("p").get("px"), equalTo("1"));
        assertThat(overlay.getFileConfiguration("f"), equalTo(new byte[] { 0x05 }));
    }

    @Test
    public void overrides() {
        Profile p = ProfileBuilder.Factory.create("p")
                .setOverrides(Arrays.asList("a", "b"))
                .getProfile();

        assertThat(p.getConfiguration("profile").size(), equalTo(2));
    }

}
