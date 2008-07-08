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

package org.apache.felix.upnp.sample.tv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class TvPane extends JPanel  // MouseListener
{
	private final static int IMAGE_BORDER_SIZE = 20;
	private final static int IMAGE_BOTTOM_BORDER_SIZE = 30;
	
	////////////////////////////////////////////////
	//	Constructor
	////////////////////////////////////////////////

	public TvPane()
	{
		loadImage();
		initPanel();
	}

	////////////////////////////////////////////////
	//	TvDevice
	////////////////////////////////////////////////

	private TvDevice tvDev = null;
	
	public void setDevice(TvDevice dev)
	{
		tvDev = dev;
	}

	public TvDevice getDevice()
	{
		return tvDev;
	}
	
	////////////////////////////////////////////////
	//	Background
	////////////////////////////////////////////////

	private BufferedImage panelmage;
	private BufferedImage tvOnImage;
	
	private void loadImage()
	{
		try {
			panelmage = ImageIO.read(TvPane.class.getResourceAsStream("images/tv.jpg"));
			tvOnImage = ImageIO.read(TvPane.class.getResourceAsStream("images/tvon.jpg"));
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	////////////////////////////////////////////////
	//	Background
	////////////////////////////////////////////////

	private void initPanel()
	{
		setPreferredSize(new Dimension(panelmage.getWidth(), panelmage.getHeight()));
	}

	////////////////////////////////////////////////
	//	Font
	////////////////////////////////////////////////

	private final static String DEFAULT_FONT_NAME = "Lucida Console";
	private final static int DEFAULT_TIME_FONT_SIZE = 10;

	private Font timeFont = null;

	private Font getFont(Graphics g, int size)
	{
		Font font = new Font(DEFAULT_FONT_NAME, Font.BOLD, size);
		if (font != null)
			return font;
		return g.getFont();
	}
		
	private Font getFont(Graphics g)
	{
		if (timeFont == null)
			timeFont = getFont(g, DEFAULT_TIME_FONT_SIZE);
		return timeFont;
	}

	////////////////////////////////////////////////
	//	paint
	////////////////////////////////////////////////

	private void drawClockInfo(Graphics g)
	{
		TvDevice tvDev = getDevice();

		int winWidth = getWidth();
		int winHeight = getHeight();
		
		Font font = getFont(g);
		g.setFont(font);
		FontMetrics fontMetric = g.getFontMetrics();
		g.setColor(Color.RED);

		Rectangle2D strBounds;
		int strWidth;
		int strHeight;
		int strX;
		int strY;
		
		//// Time String ////
		
		String timeStr = tvDev.getClockTime();
		if (timeStr != null && 0 < timeStr.length()) {
			strBounds = fontMetric.getStringBounds(timeStr, g);
			strWidth = (int)strBounds.getWidth();		
			strHeight = (int)strBounds.getHeight();
			strX = IMAGE_BORDER_SIZE;
			strY = IMAGE_BORDER_SIZE + strHeight;
			g.drawString(
				timeStr,
				strX,
				strY);
		}
		
		//// Tempture String ////
		
		String tempStr = tvDev.getAirconTempture();
		if (tempStr != null && 0 < tempStr.length()) {
			tempStr += "C";
			strBounds = fontMetric.getStringBounds(tempStr, g);
			strWidth = (int)strBounds.getWidth();		
			strHeight = (int)strBounds.getHeight();
			strX = winWidth - IMAGE_BORDER_SIZE - strWidth;
			strY = IMAGE_BORDER_SIZE + strHeight;
			g.drawString(
				tempStr,
				strX,
				strY);
		}

		//// Message String ////
		
		String msgStr = tvDev.getMessage();
		if (msgStr != null && 0 < msgStr.length()) {
			strBounds = fontMetric.getStringBounds(msgStr, g);
			strWidth = (int)strBounds.getWidth();		
			strHeight = (int)strBounds.getHeight();
			strX = IMAGE_BORDER_SIZE;
			strY = getHeight()-IMAGE_BOTTOM_BORDER_SIZE-2;
			g.drawString(
				msgStr,
				strX,
				strY);
		}
	}

	private void drawTvImage(Graphics g)
	{
		//g.setColor(Color.WHITE);
		//g.clearRect(IMAGE_BORDER_SIZE, IMAGE_BORDER_SIZE, getWidth()-IMAGE_BORDER_SIZE*2, getHeight()-IMAGE_BORDER_SIZE-IMAGE_BOTTOM_BORDER_SIZE);
		g.drawImage(tvOnImage, IMAGE_BORDER_SIZE, IMAGE_BORDER_SIZE, null);
	}
	
	private void clear(Graphics g)
	{
		g.setColor(Color.GRAY);
		g.clearRect(0, 0, getWidth(), getHeight());
	}
	

	private void drawPanelImage(Graphics g)
	{
		g.drawImage(panelmage, 0, 0, null);
	}
		
	public void paint(Graphics g)
	{
		clear(g);
		drawPanelImage(g);
		TvDevice tvDev = getDevice();
		if (tvDev.isOn() == true) {
			drawTvImage(g);
			drawClockInfo(g);
		}
	}
}

