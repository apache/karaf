/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.osgi.framework.installer.property;

import javax.swing.JComponent;

import org.apache.osgi.framework.installer.BooleanProperty;
import org.apache.osgi.framework.installer.editor.BooleanEditor;

public class BooleanPropertyImpl implements BooleanProperty
{
    private String m_name = null;
    private boolean m_value = false;
    private JComponent m_editor = null;

    public BooleanPropertyImpl(String name, boolean value)
    {
        m_name = name;
        m_value = value;
    }

    public String getName()
    {
        return m_name;
    }

    public boolean getBooleanValue()
    {
        return m_value;
    }

    public void setBooleanValue(boolean b)
    {
        m_value = b;
    }

    public JComponent getEditor()
    {
        if (m_editor == null)
        {
            m_editor = new BooleanEditor(this);
        }
        return m_editor;
    }

    public void setEditor(JComponent comp)
    {
        m_editor = comp;
    }

    public String toString()
    {
        return (m_value) ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
    }
}