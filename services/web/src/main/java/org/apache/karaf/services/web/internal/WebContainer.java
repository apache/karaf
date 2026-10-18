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
package org.apache.karaf.services.web.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.WebService;
import org.apache.karaf.services.web.internal.wab.WabDeployment;
import org.apache.karaf.services.web.internal.wab.WabExtender;
import org.apache.karaf.services.web.internal.wab.WebApplication;
import org.apache.karaf.services.web.internal.whiteboard.Whiteboard;
import org.apache.karaf.services.web.internal.whiteboard.WhiteboardContextModel;
import org.apache.karaf.services.web.internal.whiteboard.WhiteboardDeployment;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The web container: the embedded Tomcat server, the whiteboard and the web application bundles.
 * <p>
 * The contexts are deployed by a single thread, which computes the contexts to deploy each time a whiteboard
 * service or a web application bundle comes, changes or goes, and applies the difference to Tomcat. A removal
 * waits for the context, or the servlet, to be undeployed, so that it happens while the service is still
 * registered and the bundle still active. The other changes are applied after a short delay, so that the
 * services registered together, typically by a bundle starting, are deployed together.
 */
public class WebContainer implements WebService, ModelListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebContainer.class);

    private static final long DEPLOY_DELAY = 100;

    private static final Comparator<WebElement> ORDER = Comparator
            .comparing(WebElement::getContextPath, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(WebElement::getName, Comparator.nullsLast(Comparator.naturalOrder()));

    private final BundleContext bundleContext;
    private final WebConfig config;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private volatile Thread executorThread;
    private volatile boolean stopping;

    private TomcatServer server;
    private Whiteboard whiteboard;
    private WabExtender wabExtender;

    // used by the executor thread only
    private final Map<String, Deployment> deployments = new LinkedHashMap<>();
    private Set<String> loggedFailures = new HashSet<>();

    private volatile List<WebElement> contexts = Collections.emptyList();
    private volatile List<WebElement> servlets = Collections.emptyList();
    private volatile List<WebElement> filters = Collections.emptyList();

    public WebContainer(BundleContext bundleContext, WebConfig config) {
        this.bundleContext = bundleContext;
        this.config = config;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "karaf-web");
            thread.setDaemon(true);
            executorThread = thread;
            return thread;
        });
    }

    public WebConfig getConfig() {
        return config;
    }

    public void start() throws Exception {
        server = new TomcatServer(config, bundleContext.getDataFile("tomcat"));
        server.start();
        LOGGER.info("Web container started on {}", String.join(", ", server.getEndpoints()));
        whiteboard = new Whiteboard(bundleContext, this);
        whiteboard.open();
        wabExtender = new WabExtender(bundleContext, this);
        wabExtender.open();
        changed(false);
    }

    public void stop() {
        stopping = true;
        // undeploy while the services are still tracked, so that they are released
        await(executor.submit(() -> {
            for (Deployment deployment : deployments.values()) {
                undeploy(deployment);
            }
            deployments.clear();
            if (server != null) {
                server.stop();
            }
        }));
        executor.shutdown();
        if (wabExtender != null) {
            wabExtender.close();
        }
        if (whiteboard != null) {
            whiteboard.close();
        }
        contexts = Collections.emptyList();
        servlets = Collections.emptyList();
        filters = Collections.emptyList();
        LOGGER.info("Web container stopped");
    }

    @Override
    public void changed(boolean removal) {
        if (stopping) {
            return;
        }
        if (removal && Thread.currentThread() != executorThread) {
            await(executor.submit(this::reconcile));
        } else if (scheduled.compareAndSet(false, true)) {
            executor.schedule(() -> {
                scheduled.set(false);
                reconcile();
            }, DEPLOY_DELAY, TimeUnit.MILLISECONDS);
        }
    }

    private void await(Future<?> future) {
        try {
            future.get(config.undeployTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOGGER.warn("The web container did not undeploy within {} ms, continuing", config.undeployTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("Error in the web container", e.getCause());
        }
    }

    /**
     * Computes the contexts to deploy, and applies the difference with the deployed ones.
     */
    private void reconcile() {
        if (stopping) {
            return;
        }
        try {
            List<WebElement> failures = new ArrayList<>();
            Map<String, Object> wanted = new LinkedHashMap<>();
            // the web application bundles first, they win over the whiteboard contexts with the same path
            for (WebApplication application : wabExtender.getApplications()) {
                String path = application.getPath();
                if (path == null) {
                    failures.add(WabDeployment.failed(application,
                            "invalid Web-ContextPath " + application.getContextPathHeader()));
                } else if (wanted.containsKey(path)) {
                    WebApplication other = (WebApplication) wanted.get(path);
                    failures.add(WabDeployment.failed(application,
                            "the context path is already used by bundle " + other.getBundle().getBundleId()));
                } else {
                    wanted.put(path, application);
                }
            }
            for (WhiteboardContextModel model : whiteboard.model(new HashSet<>(wanted.keySet()), failures)) {
                wanted.put(model.getPath(), model);
            }

            for (Iterator<Deployment> it = deployments.values().iterator(); it.hasNext(); ) {
                Deployment deployment = it.next();
                if (!accepts(deployment, wanted.get(deployment.getPath()))) {
                    undeploy(deployment);
                    it.remove();
                }
            }
            for (Map.Entry<String, Object> entry : wanted.entrySet()) {
                Deployment deployment = deployments.get(entry.getKey());
                if (entry.getValue() instanceof WebApplication) {
                    if (deployment == null) {
                        WabDeployment wab = new WabDeployment((WebApplication) entry.getValue(), server.getHost(),
                                config);
                        wab.deploy();
                        deployments.put(entry.getKey(), wab);
                    }
                } else {
                    WhiteboardContextModel model = (WhiteboardContextModel) entry.getValue();
                    if (deployment == null) {
                        WhiteboardDeployment context = new WhiteboardDeployment(model, whiteboard, server.getHost(),
                                config, bundleContext);
                        context.deploy(model);
                        deployments.put(entry.getKey(), context);
                    } else {
                        ((WhiteboardDeployment) deployment).update(model);
                    }
                }
            }
            describe(failures);
        } catch (RuntimeException e) {
            LOGGER.error("Error while updating the web container", e);
        }
    }

    private static boolean accepts(Deployment deployment, Object wanted) {
        if (deployment instanceof WabDeployment) {
            return ((WabDeployment) deployment).getApplication() == wanted;
        }
        return wanted instanceof WhiteboardContextModel
                && ((WhiteboardDeployment) deployment).accepts((WhiteboardContextModel) wanted);
    }

    private void undeploy(Deployment deployment) {
        try {
            deployment.undeploy();
        } catch (RuntimeException e) {
            LOGGER.warn("Error while undeploying {}", deployment.getPath(), e);
        }
    }

    private void describe(List<WebElement> failures) {
        List<WebElement> newContexts = new ArrayList<>();
        List<WebElement> newServlets = new ArrayList<>();
        List<WebElement> newFilters = new ArrayList<>();
        List<WebElement> allFailures = new ArrayList<>(failures);
        for (Deployment deployment : deployments.values()) {
            deployment.describe(newContexts, newServlets, newFilters);
            if (deployment instanceof WhiteboardDeployment) {
                allFailures.addAll(((WhiteboardDeployment) deployment).getFailures());
            }
        }
        for (WebElement failure : failures) {
            switch (failure.getType()) {
                case WHITEBOARD:
                case WAB:
                    newContexts.add(failure);
                    break;
                case FILTER:
                    newFilters.add(failure);
                    break;
                case SERVLET:
                case RESOURCE:
                    newServlets.add(failure);
                    break;
                default:
                    // the listeners are only logged
            }
        }
        newContexts.sort(ORDER);
        newServlets.sort(ORDER);
        newFilters.sort(ORDER);
        contexts = Collections.unmodifiableList(newContexts);
        servlets = Collections.unmodifiableList(newServlets);
        filters = Collections.unmodifiableList(newFilters);

        Set<String> current = new HashSet<>();
        for (WebElement failure : allFailures) {
            String key = failure.toString() + "#" + failure.getServiceId() + "#" + failure.getBundleId();
            current.add(key);
            if (!loggedFailures.contains(key)) {
                StringBuilder element = new StringBuilder(failure.getType().toString());
                if (failure.getName() != null) {
                    element.append(' ').append(failure.getName());
                }
                element.append(" (bundle ").append(failure.getBundleId());
                if (failure.getServiceId() >= 0) {
                    element.append(", service ").append(failure.getServiceId());
                }
                LOGGER.warn("{}) is not deployed: {}", element, failure.getMessage());
            }
        }
        loggedFailures = current;
    }

    @Override
    public List<String> getEndpoints() {
        TomcatServer current = server;
        return current == null ? Collections.emptyList() : current.getEndpoints();
    }

    @Override
    public List<WebElement> getContexts() {
        return contexts;
    }

    @Override
    public List<WebElement> getServlets() {
        return servlets;
    }

    @Override
    public List<WebElement> getFilters() {
        return filters;
    }

}
