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
package org.apache.felix.scrplugin.om;

import org.apache.felix.scrplugin.tags.JavaTag;

/**
 * The <code>AbstractObject</code>
 * is the base class for the all classes of the scr om.
 */
public abstract class AbstractObject {

    protected final JavaTag tag;

    protected AbstractObject(JavaTag tag) {
        this.tag = tag;
    }

    protected String getMessage(String message) {
        if ( this.tag == null ) {
            return message;
        } else {
            return "@" + this.tag.getName() + ": " + message + " (" + this.tag.getSourceLocation() + ")";
        }
    }
}
