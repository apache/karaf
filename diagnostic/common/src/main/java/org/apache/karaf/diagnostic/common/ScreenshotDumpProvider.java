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
package org.apache.karaf.diagnostic.common;

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
 * Create screenshot from all devices.
 * 
 * @author ldywicki
 */
public class ScreenshotDumpProvider implements DumpProvider {

	public void createDump(DumpDestination destination) throws Exception {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		 
		for (int i=0; i < gs.length; i++) {
			DisplayMode mode = gs[i].getDisplayMode();
			Rectangle bounds = new Rectangle(0, 0, mode.getWidth(), mode.getHeight());
		    BufferedImage screenshot = new Robot(gs[i]).createScreenCapture(bounds);
		    OutputStream outputStream = destination.add("screenshot/display_" + i + ".jpg");
		    ImageIO.write(screenshot, "PNG", outputStream);
		}

	}

}
