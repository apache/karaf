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
package org.apache.felix.karaf.tooling.features;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Edwin Punzalan
 * @version $Revision: 1.1 $
 */
public class ResolutionListenerImpl implements ResolutionListener {
    private Stack parents = new Stack();
    private Map artifacts = new HashMap();
    private Node rootNode;
    private Log log;

    public void setLog(Log log) {
        this.log = log;
    }

    public Log getLog() {
        return log;
    }

    public void testArtifact(Artifact artifact) {
        // getLog().debug("testArtifact: " + artifact);
        // intentionally blank
    }

    public void startProcessChildren(Artifact artifact) {
        // getLog().debug("startProcessChildren: " + artifact);
        Node node = (Node) artifacts.get(artifact.getDependencyConflictId());
        if (parents.isEmpty()) {
            rootNode = node;
        }
        parents.push(node);
    }

    public void endProcessChildren(Artifact artifact) {
        // getLog().debug("endProcessChildren: " + artifact);
        Node check = (Node) parents.pop();
        assert artifact.equals(check.getArtifact());
    }

    public void omitForNearer(Artifact omitted, Artifact kept) {
        // getLog().debug("omitForNearer: omitted=" + omitted + ", kept=" +
        // kept);
        assert omitted.getDependencyConflictId().equals(
                kept.getDependencyConflictId());
        Node node = (Node) artifacts.get(omitted.getDependencyConflictId());
        assert node != null;
        node.setArtifact(kept);
    }

    public void omitForCycle(Artifact artifact) {
        // getLog().debug("omitForCycle: " + artifact);
        // intentionally blank
    }

    public void includeArtifact(Artifact artifact) {
        // getLog().debug("includeArtifact: " + artifact);
        Node node = (Node) artifacts.get(artifact.getDependencyConflictId());
        if (node == null) {
            node = new Node();
            artifacts.put(artifact.getDependencyConflictId(), node);
        }
        node.setArtifact(artifact);
        if (!parents.isEmpty()) {
            Node parent = (Node) parents.peek();
            parent.getChildren().add(node);
            node.getParents().add(parent);
        }
        if (rootNode != null) {
            // print(rootNode, "");
        }
    }

    protected void print(Node node, String string) {
        // getLog().debug(string + rootNode.getArtifact());
        for (Iterator iter = node.getChildren().iterator(); iter.hasNext();) {
            Node n = (Node) iter.next();
            print(n, string + "  ");
        }
    }

    public void updateScope(Artifact artifact, String scope) {
        // getLog().debug("updateScope: " + artifact);
        Node node = (Node) artifacts.get(artifact.getDependencyConflictId());

        node.getArtifact().setScope(scope);
    }

    public void manageArtifact(Artifact artifact, Artifact replacement) {
        // getLog().debug("manageArtifact: artifact=" + artifact + ",
        // replacement=" + replacement);
        Node node = (Node) artifacts.get(artifact.getDependencyConflictId());
        if (node != null) {
            if (replacement.getVersion() != null) {
                node.getArtifact().setVersion(replacement.getVersion());
            }
            if (replacement.getScope() != null) {
                node.getArtifact().setScope(replacement.getScope());
            }
        }
    }

    public void updateScopeCurrentPom(Artifact artifact, String key) {

        getLog().debug("updateScopeCurrentPom: " + artifact);
        // intentionally blank
    }

    public void selectVersionFromRange(Artifact artifact) {

        getLog().debug("selectVersionFromRange: " + artifact);
        // intentionally blank
    }

    public void restrictRange(Artifact artifact, Artifact artifact1,
            VersionRange versionRange) {

        getLog().debug("restrictRange: " + artifact);
        // intentionally blank
    }

    public Node getNode(Artifact artifact) {
        return (Node) artifacts.get(artifact.getDependencyConflictId());
    }

    public Collection getArtifacts() {
        return artifacts.values();
    }

    public Node getRootNode() {
        return rootNode;
    }
}
