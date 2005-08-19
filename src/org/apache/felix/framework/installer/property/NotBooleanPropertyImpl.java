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
package org.apache.felix.framework.installer.property;

import javax.swing.JComponent;

import org.apache.felix.framework.installer.BooleanProperty;
import org.apache.felix.framework.installer.editor.BooleanEditor;

public class NotBooleanPropertyImpl implements BooleanProperty
{
    private BooleanProperty m_prop = null;
    private JComponent m_editor = null;

    public NotBooleanPropertyImpl(BooleanProperty prop)
    {
        m_prop = prop;
    }

    public String getName()
    {
        return "NOT " + m_prop.getName();
    }

    public boolean getBooleanValue()
    {
        return !m_prop.getBooleanValue();
    }

    public void setBooleanValue(boolean b)
    {
        m_prop.setBooleanValue(!b);
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
        return (getBooleanValue()) ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
    }
}