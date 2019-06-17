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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to manipulate docker via REST API.
 */
public class DockerClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(DockerClient.class);

    public static final String DEFAULT_URL = "http://localhost:2375";

    private String url;
    private ObjectMapper mapper;

    public DockerClient(String url) {
        if (url == null) {
            this.url = DEFAULT_URL;
        } else {
            this.url = url;
        }
        mapper = new ObjectMapper();
    }

    public Info info() throws Exception {
        URL dockerUrl = new URL(this.url + "/info");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        Info info = mapper.readValue(connection.getInputStream(), Info.class);
        return info;
    }

    public Version version() throws Exception {
        URL dockerUrl = new URL(this.url + "/version");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        Version version = mapper.readValue(connection.getInputStream(), Version.class);
        return version;
    }

    public List<Container> ps(boolean showAll) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/json?all=" + showAll);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        List<Container> containers = mapper.readValue(connection.getInputStream(), new TypeReference<List<Container>>(){});
        for (Container container : containers) {
            cleanName(container);
        }

        return containers;
    }

    private void cleanName(Container container) throws Exception {
        List<String> cleanNames = new ArrayList<>();
        for (String name : container.getNames()) {
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            cleanNames.add(name);
        }
        container.setNames(cleanNames);
    }

    public Container inspect(String id) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/json?size=1");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        Container container = mapper.readValue(connection.getInputStream(), Container.class);
        return container;
    }

    public Top top(String id) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/top?ps_args=aux");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        Top top = mapper.readValue(connection.getInputStream(), Top.class);
        return top;
    }

    public void create(ContainerConfig config, String name) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/create?name=" + name);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        mapper.writeValue(connection.getOutputStream(), config);

        if (connection.getResponseCode() != 201) {
            throw new IllegalStateException("Can't create Docker container " + name + ": " + connection.getResponseMessage());
        }
    }

    public void rm(String id, boolean removeVolumes, boolean force) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "?v=" + removeVolumes + "&force=" + force);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("DELETE");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't remove Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void start(String id) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/start");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't start Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void stop(String id, int timeToWait) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/stop?t=" + timeToWait);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't stop Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void restart(String id, int timeToWait) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/restart?t=" + timeToWait);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't restart Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void kill(String id, String signal) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/kill?signal=" + signal);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't kill Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void rename(String id, String name) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/rename?name=" + name);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't rename Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void pause(String id) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/pause");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't pause Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public void unpause(String id) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/unpause");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 204) {
            throw new IllegalStateException("Can't unpause Docker container " + id + ": " + connection.getResponseMessage());
        }
    }

    public String logs(String id, boolean stdout, boolean stderr, boolean timestamps, boolean details) throws Exception {
        URL dockerUrl = new URL(this.url + "/containers/" + id + "/logs?stdout=" + stdout + "&stderr=" + stderr + "&timestamps=" + timestamps + "&details=" + details);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        return buffer.toString();
    }

    public void commit(String container, ContainerConfig config, String message, String repo, String tag) throws Exception {
        URL dockerUrl = new URL(this.url + "/commit?container=" + container + "&comment=" + message + "&repo=" + repo + "&tag=" + tag);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        mapper.writeValue(connection.getOutputStream(), config);

        if (connection.getResponseCode() != 201) {
            throw new IllegalStateException("Can't commit Docker container " + container + ": " + connection.getResponseMessage());
        }
    }

    public List<Image> images(boolean showAll) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/json?all=" + showAll);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        List<Image> images = mapper.readValue(connection.getInputStream(), new TypeReference<List<Image>>(){});
        return images;
    }

    public void pull(String name, String tag, boolean verbose) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/create?fromImage=" + name + "&tag=" + tag);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            LOGGER.debug(line);
            if (verbose) {
                System.out.println(line);
            }
        }

        if (connection.getResponseCode() != 200) {
            throw new IllegalStateException("Can't pull image " + name + ": " + connection.getResponseMessage());
        }
    }

    public void push(String name, String tag, boolean verbose) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/" + name + "/push?tag=" + tag);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            LOGGER.debug(line);
            if (verbose) {
                System.out.println(line);
            }
        }

        if (connection.getResponseCode() != 200) {
            throw new IllegalStateException("Can't push image " +  name + ": " + connection.getResponseMessage());
        }
    }

    public void tag(String name, String repo, String tag) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/" + name + "/tag?repo=" + repo + "&tag=" + tag);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("POST");

        if (connection.getResponseCode() != 201) {
            throw new IllegalStateException("Can't tag image " + name + ": " + connection.getResponseMessage());
        }
    }

    public void rmi(String name, boolean force, boolean noprune) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/" + name + "?force=" + force + "&noprune=" + noprune);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("DELETE");

        if (connection.getResponseCode() != 200) {
            throw new IllegalStateException("Can't remove image " + name + ": " + connection.getResponseMessage());
        }
    }

    public List<ImageSearch> search(String term) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/search?term=" + term);
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        List<ImageSearch> images = mapper.readValue(connection.getInputStream(), new TypeReference<List<ImageSearch>>(){});
        return images;
    }

    public Image getImage(String name) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/" + name + "/json");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        Image image = mapper.readValue(connection.getInputStream(), Image.class);
        return image;
    }

    public List<ImageHistory> history(String name) throws Exception {
        URL dockerUrl = new URL(this.url + "/images/" + name + "/history");
        HttpURLConnection connection = (HttpURLConnection) dockerUrl.openConnection();
        connection.setRequestMethod("GET");

        List<ImageHistory> images = mapper.readValue(connection.getInputStream(), new TypeReference<List<ImageHistory>>(){});
        return images;
    }

}
