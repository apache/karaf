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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
 * A read only resource set serving the entries of a bundle and of its fragments, the root of the bundle being the
 * root of the web application.
 */
public class BundleResourceSet extends LifecycleBase implements WebResourceSet {

    private WebResourceRoot root;
    private final Bundle bundle;
    private volatile Entries entries;
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
        Entries current = entries();
        String entryPath = path.substring(1);
        String name = entryPath.endsWith("/") ? entryPath.substring(0, entryPath.length() - 1) : entryPath;
        if (current.directories.containsKey(name)) {
            return new BundleResource(root, path, current.directories.get(name), true, bundle.getLastModified());
        }
        URL url = entryPath.endsWith("/") ? null : current.files.get(name);
        if (url != null) {
            return new BundleResource(root, path, url, false, bundle.getLastModified());
        }
        return new EmptyResource(root, path);
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

    /**
     * @return the paths of the entries in a directory, the ones of the directories with a trailing slash.
     */
    private List<String> children(String path) {
        String name = path.startsWith("/") ? path.substring(1) : path;
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        Set<String> children = entries().children.get(name);
        return children == null ? List.of() : new ArrayList<>(children);
    }

    private Entries entries() {
        Entries current = entries;
        if (current == null) {
            synchronized (this) {
                current = entries;
                if (current == null) {
                    current = Entries.read(bundle);
                    entries = current;
                }
            }
        }
        return current;
    }

    /**
     * The entries of the bundle and of its fragments, the resources of a web application bundle as defined by the
     * OSGi Web Applications specification. They are read once: the content of a bundle does not change while it is
     * started, and listing the entries of a bundle goes through all of them.
     */
    static final class Entries {

        // the files and the directories, by path without a trailing slash, the root directory being the empty path
        private final Map<String, URL> files = new HashMap<>();
        // the URL of a directory is null when the bundle only has entries inside
        private final Map<String, URL> directories = new HashMap<>();
        private final Map<String, Set<String>> children = new HashMap<>();

        static Entries read(Bundle bundle) {
            Entries entries = new Entries();
            entries.directories.put("", null);
            entries.children.put("", new LinkedHashSet<>());
            Enumeration<URL> urls = bundle.findEntries("/", "*", true);
            while (urls != null && urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String path = url.getPath();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                if (path.isEmpty()) {
                    continue;
                }
                // the bundle first, then the fragments: the first entry wins
                if (path.endsWith("/")) {
                    String name = path.substring(0, path.length() - 1);
                    if (entries.directories.get(name) == null) {
                        entries.directories.put(name, url);
                    }
                    entries.add(name, true);
                } else if (!entries.files.containsKey(path)) {
                    entries.files.put(path, url);
                    entries.add(path, false);
                }
            }
            // depending on how the bundle is stored, the entry of a directory may have no trailing slash
            for (String name : entries.directories.keySet()) {
                if (entries.files.remove(name) != null) {
                    entries.children.get(parent(name)).remove(name);
                }
            }
            return entries;
        }

        private void add(String name, boolean directory) {
            String parent = parent(name);
            Set<String> siblings = children.get(parent);
            if (siblings == null) {
                siblings = new LinkedHashSet<>();
                children.put(parent, siblings);
                // a directory without entry, but with entries inside
                directories.putIfAbsent(parent, null);
                add(parent, true);
            }
            siblings.add(directory ? name + "/" : name);
            if (directory) {
                children.putIfAbsent(name, new LinkedHashSet<>());
            }
        }

        private static String parent(String name) {
            int slash = name.lastIndexOf('/');
            return slash < 0 ? "" : name.substring(0, slash);
        }

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
