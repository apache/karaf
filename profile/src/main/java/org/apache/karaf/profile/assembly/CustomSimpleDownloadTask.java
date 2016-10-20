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
package org.apache.karaf.profile.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.deployer.blueprint.BlueprintTransformer;
import org.apache.karaf.deployer.spring.SpringTransformer;
import org.apache.karaf.features.internal.download.impl.AbstractRetryableDownloadTask;
import org.apache.karaf.profile.Profile;

public class CustomSimpleDownloadTask extends AbstractRetryableDownloadTask {

    private static final String WRAP_URI_PREFIX = "wrap";
    private static final String SPRING_URI_PREFIX = "spring";
    private static final String BLUEPRINT_URI_PREFIX = "blueprint";
    private static final String WAR_URI_PREFIX = "war";
    private static final String PROFILE_URI_PREFIX = "profile";

    private final Profile profile;

    public CustomSimpleDownloadTask(ScheduledExecutorService executorService, Profile profile, String url) {
        super(executorService, url);
        this.profile = profile;
    }

    @Override
    protected File download(Exception previousExceptionNotUsed) throws Exception {
        URL url = createUrl(getUrl());
        Path path = Files.createTempFile("download-", null);
        try (InputStream is = url.openStream()) {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return path.toFile();
    }

    private URL createUrl(String url) throws MalformedURLException, URISyntaxException {
        URLStreamHandler handler = getUrlStreamHandler(url);
        if (handler != null) {
            return new URL(null, url, handler);
        } else {
            return new URL(url);
        }
    }

    private URLStreamHandler getUrlStreamHandler(String url) throws URISyntaxException {
        if(url.contains("\\")){
            url = url.replace("\\","/");
        }
        String scheme = url.substring(0, url.indexOf(':'));
        switch (scheme) {
        case WRAP_URI_PREFIX:
            return new org.ops4j.pax.url.wrap.Handler();
        case WAR_URI_PREFIX:
            return new org.ops4j.pax.url.war.Handler();
        case SPRING_URI_PREFIX:
            return new SpringURLHandler();
        case BLUEPRINT_URI_PREFIX:
            return new BlueprintURLHandler();
        case PROFILE_URI_PREFIX:
            if (profile != null) {
                return new ProfileURLHandler();
            }
        default:
            return null;
        }
    }

    public class SpringURLHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        SpringTransformer.transform(createUrl(url.getPath()), os);
                        os.close();
                        return new ByteArrayInputStream(os.toByteArray());
                    } catch (Exception e) {
                        throw new IOException("Error opening spring xml url", e);
                    }
                }
            };
        }
    }

    public class BlueprintURLHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        BlueprintTransformer.transform(createUrl(url.getPath()), os);
                        os.close();
                        return new ByteArrayInputStream(os.toByteArray());
                    } catch (Exception e) {
                        throw new IOException("Error opening blueprint xml url", e);
                    }
                }
            };
        }
    }

    public class ProfileURLHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    String path = url.getPath();
                    byte[] data = profile.getFileConfiguration(path);
                    if (data == null) {
                        throw new FileNotFoundException(url.toExternalForm());
                    }
                    return new ByteArrayInputStream(data);
                }
            };
        }
    }
}
