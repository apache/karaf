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

    private static final long serialVersionUID = 1L;

    private List<TestRecord> list = new ArrayList<TestRecord>();

    public Object getElementAt(int index) {
        if (index >= list.size()) {
            return null;
        } else {
            return list.get(index).name;
        }
    }

    public Test getTestElementAt(int index) {
        return list.get(index).test;
    }

    public void addTest(Test test) {
        synchronized(this) {
            TestRecord tr = new TestRecord();
            tr.test = test;
            tr.name = test.toString();
            list.add(tr);
        }
        fireContentsChanged(this, list.size() - 1, list.size() - 1);
    }

    public void removeTest(Test test) {
        int index = 1;
        synchronized(this) {
            for (TestRecord t : list) {
                if (t.test.equals(test)) {
                    index = list.indexOf(t);
                    list.remove(t);
                    return;
                }
            }
        }
        
        if (index != -1) {
            fireContentsChanged(this, index, index);
        }
    }

    public void clear() {
        list.clear();
    }

    private class TestRecord {
        public Test test;

        public String name;
    }

    public int getSize() {
        return list.size();
    }

}
