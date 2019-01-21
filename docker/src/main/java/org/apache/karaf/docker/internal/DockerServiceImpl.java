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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

public class DockerServiceImpl implements DockerService {

    private File storageLocation;

    public File getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(File storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public List<Container> ps(boolean showAll, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.ps(showAll);
    }

    @Override
    public Info info(String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);

        return dockerClient.info();
    }

    @Override
    public Version version(String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.version();
    }

    @Override
    public void create(String name, String url) throws Exception {
        // pull the java:8-jre-alpine image
        // TODO it should be changed to the official karaf image as soon as it's available on Docker HUB
        pull("java", "8-jre-alpine", true, url);

        // create a default Karaf docker container configuration
        ContainerConfig config = new ContainerConfig();
        config.setTty(true);
        config.setAttachStdin(true);
        config.setAttachStderr(true);
        config.setAttachStdout(true);
        // TODO it should be changed to the official karaf image as soon as it's available on Docker HUB
        config.setImage("java:8-jre-alpine");
        config.setHostname("");
        config.setUser("");
        config.setCmd(new String[]{ "/opt/apache-karaf/bin/karaf" });
        config.setWorkingDir("");
        config.setOpenStdin(true);
        config.setStdinOnce(true);
        Map<String, Map<String, String>> exposedPorts = new HashMap<>();
        exposedPorts.put("8101/tcp", new HashMap<>());
        exposedPorts.put("1099/tcp", new HashMap<>());
        exposedPorts.put("44444/tcp", new HashMap<>());
        exposedPorts.put("8181/tcp", new HashMap<>());
        config.setExposedPorts(exposedPorts);

        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(false);
        hostConfig.setPublishAllPorts(false);

        // getting the container storage
        File containerStorage = new File(storageLocation, name);
        if (containerStorage.exists()) {
            hostConfig.setBinds(new String[]{ containerStorage.getAbsolutePath() + ":/opt/apache-karaf" });
        }

        hostConfig.setNetworkMode("bridge");
        hostConfig.setLxcConf(new String[]{});

        Map<String, List<HostPortBinding>> portBindings = new HashMap<>();
        // ssh
        List<HostPortBinding> sshPortBindings = new ArrayList<>();
        HostPortBinding sshPortBinding = new HostPortBinding();
        sshPortBinding.setHostIp("");
        sshPortBinding.setHostPort("8101");
        sshPortBindings.add(sshPortBinding);
        portBindings.put("8101/tcp", sshPortBindings);
        // jmx rmi
        List<HostPortBinding> jmxPortBindings = new ArrayList<>();
        HostPortBinding jmxPortBinding = new HostPortBinding();
        jmxPortBinding.setHostIp("");
        jmxPortBinding.setHostPort("1099");
        jmxPortBindings.add(jmxPortBinding);
        portBindings.put("1099/tcp", jmxPortBindings);
        // jmx rmi registry
        List<HostPortBinding> jmxRegistryPortBindings = new ArrayList<>();
        HostPortBinding jmxRegistryPortBinding = new HostPortBinding();
        jmxRegistryPortBinding.setHostIp("");
        jmxRegistryPortBinding.setHostPort("44444");
        jmxRegistryPortBindings.add(jmxRegistryPortBinding);
        portBindings.put("44444/tcp", jmxRegistryPortBindings);
        // http
        List<HostPortBinding> httpPortBindings = new ArrayList<>();
        HostPortBinding httpPortBinding = new HostPortBinding();
        httpPortBinding.setHostIp("");
        httpPortBinding.setHostPort("8181");
        httpPortBindings.add(httpPortBinding);
        portBindings.put("8181/tcp", httpPortBindings);

        hostConfig.setPortBindings(portBindings);

        config.setHostConfig(hostConfig);

        create(name, url, config);
    }

    @Override
    public void create(String name, String url, ContainerConfig config) throws Exception {
        // creating the docker container
        DockerClient dockerClient = new DockerClient(url);

        dockerClient.create(config, name);
    }

