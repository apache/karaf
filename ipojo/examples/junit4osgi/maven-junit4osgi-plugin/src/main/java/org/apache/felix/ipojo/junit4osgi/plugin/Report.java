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
package org.apache.felix.ipojo.junit4osgi.plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import junit.framework.Test;

import org.codehaus.plexus.util.StringUtils;

/**
 * Test report. 
 * This class is provides the basics to support several output format.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Report {
    
    /**
     * Number of ran tests.
     */
    protected int completedCount;

    /**
     * Number of errors
     */
    protected int errorsCount;
    
    /**
     * Number of failures
     */
    protected int failuresCount;
    
    
    /**
     * Failing tests.
     */
    private List failureSources = new ArrayList();
    
    /**
     * Tests in error
     */
    private List errorSources = new ArrayList();
    
    /**
     * Time at the beginning of the test execution. 
     */
    protected long startTime;

    /**
     * Time at the end of the test execution. 
     */
    protected long endTime;

    /**
     * Double format. 
     */
    private NumberFormat numberFormat = NumberFormat.getInstance( Locale.US );

    /**
     * New line constant.
     */
    protected static final String NL = System.getProperty( "line.separator" );
   
    /**
     * Gets failing tests.
     * @return the list of failing tests.
     */
    public List getFailureSources() {
        return this.failureSources;
    }

    /**
     * Gets tests in error.
     * @return the list of test throwing unexpected exceptions
     */
    public List getErrorSources() {
        return this.errorSources;
    }
    
    /**
     * Callback called when a test starts.
     */
    public void testStarting() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Callback called when a test ends successfully.
     */
    public void testSucceeded() {
        endTest();
    }

    /**
     * Callback called when a test throws an unexpected error.
     * @param test the test in error.
     */
    public void testError(Test test) {
        ++errorsCount;
        errorSources.add(test.toString());
        endTest();
    }

    /**
     * Callback called when a test fails.
     * @param test the failing test.
     */
    public void testFailed(Test test) {
        ++failuresCount;
        failureSources.add(test.toString());
        endTest();
    }

    /**
     * Callback called when a test ends.
     * This method handles common action when a test ends.
     */
    private void endTest() {
        ++completedCount;

        endTime = System.currentTimeMillis();

        if (startTime == 0) {
            startTime = endTime;
        }
    }
    
    
    public int getNumErrors() {
        return errorsCount;
    }

    public int getNumFailures() {
        return failuresCount;
    }

    public int getNumTests() {
        return completedCount;
    }

    /**
     * Reset the report.
     */
    public void reset() {
        errorsCount = 0;

        failuresCount = 0;

        completedCount = 0;

        this.failureSources = new ArrayList();

        this.errorSources = new ArrayList();

    }


    /**
     * Returns the formatted String to display the given double.
     * @param runTime the elapsed time
     * @return the String displaying the elapsed time
     */
    protected String elapsedTimeAsString(long runTime) {
        return numberFormat.format((double) runTime / 1000);
    }

    /**
     * Returns the stacktrace as String.
     * @param report ReportEntry object.
     * @return stacktrace as string.
     */
    protected String getStackTrace(Test test, Throwable e) {

        if (e == null) {
            return "";
        }

        StringWriter w = new StringWriter();
        if (e != null) {
            e.printStackTrace(new PrintWriter(w));
            w.flush();
        }
        String text = w.toString();
        String marker = "at " + test.toString();

        String[] lines = StringUtils.split(text, "\n");
        int lastLine = lines.length - 1;
        int causedByLine = -1;
        // skip first
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith(marker)) {
                lastLine = i;
            } else if (line.startsWith("Caused by")) {
                causedByLine = i;
                break;
            }
        }

        StringBuffer trace = new StringBuffer();
        for (int i = 0; i <= lastLine; i++) {
            trace.append(lines[i]);
            trace.append("\n");
        }

        if (causedByLine != -1) {
            for (int i = causedByLine; i < lines.length; i++) {
                trace.append(lines[i]);
                trace.append("\n");
            }
        }
        return trace.toString();
    }
    
    

}
