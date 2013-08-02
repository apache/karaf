/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features;

import java.util.Set;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;

public class Node {
    private Set<Node> children = new HashSet<Node>();
    private Set<Node> parents = new HashSet<Node>();
    private Artifact artifact;

    public Set<Node> getChildren() {
        return children;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public Set<Node> getParents() {
        return parents;
    }

    public void setChildren(Set<Node> children) {
        this.children = children;
    }

    public void setParents(Set<Node> parents) {
        this.parents = parents;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
}
