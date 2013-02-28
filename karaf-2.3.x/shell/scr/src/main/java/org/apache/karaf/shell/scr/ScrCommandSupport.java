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
package org.apache.karaf.shell.scr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.ScrService;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.scr.action.ScrActionSupport;
import org.apache.karaf.shell.scr.completer.ScrCompleterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ScrCommandSupport extends AbstractCommand implements CompletableFunction {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private ScrService scrService;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public abstract Class<? extends Action> getActionClass();

    public abstract List<Class<? extends Completer>> getCompleterClasses();

    @Override
    public Action createNewAction() {
        try {
            lock.readLock().lock();
            ScrActionSupport action = (ScrActionSupport)getActionClass().newInstance();
            action.setScrService(getScrService());
            return action;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Completer> getCompleters() {
        List<Completer> completers = null;

        if (getCompleterClasses() != null) {
            try {
                lock.readLock().lock();
                completers = new ArrayList<Completer>();
                for (Class<? extends Completer> completerClass : getCompleterClasses()) {
                    ScrCompleterSupport ccs = (ScrCompleterSupport)completerClass.newInstance();
                    ccs.setScrService(scrService);
                    completers.add(ccs);
                }

            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                lock.readLock().unlock();
            }
        }
        return completers;
    }

    public Map<String, Completer> getOptionalCompleters() {
        return null;
    }

    /**
     * Returns the instance of ScrService for this instance of
     * ScrCommandSupport.
     * 
     * @return the ScrCommandSupport or null
     */
    public ScrService getScrService() {
        return scrService;
    }

    public void setScrService(ScrService scrService) {
        try {
            lock.writeLock().lock();
            this.scrService = scrService;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unsetScrService(ScrService scrService) {
        try {
            lock.writeLock().lock();
            this.scrService = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
