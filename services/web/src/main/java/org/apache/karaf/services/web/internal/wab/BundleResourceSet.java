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
package org.apache.karaf.services.web.internal.wab;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.util.LifecycleBase;
import org.apache.catalina.webresources.EmptyResource;
import org.osgi.framework.Bundle;

/**
 * A read only resource set serving the entries of a bundle, the root of the bundle being the root of the web
 * application.
 */
public class BundleResourceSet extends LifecycleBase implements WebResourceSet {

    private WebResourceRoot root;
    private final Bundle bundle;
    private boolean classLoaderOnly;
    private boolean staticOnly;

    public BundleResourceSet(WebResourceRoot root, Bundle bundle) {
        this.root = root;
        this.bundle = bundle;
    }

    @Override
    public WebResource getResource(String path) {
        if ("/".equals(path)) {
            return new BundleResource(root, path, bundle.getEntry("/"), true, bundle.getLastModified());
        }
        String entryPath = path.substring(1);
        String directoryPath = entryPath.endsWith("/") ? entryPath : entryPath + "/";
        // a directory is detected by its content: depending on how the bundle is stored, the entry of a directory
        // is returned for its path without the trailing slash, and the entry of a file for its path with one
        if (hasChildren(directoryPath)) {
            return new BundleResource(root, path, bundle.getEntry(directoryPath), true, bundle.getLastModified());
        }
        URL url = entryPath.endsWith("/") ? null : bundle.getEntry(entryPath);
        if (url != null) {
            return new BundleResource(root, path, url, false, bundle.getLastModified());
        }
        return new EmptyResource(root, path);
    }

    /**
     * Also true for a directory without entry, but with entries inside.
     */
    private boolean hasChildren(String directoryPath) {
        Enumeration<String> children = bundle.getEntryPaths(directoryPath);
        return children != null && children.hasMoreElements();
    }

    @Override
    public String[] list(String path) {
        List<String> names = new ArrayList<>();
        for (String child : children(path)) {
            String name = child.endsWith("/") ? child.substring(0, child.length() - 1) : child;
            names.add(name.substring(name.lastIndexOf('/') + 1));
        }
        return names.toArray(new String[0]);
    }

    @Override
    public Set<String> listWebAppPaths(String path) {
        Set<String> paths = new LinkedHashSet<>();
        for (String child : children(path)) {
            paths.add("/" + child);
        }
        return paths;
    }

    private List<String> children(String path) {
        String entryPath = path.startsWith("/") ? path.substring(1) : path;
        if (!entryPath.isEmpty() && !entryPath.endsWith("/")) {
            entryPath = entryPath + "/";
        }
        Enumeration<String> entries = bundle.getEntryPaths(entryPath.isEmpty() ? "/" : entryPath);
        return entries == null ? Collections.emptyList() : Collections.list(entries);
    }

    @Override
    public boolean mkdir(String path) {
        return false;
    }

    @Override
    public boolean write(String path, InputStream is, boolean overwrite) {
        return false;
    }

    @Override
    public void setRoot(WebResourceRoot root) {
        this.root = root;
    }

    @Override
    public boolean getClassLoaderOnly() {
        return classLoaderOnly;
    }

    @Override
    public void setClassLoaderOnly(boolean classLoaderOnly) {
        this.classLoaderOnly = classLoaderOnly;
    }

    @Override
    public boolean getStaticOnly() {
        return staticOnly;
    }

    @Override
    public void setStaticOnly(boolean staticOnly) {
        this.staticOnly = staticOnly;
    }

    @Override
    public URL getBaseUrl() {
        return bundle.getEntry("/");
    }

    @Override
    public void setReadOnly(boolean readOnly) {
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void gc() {
    }

    @Override
    public void setAllowLinking(boolean allowLinking) {
    }

    @Override
    public boolean getAllowLinking() {
        return false;
    }

    @Override
    protected void initInternal() throws LifecycleException {
    }

    @Override
    protected void startInternal() throws LifecycleException {
        setState(LifecycleState.STARTING);
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        setState(LifecycleState.STOPPING);
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
    }

}
