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

import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import junit.framework.Test;

/**
 * Test Suite list model.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TestListModel extends AbstractListModel {

    /**
     * Id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * List of {@link TestRecord}. 
     */
    private List<TestRecord> m_list = new ArrayList<TestRecord>();

    /**
     * Gets the element placed at the given index.
     * @param index the index
     * @return the element placed at the given index 
     * or <code>null</code> if no element at this index
     * @see javax.swing.ListModel#getElementAt(int)
     */
    public Object getElementAt(int index) {
        if (index >= m_list.size()) {
            return null;
        } else {
            return m_list.get(index).m_name;
        }
    }

    /**
     * Gets the test object placed at the given index.
     * @param index the index
     * @return the test object placed at the given index
     */
    public Test getTestElementAt(int index) {
        return m_list.get(index).m_test;
    }

    /**
     * Adds a test.
     * @param test the test to add
     */
    public void addTest(Test test) {
        synchronized (this) {
            TestRecord tr = new TestRecord();
            tr.m_test = test;
            tr.m_name = test.toString();
            m_list.add(tr);
        }
        fireContentsChanged(this, m_list.size() - 1, m_list.size() - 1);
    }

    /**
     * Removes a test.
     * @param test the test to remove
     */
    public void removeTest(Test test) {
        int index = 1;
        synchronized (this) {
            for (TestRecord t : m_list) {
                if (t.m_test.equals(test)) {
                    index = m_list.indexOf(t);
                    m_list.remove(t);
                    return;
                }
            }
        }
        
        if (index != -1) {
            fireContentsChanged(this, index, index);
        }
    }

    /**
     * Clears the list.
     */
    public void clear() {
        m_list.clear();
    }

    private class TestRecord {
        /**
         * The test.
         */
        public Test m_test;

        /**
         * The test name.
         */
        public String m_name;
    }

    /**
     * Gets the list size.
     * @return the list size.
     * @see javax.swing.ListModel#getSize()
     */
    public int getSize() {
        return m_list.size();
    }

}
