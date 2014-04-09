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
package org.apache.karaf.kar.internal;

import org.apache.karaf.kar.KarService;
import org.apache.karaf.kar.KarsMBean;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.net.URI;
import java.util.List;

public class KarsMBeanImpl extends StandardMBean implements KarsMBean {

    private KarService karService;

    public KarsMBeanImpl() throws NotCompliantMBeanException {
        super(KarsMBean.class);
    }

    public List<String> getKars() throws MBeanException {
        try {
            return karService.list();
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public void create(String repoName, List<String> features) {
        karService.create(repoName, features, null);
    }

    public void install(String url) throws MBeanException {
        try {
            karService.install(new URI(url));
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public void uninstall(String name) throws MBeanException {
        try {
            karService.uninstall(name);
        } catch (Exception e) {
            throw new MBeanException(null, e.getMessage());
        }
    }

    public KarService getKarService() {
        return karService;
    }

    public void setKarService(KarService karService) {
        this.karService = karService;
    }

}
