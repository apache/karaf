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
package org.apache.karaf.wrapper.management.internal;

import org.apache.karaf.wrapper.WrapperService;
import org.apache.karaf.wrapper.management.WrapperMBean;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.io.File;

/**
 * Implementation of the wrapper MBean.
 */
public class WrapperMBeanImpl extends StandardMBean implements WrapperMBean {

    private WrapperService wrapperService;

    public WrapperMBeanImpl() throws NotCompliantMBeanException {
        super(WrapperMBean.class);
    }

    public void setWrapperService(WrapperService wrapperService) {
        this.wrapperService = wrapperService;
    }

    public WrapperService getWrapperService() {
        return this.wrapperService;
    }

    public void install() throws MBeanException {
        try {
            wrapperService.install();
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    public File[] install(String name, String displayName, String description, String startType) throws MBeanException {
        try {
            return wrapperService.install(name, displayName, description, startType);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }
    
    public File[] install(String name, String displayName, String description, String startType, String[] envs, String[] includes) throws MBeanException {
        try {
            return wrapperService.install(name, displayName, description, startType, envs, includes);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

}