    @Override
    public void provision(String name, String sshPort, String jmxRmiPort, String jmxRmiRegistryPort, String httpPort, boolean copy, String url) throws Exception {
        // pull the java:8-jre-alpine image
        pull("java", "8-jre-alpine", true, url);

        // use Karaf instance as base
        File karafBase = new File(System.getProperty("karaf.base"));
        File containerStorage;
        if (copy) {
            containerStorage = new File(storageLocation, name);
            containerStorage.mkdirs();
            copy(karafBase, containerStorage);
            makeFileExecutable(new File(containerStorage, "bin/karaf"));
            makeFileExecutable(new File(containerStorage, "bin/client"));
            makeFileExecutable(new File(containerStorage, "bin/inc"));
            makeFileExecutable(new File(containerStorage, "bin/instance"));
            makeFileExecutable(new File(containerStorage, "bin/setenv"));
            makeFileExecutable(new File(containerStorage, "bin/shell"));
            makeFileExecutable(new File(containerStorage, "bin/start"));
            makeFileExecutable(new File(containerStorage, "bin/status"));
            makeFileExecutable(new File(containerStorage, "bin/stop"));
        } else {
            containerStorage = karafBase;
        }

        // creating the Karaf Docker container
        DockerClient dockerClient = new DockerClient(url);
        ContainerConfig config = new ContainerConfig();
        config.setTty(true);
        config.setAttachStdout(true);
        config.setAttachStderr(true);
        config.setAttachStdin(true);
        config.setImage("java:8-jre-alpine");
        config.setHostname("");
        config.setUser("");
        config.setCmd(new String[]{ "/opt/apache-karaf/bin/karaf" });
        config.setWorkingDir("");
        config.setOpenStdin(true);
        config.setStdinOnce(true);
        Map<String, Map<String, String>> exposedPorts = new HashMap<>();
        exposedPorts.put("8101/tcp", new HashMap<>());
        exposedPorts.put("1099/tcp", new HashMap<>());
        exposedPorts.put("44444/tcp", new HashMap<>());
        exposedPorts.put("8181/tcp", new HashMap<>());
        config.setExposedPorts(exposedPorts);

        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(false);
        hostConfig.setPublishAllPorts(false);

        // binding filesystem
        hostConfig.setBinds(new String[]{containerStorage.getAbsolutePath() + ":/opt/apache-karaf"});

        hostConfig.setNetworkMode("bridge");
        hostConfig.setLxcConf(new String[]{});

        Map<String, List<HostPortBinding>> portBindings = new HashMap<>();
        // ssh
        List<HostPortBinding> sshPortBindings = new ArrayList<>();
        HostPortBinding sshPortBinding = new HostPortBinding();
        sshPortBinding.setHostIp("");
        sshPortBinding.setHostPort(sshPort);
        sshPortBindings.add(sshPortBinding);
        portBindings.put("8101/tcp", sshPortBindings);
        // jmx rmi
        List<HostPortBinding> jmxPortBindings = new ArrayList<>();
        HostPortBinding jmxPortBinding = new HostPortBinding();
        jmxPortBinding.setHostIp("");
        jmxPortBinding.setHostPort(jmxRmiPort);
        jmxPortBindings.add(jmxPortBinding);
        portBindings.put("1099/tcp", jmxPortBindings);
        // jmx rmi registry
        List<HostPortBinding> jmxRegistryPortBindings = new ArrayList<>();
        HostPortBinding jmxRegistryPortBinding = new HostPortBinding();
        jmxRegistryPortBinding.setHostIp("");
        jmxRegistryPortBinding.setHostPort(jmxRmiRegistryPort);
        jmxRegistryPortBindings.add(jmxRegistryPortBinding);
        portBindings.put("44444/tcp", jmxRegistryPortBindings);
        // http
        List<HostPortBinding> httpPortBindings = new ArrayList<>();
        HostPortBinding httpPortBinding = new HostPortBinding();
        httpPortBinding.setHostIp("");
        httpPortBinding.setHostPort(httpPort);
        httpPortBindings.add(httpPortBinding);
        portBindings.put("8181/tcp", httpPortBindings);

        hostConfig.setPortBindings(portBindings);

        config.setHostConfig(hostConfig);

        dockerClient.create(config, name);
    }

