/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.karaf.scr.examples.service.provider;

import org.apache.karaf.scr.examples.service.ExampleService;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class ExampleServiceProvider implements ExampleService {

    public static final String COMPONENT_NAME = "ExampleServiceProvider";

    public static final String COMPONENT_LABEL = "Example Service Consumer Component";

    private LogService logService;

    private String name = "To whom it may concern";

    private String salutation = "Hello";

    public void printGreetings() {
        logService.log(LogService.LOG_INFO, salutation + " " + name);
    }

	/**
	 * @param salutation the salutation to set
	 */
	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

    @Reference
    protected void setLogService(LogService logService) {
        this.logService = logService;
    }

    protected void unsetLogService(LogService logService) {
        this.logService = logService;
    }

}
