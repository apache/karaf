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
package org.apache.felix.example.servicebased.host;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.apache.felix.example.servicebased.host.service.SimpleShape;

public class DrawingFrame extends JFrame
    implements MouseListener, MouseMotionListener
{
    private static final long serialVersionUID = 1L;
    private static final int BOX = 54;
    private JToolBar m_toolbar;
    private String m_selected;
    private JPanel m_panel;
    private Map m_shapes = new HashMap();
    private ShapeComponent m_selectedComponent;
    private SimpleShape m_defaultShape = new DefaultShape();
    private ActionListener m_reusableActionListener = new ShapeActionListener();

    public DrawingFrame()
    {
        super("Service-Based Host");

        m_toolbar = new JToolBar("Toolbar");
        m_panel = new JPanel();
        m_panel.setBackground(Color.WHITE);
        m_panel.setLayout(null);
        m_panel.setMinimumSize(new Dimension(400, 400));
        m_panel.addMouseListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(m_toolbar, BorderLayout.NORTH);
        getContentPane().add(m_panel, BorderLayout.CENTER);
        setSize(400, 400);
    }

    public void selectShape(String name)
    {
        m_selected = name;
    }

    public SimpleShape getShape(String name)
    {
        ShapeInfo info = (ShapeInfo) m_shapes.get(name);
        if (info == null)
        {
            return m_defaultShape;
        }
        else
        {
            return info.m_shape;
        }
    }

    public void addShape(String name, Icon icon, SimpleShape shape)
    {
        m_shapes.put(name, new ShapeInfo(name, icon, shape));
        JButton button = new JButton(icon);
        button.setActionCommand(name);
        button.addActionListener(m_reusableActionListener);

        if (m_selected == null)
        {
            button.doClick();
        }

        m_toolbar.add(button);
        m_toolbar.validate();
        repaint();
    }

    public void removeShape(String name)
    {
        m_shapes.remove(name);

        if ((m_selected != null) && m_selected.equals(name))
        {
            m_selected = null;
        }

        for (int i = 0; i < m_toolbar.getComponentCount(); i++)
        {
            JButton sb = (JButton) m_toolbar.getComponent(i);
            if (sb.getActionCommand().equals(name))
            {
                m_toolbar.remove(i);
                m_toolbar.invalidate();
                validate();
                repaint();
                break;
            }
        }

        if ((m_selected == null) && (m_toolbar.getComponentCount() > 0))
        {
            ((JButton) m_toolbar.getComponent(0)).doClick();
        }
    }

    public void mouseClicked(MouseEvent evt)
    {
        if (m_selected == null)
        {
            return;
        }

        if (m_panel.contains(evt.getX(), evt.getY()))
        {
            ShapeComponent sc = new ShapeComponent(this, m_selected);
            sc.setBounds(evt.getX() - BOX / 2, evt.getY() - BOX / 2, BOX, BOX);
            m_panel.add(sc, 0);
            m_panel.validate();
            m_panel.repaint(sc.getBounds());
        }
    }

    public void mouseEntered(MouseEvent evt)
    {
    }

    public void mouseExited(MouseEvent evt)
    {
    }

    public void mousePressed(MouseEvent evt)
    {
        Component c = m_panel.getComponentAt(evt.getPoint());
        if (c instanceof ShapeComponent)
        {
            m_selectedComponent = (ShapeComponent) c;
            m_panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            m_panel.addMouseMotionListener(this);
            m_selectedComponent.repaint();
        }
    }

    public void mouseReleased(MouseEvent evt)
    {
        if (m_selectedComponent != null)
        {
            m_panel.removeMouseMotionListener(this);
            m_panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            m_selectedComponent.setBounds(
                evt.getX() - BOX / 2, evt.getY() - BOX / 2, BOX, BOX);
            m_selectedComponent.repaint();
            m_selectedComponent = null;
        }
    }

    public void mouseDragged(MouseEvent evt)
    {
        m_selectedComponent.setBounds(
            evt.getX() - BOX / 2, evt.getY() - BOX / 2, BOX, BOX);
    }

    public void mouseMoved(MouseEvent evt)
    {
    }

    private class DefaultShape implements SimpleShape
    {
        private ImageIcon m_icon = null;

        public void draw(Graphics2D g2, Point p)
        {
            if (m_icon == null)
            {
                try
                {
                    m_icon = new ImageIcon(this.getClass().getResource("underc.png"));
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    g2.setColor(Color.red);
                    g2.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
                    return;
                }
            }
            g2.drawImage(m_icon.getImage(), 0, 0, null);
        }

        public String getName()
        {
            return "Default";
        }
    }

    private class ShapeActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            selectShape(evt.getActionCommand());
        }
    }

    private static class ShapeInfo
    {
        public String m_name;
        public Icon m_icon;
        public SimpleShape m_shape;
        public ShapeInfo(String name, Icon icon, SimpleShape shape)
        {
            m_name = name;
            m_icon = icon;
            m_shape = shape;
        }
    }
}