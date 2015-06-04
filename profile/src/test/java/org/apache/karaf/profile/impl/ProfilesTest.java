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

import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProfilesTest {

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
    public void testProfilePlaceholderResolverWitCycle() {
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
}
