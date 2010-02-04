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
package org.apache.felix.sigil.gogo.junit;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

public class PrintListener implements TestListener {

    public PrintListener() {
    }

    public void startTest(Test test) {
        System.out.println( "Start " + test );
        System.out.flush();
    }

    public void endTest(Test test) {
        System.out.println( "End " + test );
        System.out.flush();
    }


    public void addError(Test test, Throwable t) {
        System.out.println( "Error " + test );
        t.printStackTrace(System.out);
        System.out.flush();
    }

    public void addFailure(Test test, AssertionFailedError error) {
        System.out.println( "Failure " + test + ": " + error.getMessage() );
        System.out.flush();
    }
}
