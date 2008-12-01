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
package org.apache.felix.ipojo.junit4osgi.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

/**
 * Result Table Model.
 * Store the results of executed tests.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResultTableModel extends AbstractTableModel {

    /**
     * Success String.
     */
    public static final String SUCCESS = "success";

    /**
     * Failure String.
     */
    public static final String FAILURE = "failure";

    /**
     * Error String.
     */
    public static final String ERROR = "error";

    /**
     * UUDI.
     */
    private static final long serialVersionUID = 1L;

    /**
     * List of results.
     */
    private List<TestRecord> m_results = new ArrayList<TestRecord>();

    public int getRowCount() {
        return m_results.size();
    }

    public int getColumnCount() {
        return 2;
    }

    /**
     * Adds a failing test.
     * @param t the test
     * @param e the assertion error
     */
    public void addTest(Test t, AssertionFailedError e) {
        TestRecord rec = new TestRecord(t, e);
        m_results.add(rec);
        fireTableDataChanged();
    }

    /**
     * Adds a test in error.
     * @param t the test
     * @param e the thrown error
     */
    public void addTest(Test t, Throwable e) {
        TestRecord rec = new TestRecord(t, e);
        m_results.add(rec);
        fireTableDataChanged();
    }

    /**
     * Adds a sucessfull test.
     * @param t the test
     */
    public void addTest(Test t) {
        if (!contains(t)) {
            TestRecord rec = new TestRecord(t);
            m_results.add(rec);
            fireTableDataChanged();
        }
    }

    public int getTestCount() {
        return m_results.size();
    }

    /**
     * Gets the number of success.
     * @return the number of success
     */
    public int getSucess() {
        int count = 0;
        for (TestRecord test : m_results) {
            if (test.m_wasSucessFull) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the number of errors.
     * @return the number of errors
     */
    public int getErrors() {
        int count = 0;
        for (TestRecord test : m_results) {
            if (test.m_error != null) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the number of failures.
     * @return the number of failures
     */
    public int getFailures() {
        int count = 0;
        for (TestRecord test : m_results) {
            if (test.m_failure != null) {
                count++;
            }
        }
        return count;
    }

    
    /**
     * Does the result list contains the given test.
     * @param t the test 
     * @return <code>true</code> if the list contains the test.
     */
    private boolean contains(Test t) {
        for (TestRecord test : m_results) {
            if (test.m_test.equals(t)) { return true; }
        }
        return false;
    }

    /**
     * Clear the list.
     */
    public void clear() {
        m_results.clear();
        fireTableDataChanged();
    }

    /**
     * Get the Object placed in the JTable.
     * @param rowIndex the row
     * @param columnIndex the column
     * @return the object
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) { return m_results.get(rowIndex).m_test; }
        if (columnIndex == 1) {
            TestRecord tr = m_results.get(rowIndex);
            if (tr.m_wasSucessFull) { return SUCCESS; }
            if (tr.m_failure != null) { return FAILURE; }
            if (tr.m_error != null) { return ERROR; }
        }
        return null;
    }

    /**
     * Gets column names.
     * @param column the column
     * @return the column name
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    public String getColumnName(int column) {
        if (column == 0) { return "Test"; }

        if (column == 1) { return "Status"; }

        return null;
    }

    /**
     * Gets the message.
     * @param row the row
     * @param column the column
     * @return the message for this cell
     */
    public String getMessage(int row, int column) {
        if (row == -1) { return null; }
        TestRecord rec = m_results.get(row);
        if (rec.m_wasSucessFull) { return "The test " + rec.m_test + " was executed sucessfully."; }
        if (rec.m_failure != null) { return "The test " + rec.m_test + " has failed : \n" + rec.m_failure.getMessage(); }
        if (rec.m_error != null) {
            String message = "The test " + rec.m_test + " has thrown an error : \n" + rec.m_error.getMessage();
            StringWriter sw = new StringWriter();
            rec.m_error.printStackTrace(new PrintWriter(sw));
            message += "\n" + sw.toString();
            return message;
        }
        return "";
    }

    private class TestRecord {
        /**
         * Was the test successful?
         */
        private boolean m_wasSucessFull;

        /**
         * The test.
         */
        private Test m_test;

        /**
         * The failure.
         */
        private AssertionFailedError m_failure;

        /**
         * The error.
         */
        private Throwable m_error;

        /**
         * Creates a TestRecord.
         * @param t the test
         * @param e the failure
         */
        public TestRecord(Test t, AssertionFailedError e) {
            m_test = t;
            m_wasSucessFull = false;
            m_failure = e;
        }

        /**
         * Creates a TestRecord.
         * @param t the test
         * @param e the error
         */
        public TestRecord(Test t, Throwable e) {
            m_test = t;
            m_wasSucessFull = false;
            m_error = e;
        }

        /**
         * Creates a TestRecord.
         * @param t the test
         */
        public TestRecord(Test t) {
            m_test = t;
            m_wasSucessFull = true;
        }
    }

}
