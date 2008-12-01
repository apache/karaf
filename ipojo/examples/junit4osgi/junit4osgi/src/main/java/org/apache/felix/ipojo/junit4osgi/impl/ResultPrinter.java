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
package org.apache.felix.ipojo.junit4osgi.impl;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Enumeration;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.runner.BaseTestRunner;

/**
 * Result Printer.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResultPrinter implements TestListener {
    /**
     * the writer.
     */
    PrintStream m_fWriter;

    /**
     * The column .
     */
    int m_fColumn = 0;

    /**
     * Creates a ResultPrinter.
     * @param writer the printer
     */
    public ResultPrinter(PrintStream writer) {
        m_fWriter = writer;
    }

    /**
     * Prints the result.
     * @param result the test result
     * @param runTime the test duration
     */
    synchronized void print(TestResult result, long runTime) {
        printHeader(runTime);
        printErrors(result);
        printFailures(result);
        printFooter(result);
    }

    /**
     * Prints message wiating for prompt.
     */
    void printWaitPrompt() {
        getWriter().println();
        getWriter().println("<RETURN> to continue");
    }

    /*
     * Internal methods
     */

    /**
     * Prints the result header.
     * @param runTime the test execution duration
     */
    protected void printHeader(long runTime) {
        getWriter().println();
        getWriter().println("Time: " + elapsedTimeAsString(runTime));
    }

    /**
     * Prints the errors.
     * @param result the test result
     */
    protected void printErrors(TestResult result) {
        printDefects(result.errors(), result.errorCount(), "error");
    }

    /**
     * Prints failures.
     * @param result the test result
     */
    protected void printFailures(TestResult result) {
        printDefects(result.failures(), result.failureCount(), "failure");
    }

    /**
     * Prints failures.
     * @param booBoos the failures
     * @param count the number of failures
     * @param type the type
     */
    protected void printDefects(Enumeration/*<TestFailure>*/ booBoos, int count, String type) {
        if (count == 0) {
            return;
        }
        
        if (count == 1) {
            getWriter().println("There was " + count + " " + type + ":");
        } else {
            getWriter().println("There were " + count + " " + type + "s:");
        }
        
        for (int i = 1; booBoos.hasMoreElements(); i++) {
            printDefect((TestFailure) booBoos.nextElement(), i);
        }
    }

    /**
     * Prints a failure.
     * @param booBoo the failure
     * @param count the count
     */
    public void printDefect(TestFailure booBoo, int count) { // only public for testing purposes
        printDefectHeader(booBoo, count);
        printDefectTrace(booBoo);
    }

    /**
     * Prints defect header.
     * @param booBoo the failure
     * @param count the count
     */
    protected void printDefectHeader(TestFailure booBoo, int count) {
        // I feel like making this a println, then adding a line giving the throwable a chance to print something
        // before we get to the stack trace.
        getWriter().print(count + ") " + booBoo.failedTest());
    }

    /**
     * Prints the stack trace.
     * @param booBoo the failure
     */
    protected void printDefectTrace(TestFailure booBoo) {
        getWriter().print(BaseTestRunner.getFilteredTrace(booBoo.trace()));
    }

    /**
     * Prints the footer.
     * @param result the test result.
     */
    protected void printFooter(TestResult result) {
        if (result.wasSuccessful()) {
            getWriter().println();
            getWriter().print("OK");
            getWriter().println(" (" + result.runCount() + " test" + (result.runCount() == 1 ? "" : "s") + ")");

        } else {
            getWriter().println();
            getWriter().println("FAILURES!!!");
            getWriter().println("Tests run: " + result.runCount() + ",  Failures: " + result.failureCount() + ",  Errors: " + result.errorCount());
        }
        getWriter().println();
    }

    /**
     * Returns the formatted string of the elapsed time.
     * @param runTime the elapsed time
     * @return the elapsed time.
     */
    protected String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format((double) runTime / 1000);
    }

    public PrintStream getWriter() {
        return m_fWriter;
    }

    /**
     * Adds an error.
     * @param test the test in error.
     * @param t the thrown error
     * @see junit.framework.TestListener#addError(Test, Throwable)
     */
    public void addError(Test test, Throwable t) {
        getWriter().print("E");
    }

    /**
     * Adds a failure.
     * @param test the failing test.
     * @param t the thrown failure
     * @see junit.framework.TestListener#addFailure(Test, AssertionFailedError)
     */
    public void addFailure(Test test, AssertionFailedError t) {
        getWriter().print("F");
    }

    /**
     * A test ends.
     * (do nothing)
     * @param test the ending test
     * @see junit.framework.TestListener#endTest(Test)
     */
    public void endTest(Test test) { }

    /**
     * A test starts.
     * @param test the starting test
     * @see junit.framework.TestListener#startTest(Test)
     */
    public void startTest(Test test) {
        getWriter().print(".");
        if (m_fColumn++ >= 40) {
            getWriter().println();
            m_fColumn = 0;
        }
    }

}
