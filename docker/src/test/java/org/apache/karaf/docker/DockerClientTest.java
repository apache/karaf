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
package org.apache.karaf.docker;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DockerClient factory test.
 */
public class DockerClientTest {

    private DockerClient dockerClient;

    @Before
    public void setup() {
        dockerClient = new DockerClient(null);
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testDockerInfo() throws Exception {
        Info info = dockerClient.info();
        System.out.println("Info:");
        System.out.println("\tDriver: " + info.getDriver());
        System.out.println("\tDriver Status: " + info.getDriverStatus());
        System.out.println("\tExecution Driver: " + info.getExecutionDriver());
        System.out.println("\tIndex Server Address: "  + info.getIndexServerAddress());
        System.out.println("\tInit Path: " + info.getInitPath());
        System.out.println("\tInit SHA1: " + info.getInitSha1());
        System.out.println("\tKernel Version: " + info.getKernelVersion());
        System.out.println("\tContainers: " + info.getContainers());
        System.out.println("\tImages: " + info.getImages());
        System.out.println("\tNFD: " + info.getNfd());
        System.out.println("\tNGoRoutines: " + info.getNgoroutines());
        System.out.println("\tMemory Limit enabled: " + info.isMemoryLimit());
        System.out.println("\tSwap Limit enabled: " + info.isSwapLimit());
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testDockerVersion() throws Exception {
        Version version = dockerClient.version();
        System.out.println("Version:");
        System.out.println("\tAPI version: " + version.getApiVersion());
        System.out.println("\tArch: " + version.getArch());
        System.out.println("\tBuild Time: " + version.getBuildTime());
        System.out.println("\tExperimental: " + version.getExperimental());
        System.out.println("\tGit Commit: " + version.getGitCommit());
        System.out.println("\tGo Version: " + version.getGoVersion());
        System.out.println("\tKernel Version: " + version.getKernelVersion());
        System.out.println("\tOS: " + version.getOs());
        System.out.println("\tVersion: " + version.getVersion());
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testListImages() throws Exception {
        for (Image image : dockerClient.images(true)) {
            System.out.println("----");
            System.out.println("Image ID: " + image.getId());
            System.out.println("Created: " + image.getCreated());
            System.out.println("Size: " + image.getSize());
            System.out.println("Virtual size: " + image.getVirtualSize());
            System.out.println("RepoTags: " + image.getRepoTags());
            System.out.println("Labels: " + image.getLabels());
            System.out.println("Containers: " + image.getContainers());
        }
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testHistoryImage() throws Exception {
        for (ImageHistory history : dockerClient.history("sha256:f3ea90d50ffd7851cb984764409326b82593c612fe6e6dc7933d9568f735084b")) {
            System.out.println("----");
            System.out.println("ID: " + history.getId());
            System.out.println("Created: " + history.getCreated());
            System.out.println("Created by: " + history.getCreatedBy());
            System.out.println("Comment: " + history.getComment());
            System.out.println("Size: " + history.getSize());
            System.out.println("Tags: " + history.getTags());
        }
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testSearchImages() throws Exception {
        List<ImageSearch> images = dockerClient.search("karaf");
        for (ImageSearch image : images) {
            System.out.println("----");
            System.out.println("Image Name: " + image.getName());
            System.out.println("Image Star Count: " + image.getStarCount());
            System.out.println("Image Automated: " + image.isAutomated());
            System.out.println("Image Official: " + image.isOfficial());
            System.out.println("Image Description: " + image.getDescription());
        }
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testPullImage() throws Exception {
        dockerClient.pull("mkroli/karaf", "latest", true);
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testRemoveImage() throws Exception {
        dockerClient.rmi("sha256:f3ea90d50ffd7851cb984764409326b82593c612fe6e6dc7933d9568f735084b", true, false);
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testContainers() throws Exception {
        for (Container container : dockerClient.ps(true)) {
            System.out.println("----");
            System.out.println("Container " + container.getId());
            System.out.println("Command: " + container.getCommand());
            System.out.println("Created: " + container.getCreated());
            System.out.println("Image: " + container.getImage());
            System.out.println("Image ID: " + container.getImageId());
            System.out.println("Names: " + container.getNames());
            for (Port port : container.getPorts()) {
                System.out.println("Port: " + port.getPublicPort() + ":" + port.getPrivatePort() + " (" + port.getType() + ")");
            }
            System.out.println("Status: " + container.getStatus());
            System.out.println("State: " + container.getState());
            System.out.println("Size: " + container.getSizeRw());
            System.out.println("Size Root: " + container.getSizeRootFs());
        }
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testCreateContainer() throws Exception {
        ContainerConfig config = new ContainerConfig();
        config.setImage("java:8-jre-alpine");
        config.setAttachStderr(true);
        config.setAttachStdin(true);
        config.setAttachStdout(true);
        config.setTty(true);
        dockerClient.create(config, "test");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testCreateKarafContainer() throws Exception {
        ContainerConfig config = new ContainerConfig();
        config.setTty(true);
        config.setAttachStdout(true);
        config.setAttachStderr(true);
        config.setAttachStdin(true);
        config.setImage("karaf:latest");
        config.setHostname("docker");
        config.setUser("");
        config.setCmd(new String[]{ "/bin/karaf" });
        config.setWorkingDir("");
        config.setOpenStdin(true);
        config.setStdinOnce(true);
        Map<String, Map<String, String>> exposedPorts = new HashMap<>();
        Map<String, String> exposedPort = new HashMap<>();
        exposedPorts.put("8101/tcp", exposedPort);
        config.setExposedPorts(exposedPorts);

        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(false);
        hostConfig.setPublishAllPorts(false);

        // getting the dock
        File dock = new File("/tmp/docker", "karaf");
        if (dock.exists()) {
            hostConfig.setBinds(new String[]{dock.getAbsolutePath() + ":/opt/apache-karaf"});
        }

        hostConfig.setNetworkMode("bridge");
        hostConfig.setLxcConf(new String[]{});

        Map<String, List<HostPortBinding>> portBindings = new HashMap<>();
        List<HostPortBinding> hostPortBindings = new ArrayList<>();
        HostPortBinding hostPortBinding = new HostPortBinding();
        hostPortBinding.setHostIp("");
        hostPortBinding.setHostPort("8101");
        hostPortBindings.add(hostPortBinding);
        portBindings.put("8101/tcp", hostPortBindings);
        hostConfig.setPortBindings(portBindings);

        config.setHostConfig(hostConfig);

        dockerClient.create(config, "karaf");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testStartContainer() throws Exception {
        dockerClient.start("test");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testStopContainer() throws Exception {
        dockerClient.stop("test", 30);
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testRestartContainer() throws Exception {
        dockerClient.restart("test", 30);
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testKillContainer() throws Exception {
        dockerClient.kill("test", "SIGKILL");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testRemoveContainer() throws Exception {
        dockerClient.rm("test", true, true);
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testRenameContainer() throws Exception {
        dockerClient.rename("test", "karaf-test");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testPauseContainer() throws Exception {
        dockerClient.pause("test");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testUnpauseContainer() throws Exception {
        dockerClient.unpause("test");
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testContainerLog() throws Exception {
        System.out.println(dockerClient.logs("test", true, true, true, true));
    }

    @Test
    @Ignore("Need a running Docker daemon")
    public void testContainerTop() throws Exception {
        Top top = dockerClient.top("test");
        for (String title : top.getTitles()) {
            System.out.print(title);
            System.out.print("\t");
        }
        System.out.println();
        for (List<String> process : top.getProcesses()) {
            for (String p : process) {
                System.out.print(p);
                System.out.print("\t");
            }
            System.out.println();
        }
    }

}