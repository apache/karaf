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
package org.apache.felix.mosgi.managedelements.memoryprobe.tab;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSliderUI;

public class LogarithmicJSlider extends JSlider {

   public LogarithmicJSlider(int min, int max, int value) {
     super(min, max, value);
     SliderUI ui = new LogSliderUI(this);
     this.setUI(ui);
   }

    public static class LogSliderUI extends BasicSliderUI {

        public LogSliderUI(JSlider b) {
            super(b);
        }


        public int xPositionForValue(int value) {
            int min = slider.getMinimum();
            int max = slider.getMaximum();
            int trackLength = trackRect.width;
            double valueRange = (double) Math.log(max) - (double) Math.log(min);
            double pixelsPerValue = (double) trackLength / valueRange;
            int trackLeft = trackRect.x;
            int trackRight = trackRect.x + (trackRect.width - 1);
            int xPosition;

            if (!drawInverted()) {
                xPosition = trackLeft;
                xPosition += Math.round(pixelsPerValue * ((double) Math.log(value) - Math.log(min)));
            } else {
                xPosition = trackRight;
                xPosition -= Math.round(pixelsPerValue * ((double) Math.log(value) - Math.log(min)));
            }

            xPosition = Math.max(trackLeft, xPosition);
            xPosition = Math.min(trackRight, xPosition);

            return xPosition;


        }

        public int yPositionForValue(int value) {
            // TODO GH: Implement to support vertical log sliders
            return super.yPositionForValue(value);
        }

        public int valueForYPosition(int yPos) {
            // TODO GH: Implement to support vertical log sliders
            return super.valueForYPosition(yPos);
        }

        public int valueForXPosition(int xPos) {
            int value;
            final int minValue = slider.getMinimum();
            final int maxValue = slider.getMaximum();
            final int trackLength = trackRect.width;
            final int trackLeft = trackRect.x;
            final int trackRight = trackRect.x + (trackRect.width - 1);

            if (xPos <= trackLeft) {
                value = drawInverted() ? maxValue : minValue;
            } else if (xPos >= trackRight) {
                value = drawInverted() ? minValue : maxValue;
            } else {
                int distanceFromTrackLeft = xPos - trackLeft;
                double valueRange = (double) Math.log(maxValue) - (double) Math.log(minValue);
                //double valuePerPixel = (double)valueRange / (double)trackLength;
                //int valueFromTrackLeft =
                //    (int)Math.round( Math.pow(3.5,(double)distanceFromTrackLeft * (double)valuePerPixel));

                int valueFromTrackLeft =
                    (int)
                    Math.round(Math.pow(Math.E, Math.log(minValue) + ((((double) distanceFromTrackLeft) * valueRange) / (double) trackLength)));

                value = drawInverted() ? maxValue - valueFromTrackLeft :
                    (int) Math.log(minValue) + valueFromTrackLeft;
            }

            return value;

        }

