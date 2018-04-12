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
package org.apache.karaf.jpa.hibernate.impl;

import java.lang.reflect.Proxy;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.karaf.jpa.hibernate.StatisticsMXBean;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Track EntityManagerFactory services for the persistence units. 
 * Manage on StatisticsMXBean for each persistence unit named like:
 * org.hibernate.statistics:unitName=&lt;name of persistence unit&gt;
 */
public class StatisticsPublisher implements ServiceTrackerCustomizer<EntityManagerFactory, EntityManagerFactory> {
    private static Logger LOG = LoggerFactory.getLogger(StatisticsPublisher.class);
    private BundleContext context;
    private MBeanServer mbeanServer;
    private ServiceTracker<EntityManagerFactory, EntityManagerFactory> emfTracker;

    public StatisticsPublisher(BundleContext context, MBeanServer mbeanServer) {
        this.context = context;
        this.mbeanServer = mbeanServer;
        this.emfTracker = new ServiceTracker<>(context, EntityManagerFactory.class, this);
    }
    
    public void start() {
        this.emfTracker.open(true);
    }
    
    public void stop() {
        ServiceReference<EntityManagerFactory>[] emfRefs = this.emfTracker.getServiceReferences();
        for (ServiceReference<EntityManagerFactory> emfRef : emfRefs) {
            try {
                this.mbeanServer.unregisterMBean(getOName(emfRef));
            } catch (Exception e) {
            }
        }
        this.emfTracker.close();
    }
    
    ObjectName getOName(ServiceReference<EntityManagerFactory> reference) {
        try {
            String unitName = (String)reference.getProperty("osgi.unit.name");
            return new ObjectName("org.hibernate.statistics", "unitName", unitName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    private void publishStatistics(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory emf) {
        String persitenceProvider = (String)reference.getProperty("osgi.unit.provider");
        if (!"org.hibernate.ejb.HibernatePersistence".equals(persitenceProvider)) {
            return;
        }
        if (reference.getProperty("org.apache.aries.jpa.proxy.factory") != null) {
            return;
        }
        try {
            EntityManager em = emf.createEntityManager();
            SessionFactory sessionFactory = em.unwrap(Session.class).getSessionFactory();
            final Statistics statistics = sessionFactory.getStatistics();
            statistics.setStatisticsEnabled(true);
            mbeanServer.registerMBean(getStatisticsMBean(statistics), getOName(reference));
        } catch (Exception e) {
            LOG.warn("Error publishing StatisticsMXBean" + e.getMessage(), e);
        }
    }

    @Override
    public EntityManagerFactory addingService(ServiceReference<EntityManagerFactory> reference) {
        EntityManagerFactory emf = context.getService(reference);
        publishStatistics(reference, emf);
        return emf;
    }

    private Object getStatisticsMBean(final Statistics statistics) {
        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { StatisticsMXBean.class },
                (proxy, method, args) -> method.invoke(statistics, args));
    }

    @Override
    public void modifiedService(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory service) {
    }

    @Override
    public void removedService(ServiceReference<EntityManagerFactory> reference, EntityManagerFactory service) {
        try {
            mbeanServer.unregisterMBean(getOName(reference));
        } catch (Exception e) {
            // Ignore
        }
    }
}
