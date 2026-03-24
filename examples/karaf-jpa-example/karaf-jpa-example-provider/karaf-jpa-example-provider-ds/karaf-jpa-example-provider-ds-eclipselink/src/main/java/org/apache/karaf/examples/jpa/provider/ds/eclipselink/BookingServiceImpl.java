/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.jpa.provider.ds.eclipselink;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.apache.karaf.examples.jpa.Booking;
import org.apache.karaf.examples.jpa.BookingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

/**
 * Implementation of the booking service using direct JPA EntityManagerFactory.
 */
@Component(service = BookingService.class, immediate = true)
public class BookingServiceImpl implements BookingService {

    @Reference(target = "(osgi.unit.name=booking-eclipselink)")
    private EntityManagerFactory emf;

    @Override
    public void add(Booking booking) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(booking);
            em.flush();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public void add(String flight, String customer) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setFlight(flight);
        add(booking);
    }

    @Override
    public List<Booking> list() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT b FROM Booking b", Booking.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public Booking get(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Booking.class, id);
        } finally {
            em.close();
        }
    }

    @Override
    public void remove(Long id) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Booking booking = em.find(Booking.class, id);
            if (booking != null) {
                em.remove(booking);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
