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
import java.util.concurrent.locks.ReentrantLock;

import org.apache.karaf.services.web.WebElement;
import org.apache.karaf.services.web.WebService;
import org.apache.karaf.services.web.internal.wab.WabDeployment;
import org.apache.karaf.services.web.internal.wab.WabExtender;
import org.apache.karaf.services.web.internal.wab.WebApplication;
import org.apache.karaf.services.web.internal.whiteboard.Whiteboard;
import org.apache.karaf.services.web.internal.whiteboard.WhiteboardContextModel;
import org.apache.karaf.services.web.internal.whiteboard.WhiteboardDeployment;
import org.apache.karaf.services.web.internal.whiteboard.WhiteboardService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The web container: the embedded Tomcat server, the whiteboard and the web application bundles.
 * <p>
 * The contexts are deployed by a single thread, which computes the contexts to deploy each time a whiteboard
 * service or a web application bundle comes, changes or goes, and applies the difference to Tomcat. The changes are
 * applied after a short delay, so that the services registered together, typically by a bundle starting, are
 * deployed together.
 * <p>
 * A whiteboard service or a web application bundle going away is undeployed by the thread removing it, so that it
 * happens while the service is still registered and the bundle still active, without waiting for the deployment of
 * other contexts.
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
    // nothing is submitted to the executor once it is stopping, the executor being shut down
    private final Object stopLock = new Object();
    private volatile boolean stopping;

    private TomcatServer server;
    private Whiteboard whiteboard;
    private WabExtender wabExtender;

    // changed by the executor thread only, holding its lock, and read by the threads removing a service or a bundle
    private final Map<String, Deployment> deployments = new LinkedHashMap<>();
    // used by the executor thread only
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

    public void start() throws Exception {
        server = new TomcatServer(config, bundleContext.getDataFile("tomcat"));
        server.start();
        LOGGER.info("Web container started on {}", String.join(", ", server.getEndpoints()));
        // both are created before a tracked service or bundle schedules a deployment, which uses them
        whiteboard = new Whiteboard(bundleContext, this);
        wabExtender = new WabExtender(bundleContext, this);
        whiteboard.open();
        wabExtender.open();
        changed();
    }

    public void stop() {
        Future<?> stopped;
        synchronized (stopLock) {
            stopping = true;
            // undeploy while the services are still tracked, so that they are released
            stopped = executor.submit(() -> {
                for (Deployment deployment : deployments()) {
                    undeploy(deployment);
                }
                synchronized (deployments) {
                    deployments.clear();
                }
                if (server != null) {
                    server.stop();
                }
            });
            executor.shutdown();
        }
        if (!await(stopped) && server != null) {
            // the contexts are still being undeployed: the ports are released for the next web container
            server.stopConnectors();
        }
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
    public void changed() {
        synchronized (stopLock) {
            if (!stopping && scheduled.compareAndSet(false, true)) {
                executor.schedule(() -> {
                    scheduled.set(false);
                    reconcile();
                }, DEPLOY_DELAY, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void removed(WhiteboardService service) {
        // the service is marked as removed: it is either seen used by a context, or not deployed in it anymore
        for (Deployment deployment : deployments()) {
            if (deployment instanceof WhiteboardDeployment && ((WhiteboardDeployment) deployment).uses(service)) {
                undeployNow(deployment, () -> ((WhiteboardDeployment) deployment).remove(service));
            }
        }
        changed();
    }

    @Override
    public void removed(WebApplication application) {
        for (Deployment deployment : deployments()) {
            if (deployment instanceof WabDeployment && ((WabDeployment) deployment).getApplication() == application) {
                undeployNow(deployment, deployment::undeploy);
            }
        }
        changed();
    }

    /**
     * Undeploys what a thread removing a service or stopping a bundle removes, rather than waiting for the executor
     * thread, which may meanwhile be deploying another context needing a lock this thread holds. The executor thread,
     * or a thread already changing the deployment, leaves it to the update following the removal.
     */
    private void undeployNow(Deployment deployment, Runnable undeploy) {
        ReentrantLock lock = deployment.getLock();
        if (Thread.currentThread() == executorThread || lock.isHeldByCurrentThread()) {
            return;
        }
        // once stopping, the executor thread undeploys all the contexts: a busy one is not waited for again
        boolean waiting = !stopping;
        try {
            if (!lock.tryLock(waiting ? config.undeployTimeout : 0, TimeUnit.MILLISECONDS)) {
                if (waiting) {
                    LOGGER.warn("The context {} is being deployed, it is not undeployed within {} ms, continuing",
                            deployment.getPath(), config.undeployTimeout);
                }
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            undeploy.run();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("Error while undeploying {}", deployment.getPath(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return {@code true} if the task is done, {@code false} if it is still running after the undeploy timeout.
     */
    private boolean await(Future<?> future) {
        try {
            future.get(config.undeployTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOGGER.warn("The web container did not undeploy within {} ms, continuing", config.undeployTimeout);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            LOGGER.error("Error in the web container", e.getCause());
        }
        return true;
    }

    private List<Deployment> deployments() {
        synchronized (deployments) {
            return new ArrayList<>(deployments.values());
        }
    }

    private void add(Deployment deployment) {
        synchronized (deployments) {
            deployments.put(deployment.getPath(), deployment);
        }
    }

    private static void locked(Deployment deployment, Runnable action) {
        ReentrantLock lock = deployment.getLock();
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
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
            List<WebApplication> applications = wabExtender.getApplications();
            // a deployed web application bundle keeps its path, the others wait for it by bundle id
            applications.sort(Comparator.comparing((WebApplication application) -> !isDeployed(application)));
            for (WebApplication application : applications) {
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

            // the removals of all the contexts first, so that a servlet or a filter which is not prototype scoped,
            // moving from a context to another, is destroyed before it is initialized again
            for (Deployment deployment : deployments()) {
                Object model = wanted.get(deployment.getPath());
                if (!accepts(deployment, model)) {
                    undeploy(deployment);
                    synchronized (deployments) {
                        deployments.remove(deployment.getPath());
                    }
                } else if (deployment instanceof WhiteboardDeployment) {
                    try {
                        locked(deployment, () -> ((WhiteboardDeployment) deployment)
                                .removeUnwanted((WhiteboardContextModel) model));
                    } catch (RuntimeException | LinkageError e) {
                        LOGGER.error("Error while updating {}", deployment.getPath(), e);
                    }
                }
            }
            for (Map.Entry<String, Object> entry : wanted.entrySet()) {
                // a context failing does not prevent the others from being deployed
                try {
                    deploy(entry.getKey(), entry.getValue());
                } catch (RuntimeException | LinkageError e) {
                    LOGGER.error("Error while deploying {}", entry.getKey(), e);
                }
            }
            describe(failures);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("Error while updating the web container", e);
        }
    }

    private void deploy(String path, Object wanted) {
        Deployment deployment = deployments.get(path);
        if (wanted instanceof WebApplication) {
            if (deployment == null) {
                WabDeployment wab = new WabDeployment((WebApplication) wanted, server.getHost(), config);
                // kept even if it fails, rather than deployed again by each update
                add(wab);
                locked(wab, wab::deploy);
            }
        } else {
            WhiteboardContextModel model = (WhiteboardContextModel) wanted;
            if (deployment == null) {
                WhiteboardDeployment context = new WhiteboardDeployment(model, server.getHost(), config,
                        bundleContext);
                // kept even if it fails, rather than deployed again by each update
                add(context);
                locked(context, () -> context.deploy(model));
            } else {
                locked(deployment, () -> ((WhiteboardDeployment) deployment).addWanted(model));
            }
        }
    }

    private boolean isDeployed(WebApplication application) {
        Deployment deployment = deployments.get(application.getPath());
        return deployment instanceof WabDeployment && ((WabDeployment) deployment).getApplication() == application;
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
            locked(deployment, deployment::undeploy);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("Error while undeploying {}", deployment.getPath(), e);
        }
    }

    private void describe(List<WebElement> failures) {
        List<WebElement> newContexts = new ArrayList<>();
        List<WebElement> newServlets = new ArrayList<>();
        List<WebElement> newFilters = new ArrayList<>();
        List<WebElement> allFailures = new ArrayList<>(failures);
        for (Deployment deployment : deployments()) {
            locked(deployment, () -> {
                deployment.describe(newContexts, newServlets, newFilters);
                if (deployment instanceof WhiteboardDeployment) {
                    allFailures.addAll(((WhiteboardDeployment) deployment).getFailures());
                }
            });
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
