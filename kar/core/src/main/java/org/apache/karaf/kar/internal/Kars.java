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

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.net.URI;
import java.util.List;

public class Kars extends StandardMBean implements KarsMBean {
    
    private KarService karService;
    
    public Kars() throws NotCompliantMBeanException {
        super(KarsMBean.class);
    }
    
    public List<String> getKars() throws Exception {
        return karService.list();
    }
    
    public void install(String url) throws Exception {
        karService.install(new URI(url));
    }
    
    public void uninstall(String name) throws Exception {
        karService.uninstall(name);
    }

    public KarService getKarService() {
        return karService;
    }

    public void setKarService(KarService karService) {
        this.karService = karService;
    }
    
}