        public void paintTicks(Graphics g) {
            Rectangle tickBounds = tickRect;
            int i;
            int maj, min, max;
            int w = tickBounds.width;
            int h = tickBounds.height;
            int centerEffect, tickHeight;

            g.setColor(Color.black);

            maj = slider.getMajorTickSpacing();
            min = slider.getMinorTickSpacing();

            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                g.translate(0, tickBounds.y);

                int value = slider.getMinimum();
                int xPos = 0;

                if (slider.getMinorTickSpacing() > 0) {
                    int majorValue = slider.getMinimum();

                    while (value <= slider.getMaximum()) {
                        if (value >= majorValue) {
                            value = majorValue;
                            majorValue *= maj;
                        }
                        value += (majorValue / 10.0);

                        xPos = xPositionForValue(value);
                        paintMinorTickForHorizSlider(g, tickBounds, xPos);

                    }
                }

                if (slider.getMajorTickSpacing() > 0) {
                    value = slider.getMinimum();

                    while (value <= slider.getMaximum()) {
                        xPos = xPositionForValue(value);
                        paintMajorTickForHorizSlider(g, tickBounds, xPos);
                        value *= slider.getMajorTickSpacing();
                    }
                }

                g.translate(0, -tickBounds.y);
            } else {

                g.translate(tickBounds.x, 0);

                int value = slider.getMinimum();
                int yPos = 0;

                if (slider.getMinorTickSpacing() > 0) {
                    int majorValue = slider.getMinimum();
                    int offset = 0;
                    if (!slider.getComponentOrientation().isLeftToRight()) {
                        offset = tickBounds.width - tickBounds.width / 2;
                        g.translate(offset, 0);
                    }

                    while (value <= slider.getMaximum()) {
                        if (value >= majorValue) {
                            value = majorValue;
                            majorValue *= maj;
                        }

                        yPos = yPositionForValue(value);
                        paintMinorTickForVertSlider(g, tickBounds, yPos);
                        value += (majorValue / 10.0);
                    }

                    if (!slider.getComponentOrientation().isLeftToRight()) {
                        g.translate(-offset, 0);
                    }
                }

                if (slider.getMajorTickSpacing() > 0) {
                    value = slider.getMinimum();
                    if (!slider.getComponentOrientation().isLeftToRight()) {
                        g.translate(2, 0);
                    }

                    while (value <= slider.getMaximum()) {
                        yPos = yPositionForValue(value);
                        paintMajorTickForVertSlider(g, tickBounds, yPos);
                        value *= slider.getMajorTickSpacing();
                    }

                    if (!slider.getComponentOrientation().isLeftToRight()) {
                        g.translate(-2, 0);
                    }
                }
                g.translate(-tickBounds.x, 0);
            }
        }

    }

    public Hashtable createStandardLabels(int increment, int start) {
        if (start > getMaximum() || start < getMinimum()) {
            throw new IllegalArgumentException("Slider label start point out of range.");
        }

        if (increment <= 0) {
            throw new IllegalArgumentException("Label incremement must be > 0");
        }

        class LabelHashtable extends Hashtable implements PropertyChangeListener {
            int increment = 0;
            int start = 0;
            boolean startAtMin = false;


            public LabelHashtable(int increment, int start) {
                super();
                this.increment = increment;
                this.start = start;
                startAtMin = start == getMinimum();
                createLabels(this, increment, start);
            }

            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("minimum") && startAtMin) {
                    start = getMinimum();
                }

                if (e.getPropertyName().equals("minimum") ||
                    e.getPropertyName().equals("maximum")) {

                    Enumeration keys = getLabelTable().keys();
                    Object key = null;
                    Hashtable hashtable = new Hashtable();

                    // Save the labels that were added by the developer
                    while (keys.hasMoreElements()) {
                        key = keys.nextElement();
                        Object value = getLabelTable().get(key);
                        if (!(value instanceof LabelUIResource)) {
                            hashtable.put(key, value);
                        }
                    }

                    clear();
                    createLabels(this, increment, start);

                    // Add the saved labels
                    keys = hashtable.keys();
                    while (keys.hasMoreElements()) {
                        key = keys.nextElement();
                        put(key, hashtable.get(key));
                    }
                    ((JSlider) e.getSource()).setLabelTable(this);
                }
            }
        }

        LabelHashtable table = new LabelHashtable(increment, start);

        if (getLabelTable() != null && (getLabelTable() instanceof PropertyChangeListener)) {
            removePropertyChangeListener((PropertyChangeListener) getLabelTable());
        }

        addPropertyChangeListener(table);

        return table;
    }

    /**
     * This method creates the table of labels that are used to label major ticks
     * on the slider.
     * @param table
     * @param increment
     * @param start
     */
    protected void createLabels(Hashtable table, int increment, int start) {
        for (int labelIndex = start; labelIndex <= getMaximum(); labelIndex *= increment) {

            table.put(new Integer(labelIndex), new LabelUIResource("" + labelIndex, JLabel.CENTER));
        }
    }

    protected static class LabelUIResource extends JLabel implements UIResource {
        public LabelUIResource(String text, int alignment) {
            super(text, alignment);
            setName("Slider.label");
        }
    }

}
