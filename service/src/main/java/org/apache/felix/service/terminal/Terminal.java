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
package org.apache.felix.service.terminal;

import java.io.*;

/**
 * Terminal.
 * 
 * The Terminal interface describes a minimal terminal that can easily be mapped
 * to command line editing tools.
 * 
 * A Terminal is associated with an Input Stream and an Output Stream. The Input
 * Stream represents the keyboard and the Output Stream the screen.
 * 
 * A terminal does not block the input, each character is returned as it is
 * typed, no buffering or line editing takes place, characters are also not
 * echoed. However, the Input Stream is not restricted to bytes only, it can
 * also return translated key strokes. Integers from 1000 are used for those.
 * Not all keys have to be supported by an implementation.
 * 
 * A number of functions is provided to move the cursor and erase
 * characters/lines/screens. Any text outputed to the Output Stream is
 * immediately added to the cursor position, which is then moved forwards. The
 * control characters (LF,CR,TAB,BS) perform their normal actions. However lines
 * do not wrap. Text typed that is longer than the window will not be visible,
 * it is the responsibility of the sender to ensure this does not happen.
 * 
 * A screen is considered to be {@link #height()} lines that each have
 * {@link #width()} characters. For cursor positioning, the screen is assumed to
 * be starting at 0,0 and increases its position from left to right and from top
 * to bottom. Positioning outside the screen bounds is undefined.
 */
public interface Terminal {
	/**
	 * Cursor up key
	 */
	int CURSOR_UP = 1000;
	/**
	 * Cursor down key.
	 */
	int CURSOR_DOWN = 1001;
	/**
	 * Cursors forward key. Usually right.
	 */
	int CURSOR_FORWARD = 1002;

	/**
	 * Cursors backward key. Usually left.
	 */
	int CURSOR_BACKWARD = 1003;

	/**
	 * Page up key
	 */
	int PAGE_UP = 1004;
	/**
	 * Page down key
	 */
	int PAGE_DOWN = 1005;
	/**
	 * Home key
	 */
	int HOME = 1006;
	/**
	 * End key
	 */
	int END = 1007;
	/**
	 * Insert key
	 */
	int INSERT = 1008;
	/**
	 * Delete key
	 */
	int DELETE = 1009;
	/**
	 * Break key
	 */
	int BREAK = 1009;
	/**
	 * Function key 1
	 */
	int F1 = 1101;
	/**
	 * Function key 2
	 */
	int F2 = 1102;
	/**
	 * Function key 3
	 */
	int F3 = 1103;
	/**
	 * Function key 4
	 */
	int F4 = 1104;
	/**
	 * Function key 5
	 */
	int F5 = 1105;
	/**
	 * Function key 6
	 */
	int F6 = 1106;
	/**
	 * Function key 7
	 */
	int F7 = 1107;
	/**
	 * Function key 8
	 */
	int F8 = 1108;
	/**
	 * Function key 9
	 */
	int F9 = 1109;
	/**
	 * Function key 10
	 */
	int F10 = 1110;
	/**
	 * Function key 11
	 */
	int F11 = 1111;
	/**
	 * Function key 12
	 */
	int F12 = 1112;

	enum Attribute {
		BLINK, UNDERLINE, STRIKE_THROUGH, BOLD, DIM, REVERSED;
	}

	enum Color {
		NONE, BLACK, GREEN, YELLOW, MAGENTA, CYAN, BLUE, RED, WHITE;
	}

	/**
	 * Return the associated Input Stream that represents the keyboard. Note
	 * that this InputStream can return values > 256, these characters are
	 * defined in this interface as special keys. This Input Stream should not
	 * be closed by the client. If the client is done, it should unget the
	 * services.
	 * 
	 * @return the current Input Stream.
	 */
	InputStream getInputStream();

	/**
	 * Return the Output Stream that is associated with the screen. Any writes
	 * Clear the complete screen and position the cursor at 0,0.
	 * 
	 * @throws Exception
	 */
	void clear() throws Exception;

	/**
	 * Leave the cursor where it is but clear the remainder of the line.
	 */
	void eraseEndOfLine();

	/**
	 * Move the cursor up one line, this must not cause a scroll if the cursor
	 * moves off the screen.
	 * 
	 * @throws Exception
	 */
	void up() throws Exception;

	/**
	 * Move the cursor down one line, this must not cause a scroll if the
	 * cursors moves off the screen.
	 * 
	 * @throws Exception
	 */
	void down() throws Exception;

	/**
	 * Move the cursor backward. Must not wrap to previous line.
	 * 
	 * @throws Exception
	 */
	void backward() throws Exception;

	/**
	 * Move the cursor forward. Must not wrap to next line if the cursor becomes
	 * higher than the width.
	 * 
	 * @throws Exception
	 */
	void forward() throws Exception;

	/**
	 * Return the actual width of the screen. Some screens can change their size
	 * and this method must return the actual width.
	 * 
	 * @return the width of the screen.
	 * 
	 * @throws Exception
	 */
	int width() throws Exception;

	/**
	 * Return the actual height of the screen. Some screens can change their
	 * size and this method must return the actual height.
	 * 
	 * @return the height of the screen.
	 * 
	 * @throws Exception
	 */
	int height() throws Exception;

	/**
	 * Return the current cursor position.
	 * 
	 * The position is returned as an array of 2 elements. The first element is
	 * the x position and the second elements is the y position. Both are zero
	 * based.
	 * 
	 * @return the current position or null if not possible.
	 * 
	 * @throws Exception
	 */
	int[] getPosition() throws Exception;

	/**
	 * Position the cursor on the screen. Positioning starts at 0,0 and the
	 * maxium value is given by {@link #width()}, {@link #height()}. The visible
	 * cursor is moved to this position and text insertion will continue from
	 * that position.
	 * 
	 * @param x
	 *            The x position, must be from 0-width
	 * @param y
	 *            The y position, must be from 0-height
	 * @throws IllegalArgumenException
	 *             when x or y is not in range
	 * @throws Exception
	 */
	boolean position(int x, int y) throws Exception;

	/**
	 * Set the attributes of the text to outputed. This method
	 * must reset all current attributes. That is, attributes
	 * are not inherited from the current position.
	 * 
	 * @param foreground The foreground color
	 * @param background The background color (around the character)
	 * @param attr A number of attributes.
	 */
	boolean attributes(Color foreground, Color background, Attribute... attr);
}
