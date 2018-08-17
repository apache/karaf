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
package org.apache.karaf.examples.jpa.provider.blueprint.hibernate;

import org.apache.karaf.examples.jpa.Booking;
import org.apache.karaf.examples.jpa.BookingService;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Implementation of the booking service using the JPA entity manager service (provided by Karaf).
 */
@Transactional
public class BookingServiceImpl implements BookingService {

    @PersistenceContext(unitName = "booking-hibernate")
    private EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void add(Booking booking) {
        entityManager.persist(booking);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void add(String flight, String customer) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setFlight(flight);
        entityManager.persist(booking);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public List<Booking> list() {
        TypedQuery<Booking> query = entityManager.createQuery("SELECT b FROM Booking b", Booking.class);
        return query.getResultList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public Booking get(Long id) {
        TypedQuery<Booking> query = entityManager.createQuery("SELECT b FROM Booking b WHERE b.id=:id", Booking.class);
        query.setParameter("id", id);
        Booking booking = null;
        try {
            booking = query.getSingleResult();
        } catch (NoResultException e) {
            // nothing to do
        }
        return booking;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void remove(Long id) {
        Booking booking = get(id);
        entityManager.remove(booking);
    }
}