    @Override
    public void start(String name, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.start(name);
    }

    @Override
    public void stop(String name, int timeToWait, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.stop(name, timeToWait);
    }

    @Override
    public void restart(String name, int timeToWait, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.restart(name, timeToWait);
    }

    @Override
    public void kill(String name, String signal, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.kill(name, signal);
    }

    @Override
    public void pause(String name, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.pause(name);
    }

    @Override
    public void unpause(String name, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.unpause(name);
    }

    @Override
    public void rename(String name, String newName, String url) throws  Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.rename(name, newName);
    }

    @Override
    public void rm(String name, boolean removeVolumes, boolean force, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.rm(name, removeVolumes, force);
        File containerStorage = new File(storageLocation, name);
        if (containerStorage.exists()) {
            containerStorage.delete();
        }
    }

    @Override
    public String logs(String name, boolean stdout, boolean stderr, boolean timestamps, boolean details, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        String log = dockerClient.logs(name, stdout, stderr, timestamps, details);
        return log;
    }

    @Override
    public Top top(String name, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.top(name);
    }

    @Override
    public void commit(String name, String repo, String tag, String message, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.commit(name, null, message, repo, tag);
    }

    @Override
    public List<Image> images(String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.images(true);
    }

    @Override
    public void pull(String image, String tag, boolean verbose, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.pull(image, tag, verbose);
    }

    @Override
    public List<ImageSearch> search(String term, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.search(term);
    }

    @Override
    public Container inspect(String name, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.inspect(name);
    }

    @Override
    public void push(String image, String tag, boolean verbose, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.push(image, tag, verbose);
    }

    @Override
    public List<ImageHistory> history(String image, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        return dockerClient.history(image);
    }

    @Override
    public void tag(String image, String repo, String tag, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.tag(image, repo, tag);
    }

    @Override
    public void rmi(String image, boolean force, boolean noprune, String url) throws Exception {
        DockerClient dockerClient = new DockerClient(url);
        dockerClient.rmi(image, force, noprune);
    }

    private void copy(File source, File destination) throws IOException {
        if (source.getName().equals("docker")) {
            // ignore inner docker
            return;
        }
        if (source.getName().equals("cache.lock")) {
            // ignore cache.lock file
            return;
        }
        if (source.getName().equals("lock")) {
            // ignore lock file
            return;
        }
        if (source.getName().matches("transaction_\\d+\\.log")) {
            // ignore active txlog files
            return;
        }
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] children = source.list();
            for (String child : children) {
                copy(new File(source, child), new File(destination, child));
            }
        } else {
            try (
                    InputStream in = new FileInputStream(source);
                    OutputStream out = new FileOutputStream(destination)
            ) {
                new StreamUtils().copy(in, out);
            }
        }
    }

    class StreamUtils {

        public StreamUtils() {
        }

        public void close(Closeable... closeables) {
            for (Closeable c : closeables) {
                try {
                    if (c != null) {
                        c.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        public void close(Iterable<Closeable> closeables) {
            for (Closeable c : closeables) {
                try {
                    if (c != null) {
                        c.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        public void copy(final InputStream input, final OutputStream output) throws IOException {
            byte[] buffer = new byte[1024 * 16];
            int n;
            while ((n = input.read(buffer)) > 0) {
                output.write(buffer, 0, n);
            }
            output.flush();
        }

    }

    private void makeFileExecutable(File serviceFile) throws IOException {
        Set<PosixFilePermission> permissions = new HashSet<>();
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.GROUP_EXECUTE);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        // Get the existing permissions and add the executable permissions to them
        Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(serviceFile.toPath());
        filePermissions.addAll(permissions);
        Files.setPosixFilePermissions(serviceFile.toPath(), filePermissions);
    }

}
