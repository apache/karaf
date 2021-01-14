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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.model.Config;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Content;
import org.apache.karaf.features.internal.model.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs PID configuration to <code>${karaf.etc}</code> and <code>system/</code> directory.
 */
public class ConfigInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigInstaller.class);
    private Path etcDirectory;
    private List<String> pidsToExtract;
    
    public ConfigInstaller(Path etcDirectory, List<String> pidsToExtract) {
        this.etcDirectory = etcDirectory;
        this.pidsToExtract = pidsToExtract;
    }

    public void installConfigs(Feature feature, Downloader downloader, ArtifactInstaller installer)
        throws Exception {
        List<Content> contents = new ArrayList<>();
        contents.add(feature);
        contents.addAll(feature.getConditional());
        for (Content content : contents) {
            // Install config files
            for (Config config : content.getConfig()) {
                if (config.isExternal()) {
                    installer.installArtifact(config.getValue().trim());
                }
            }
            for (ConfigFile configFile : content.getConfigfile()) {
                installer.installArtifact(configFile.getLocation().trim());
            }
            // Extract configs
            Path homeDirectory = etcDirectory.getParent();
            for (Config config : content.getConfig()) {
                if (pidMatching(config.getName())) {
                    Path configFile = etcDirectory.resolve(config.getName() + ".cfg");
                    boolean configFileExist = Files.exists(configFile);
                    if (!config.isAppend() && configFileExist) {
                        LOGGER.info("      not changing existing config file: {}", homeDirectory.relativize(configFile));
                        continue;
                    }
                    if (config.isExternal()) {
                        downloader.download(config.getValue().trim(), provider -> {
                            synchronized (provider) {
                                if (config.isAppend()) {
                                    byte[] data = Files.readAllBytes(provider.getFile().toPath());
                                    LOGGER.info("      appending to config file: {}", homeDirectory.relativize(configFile));
                                    Files.write(configFile, data, StandardOpenOption.APPEND);
                                } else {
                                    LOGGER.info("      adding config file: {}", homeDirectory.relativize(configFile));
                                    Files.copy(provider.getFile().toPath(), configFile, StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                        });
                    } else {
                        if (config.isAppend()) {
                            if (configFileExist) {
                                LOGGER.info("      appending to config file: {}", homeDirectory.relativize(configFile));
                                Files.write(configFile, config.getValue().getBytes(), StandardOpenOption.APPEND);
                            }
                            else
                                LOGGER.warn("      Could not append, because config file does not exist: {}", homeDirectory.relativize(configFile));
                            
                        } else {
                            LOGGER.info("      adding config file: {}", homeDirectory.relativize(configFile));
                            Files.write(configFile, config.getValue().getBytes());
                        }
                    }
                }
            }
        }
    }

    private boolean pidMatching(String name) {
        if (pidsToExtract == null) {
            return true;
        }
        for (String p : pidsToExtract) {
            boolean negated = false;
            if (p.startsWith("!")) {
                negated = true;
                p = p.substring(1);
            }
            String r = globToRegex(p);
            if (Pattern.matches(r, name)) {
                return !negated;
            }
        }
        return false;
    }

    private String globToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
