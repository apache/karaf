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

package org.apache.felix.sigil.ui.eclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FileUtils {
	public static void loadFile( Shell shell, Text text, String msg, boolean isDirectory ) {
		if ( isDirectory ) {
			DirectoryDialog dialog = new DirectoryDialog(shell, SWT.NONE);
			dialog.setMessage(msg);
			String value = dialog.open();
			if ( value != null ) {
				text.setText( value );
			}
		}
		else {
			FileDialog dialog = new FileDialog(shell, SWT.NONE);
			dialog.setText(msg);
			String value = dialog.open();
			if ( value != null ) {
				text.setText( value );
			}
		}
	}

}
