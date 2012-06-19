/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.demo;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.karaf.diagnostic.core.DumpDestination;
import org.apache.karaf.diagnostic.core.DumpProvider;

/**
 * This demo provider creates images in dump destination which
 * contains screenshots from all attached displays.
 */
public class ScreenshotDumpProvider implements DumpProvider {

	/**
	 * {@inheritDoc}
	 */
    public void createDump(DumpDestination destination) throws Exception {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // get all graphic devices attached to computer
        GraphicsDevice[] gs = ge.getScreenDevices();

        // create dump entry for each device  
        for (int i=0; i < gs.length; i++) {
            DisplayMode mode = gs[i].getDisplayMode();
            Rectangle bounds = new Rectangle(0, 0, mode.getWidth(), mode.getHeight());
            BufferedImage screenshot = new Robot(gs[i]).createScreenCapture(bounds);

            // to attach your entry to destination you have to execute this line
            OutputStream outputStream = destination.add("screenshot/display_" + i + ".png");
            ImageIO.write(screenshot, "PNG", outputStream);

            outputStream.close();
        }

    }

}
