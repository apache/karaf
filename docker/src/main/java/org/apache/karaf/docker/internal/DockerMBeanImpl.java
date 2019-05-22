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
package org.apache.karaf.docker.internal;

import org.apache.karaf.docker.*;

import javax.management.MBeanException;
import javax.management.openmbean.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DockerMBeanImpl implements DockerMBean {

    private DockerService dockerService;

    public void setDockerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @Override
    public TabularData ps(boolean showAll, String url) throws MBeanException {
        try {
            CompositeType containerType = new CompositeType("container", "Docker Container",
                    new String[]{"Id", "Names", "Command", "Created", "Image", "Status"},
                    new String[]{"Container ID", "Container Names", "Command run in the container", "Container creation time", "Image used by the container", "Current container status"},
                    new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.LONG, SimpleType.STRING, SimpleType.STRING});
            TabularType tableType = new TabularType("containers", "Docker containers", containerType, new String[]{ "Id" });
            TabularData table = new TabularDataSupport(tableType);
            for (Container container : dockerService.ps(showAll, url)) {
                CompositeData data = new CompositeDataSupport(containerType,
                        new String[]{"Id", "Names", "Command", "Created", "Image", "Status"},
                        new Object[]{container.getId(), container.getNames(), container.getCommand(), container.getCreated(), container.getImage(), container.getStatus()});
                table.put(data);
            }
            return table;
        } catch (Exception e) {
            throw new MBeanException(e);
        }
    }

    @Override
    public Map<String, String> info(String url) throws MBeanException {
        try {
            Info info = dockerService.info(url);
            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("Containers", Integer.toString(info.getContainers()));
            infoMap.put("Debug", Boolean.toString(info.isDebug()));
            infoMap.put("Driver", info.getDriver());
            infoMap.put("ExecutionDriver", info.getExecutionDriver());
            infoMap.put("IPv4Forwarding", Boolean.toString(info.isIpv4Forwarding()));
            infoMap.put("Images", Integer.toString(info.getImages()));
            infoMap.put("IndexServerAddress", info.getIndexServerAddress());
            infoMap.put("InitPath", info.getInitPath());
            infoMap.put("InitSha1", info.getInitSha1());
            infoMap.put("KernelVersion", info.getKernelVersion());
            infoMap.put("MemoryLimit", Boolean.toString(info.isMemoryLimit()));
            infoMap.put("NEventsListener", Boolean.toString(info.isnEventsListener()));
            infoMap.put("NFd", Integer.toString(info.getNfd()));
            infoMap.put("NGoroutines", Integer.toString(info.getNgoroutines()));
            infoMap.put("SwapLimit", Boolean.toString(info.isSwapLimit()));
            return infoMap;
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    @Override
    public void provision(String name, String sshPort, String jmxRmiPort, String jmxRmiRegistryPort, String httpPort, boolean copy, String url) throws MBeanException {
        try {
            dockerService.provision(name, sshPort, jmxRmiPort, jmxRmiRegistryPort, httpPort, copy, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void rm(String name, boolean removeVolumes, boolean force, String url) throws MBeanException {
        try {
            dockerService.rm(name, removeVolumes, force, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void start(String name, String url) throws MBeanException {
        try {
            dockerService.start(name, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void stop(String name, int timeToWait, String url) throws MBeanException {
        try {
            dockerService.stop(name, timeToWait, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public String logs(String name, boolean stdout, boolean stderr, boolean timestamps, boolean details, String url) throws MBeanException {
        try {
            if (!stdout && !stderr) {
                throw new MBeanException(null, "You have to choose at least one stream: stdout or stderr");
            }
            return dockerService.logs(name, stdout, stderr, timestamps, details, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void commit(String name, String repo, String tag, String message, String url) throws MBeanException {
        try {
            dockerService.commit(name, repo, url, tag, message);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public TabularData images(String url) throws MBeanException {
        try {
            CompositeType type = new CompositeType("Image", "Docker Image",
                    new String[]{ "Id", "Created", "RepoTags", "Size"},
                    new String[]{ "Image Id", "Image Creation Date", "Image repository and tag", "Image size"},
                    new OpenType[]{ SimpleType.STRING, SimpleType.LONG, SimpleType.STRING, SimpleType.LONG});
            TabularType tableType = new TabularType("Images", "List of Docker Image", type, new String[]{ "Id" });
            TabularData table = new TabularDataSupport(tableType);
            for (Image image : dockerService.images(url)) {
                CompositeData data = new CompositeDataSupport(type,
                        new String[]{ "Id", "Created", "RepoTags", "Size"},
                        new Object[]{ image.getId(), image.getCreated(), image.getRepoTags(), image.getSize() });
                table.put(data);
            }
            return table;
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void pull(String image, String tag, String url) throws MBeanException {
        try {
            dockerService.pull(image, tag, false, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public Map<String, String> version(String url) throws MBeanException {
        try {
            Version version = dockerService.version(url);
            Map<String, String> versionMap = new HashMap<>();
            versionMap.put("Experimental", version.getExperimental());
            versionMap.put("ApiVersion", version.getApiVersion());
            versionMap.put("Arch", version.getArch());
            versionMap.put("BuildTime", version.getBuildTime());
            versionMap.put("GitCommit", version.getGitCommit());
            versionMap.put("GoVersion", version.getGoVersion());
            versionMap.put("KernelVersion", version.getKernelVersion());
            versionMap.put("OS", version.getOs());
            versionMap.put("Version", version.getVersion());
            return  versionMap;
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void rename(String container, String newName, String url) throws MBeanException {
        try {
            dockerService.rename(container, newName, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void restart(String container, int timeToWait, String url) throws MBeanException {
        try {
            dockerService.restart(container, timeToWait, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void kill(String container, String signal, String url) throws MBeanException {
        try {
            if (signal == null) {
                signal = "SIGKILL";
            }
            dockerService.kill(container, signal, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void pause(String container, String url) throws MBeanException {
        try {
            dockerService.pause(container, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void unpause(String container, String url) throws MBeanException {
        try {
            dockerService.unpause(container, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public TabularData search(String term, String url) throws MBeanException {
        try {
            CompositeType imageType = new CompositeType("image", "Image",
                    new String[]{"Name", "StarCount", "Official", "Automated", "Description"},
                    new String[]{"Name", "StarCount", "Official", "Automated", "Description"},
                    new OpenType[]{SimpleType.STRING, SimpleType.INTEGER, SimpleType.BOOLEAN, SimpleType.BOOLEAN, SimpleType.STRING});
            TabularType tableType = new TabularType("images", "Images", imageType, new String[]{"Name"});
            TabularData table = new TabularDataSupport(tableType);
            List<ImageSearch> images = dockerService.search(term, url);
            for (ImageSearch image : images) {
                CompositeData data = new CompositeDataSupport(imageType,
                        new String[]{"Name", "StarCount", "Official", "Automated", "Description"},
                        new Object[]{image.getName(), image.getStarCount(), image.isOfficial(), image.isAutomated(), image.getDescription()});
                table.put(data);
            }
            return table;
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void tag(String image, String tag, String repo, String url) throws MBeanException {
        try {
            dockerService.tag(image, tag, repo, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void rmi(String image, boolean force, boolean noprune, String url) throws MBeanException {
        try {
            dockerService.rmi(image, force, noprune, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void push(String image, String tag, String url) throws MBeanException {
        try {
            dockerService.push(image, tag, false, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }
}
