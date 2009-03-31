/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scrplugin.om.metatype;

import java.util.ArrayList;
import java.util.List;

public class MetaData {

    protected String localization;

    protected List<OCD> ocds = new ArrayList<OCD>();

    protected List<Designate> designates = new ArrayList<Designate>();

    public String getLocalization() {
        return this.localization;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public List<OCD> getOCDs() {
        return this.ocds;
    }

    public List<Designate> getDesignates() {
        return this.designates;
    }

    public void addOCD(OCD ocd) {
        this.ocds.add(ocd);
    }

    public void addDesignate(Designate d) {
        this.designates.add(d);
    }
}
