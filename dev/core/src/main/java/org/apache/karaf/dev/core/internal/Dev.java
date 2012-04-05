/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.dev.core.internal;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.dev.core.DevMBean;
import org.apache.karaf.dev.core.DevService;
import org.apache.karaf.dev.core.FrameworkType;

/**
 * Implementation of the DevMBean.
 */
public class Dev extends StandardMBean implements DevMBean {

    private final DevService devService;

    public Dev(DevService devService) throws NotCompliantMBeanException {
        super(DevMBean.class);
        this.devService = devService;
    }

    @Override
    public String getFramework() {
        return this.devService.getFramework().toString();
    }
    
    @Override
    public void setFramework(String framework) {
        this.devService.setFramework(FrameworkType.valueOf(framework.toLowerCase()));
    }

    @Override
    public void setFrameworkDebug(boolean debug) {
        this.devService.setFrameworkDebug(debug);
    }

    @Override
    public void restart(boolean clean) {
        this.devService.restart(clean);
    }

 

}
