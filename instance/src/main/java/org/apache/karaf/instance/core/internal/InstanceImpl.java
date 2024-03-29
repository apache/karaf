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
package org.apache.karaf.instance.core.internal;

import org.apache.karaf.instance.core.Instance;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class InstanceImpl implements Instance {

    private final InstanceServiceImpl service;
    private String name;

    public InstanceImpl(InstanceServiceImpl service, String name) {
        this.service = service;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    void doSetName(String name) {
        this.name = name;
    }

    public boolean isRoot() {
        return service.isInstanceRoot(name);
    }

    public String getLocation() {
        return service.getInstanceLocation(name);
    }

    public int getPid() {
        return service.getInstancePid(name);
    }

    public int getSshPort() {
        return service.getInstanceSshPort(name);
    }

    public String getSshHost() {
        return service.getInstanceSshHost(name);
    }

    public void changeSshPort(int port) throws Exception {
        service.changeInstanceSshPort(name, port);
    }

    public int getRmiRegistryPort() {
        return service.getInstanceRmiRegistryPort(name);
    }

    public void changeRmiRegistryPort(int port) throws Exception {
        service.changeInstanceRmiRegistryPort(name, port);
    }

    public String getRmiRegistryHost() {
        return service.getInstanceRmiRegistryHost(name);
    }

    public int getRmiServerPort() {
        return service.getInstanceRmiServerPort(name);
    }

    public void changeRmiServerPort(int port) throws Exception {
        service.changeInstanceRmiServerPort(name, port);
    }

    public String getRmiServerHost() {
        return service.getInstanceRmiServerHost(name);
    }

    public String getJavaOpts() {
        return service.getInstanceJavaOpts(name);
    }

    public void changeJavaOpts(String javaOpts) throws Exception {
        service.changeInstanceJavaOpts(name, javaOpts);
    }

    public void restart(String javaOpts) throws Exception {
        service.restartInstance(name, javaOpts);
    }

    public void start(String javaOpts) throws Exception {
        service.startInstance(name, javaOpts);
    }

    public void stop() throws Exception {
        service.stopInstance(name);
    }

    public void destroy() throws Exception {
        service.destroyInstance(name);
    }

    public String getState() throws Exception {
        return service.getInstanceState(name);
    }

    public void changeSshHost(String host) throws Exception {
        service.changeInstanceSshHost(name, host);
    }

    public void packageInZip(String destination) throws Exception {
        Path sourcePath = Paths.get(getLocation());
        Path destinationPath = Paths.get(destination).normalize();
        try (ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(destination.toString()))) {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (attributes.isSymbolicLink()) {
                        return FileVisitResult.CONTINUE;
                    }

                    String entryName = sourcePath.relativize(file).toString();

                    if (Paths.get(entryName).normalize().equals(destinationPath)) {
                        // Prevent the zip from trying to zip itself when ran in the instance directory,
                        // e.g. when executing instance:package root ./archives/root.zip
                        return FileVisitResult.CONTINUE;
                    }

                    ZipEntry zipEntry = new ZipEntry(entryName);
                    System.out.println(entryName);
                    zOut.putNextEntry(zipEntry);
                    Files.copy(file, zOut);
                    zOut.closeEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
