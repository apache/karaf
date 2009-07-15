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

package org.apache.felix.sigil.ui.eclipse.ui.form;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SigilFormEntry {
	private static final IFormValueConverter NULL_DESCRIPTOR = new IFormValueConverter() {
		public String getLabel(Object value) {
			return (String) value;
		}

		public Object getValue(String label) {
			return label;
		}
	};
	
	private Label lbl;
	private Text txt;
	private Button btn;
	private IFormValueConverter descriptor;
	private boolean freeText = true;
	
	private Object value;
	private ISigilFormEntryListener listener;
	
	public SigilFormEntry(Composite parent, FormToolkit toolkit, String title) {
		this(parent, toolkit, title, null, null);
	}
	
	public SigilFormEntry(Composite parent, FormToolkit toolkit, String title, String browse, IFormValueConverter descriptor) {
		this.descriptor = descriptor == null ? NULL_DESCRIPTOR : descriptor;
		createComponent(parent, title, browse, toolkit);
	}
	
	public void setFormEntryListener(ISigilFormEntryListener listener) {
		this.listener = listener;
	}
	
	public void setValue(Object value) {
		this.value = value;
		String text = descriptor.getLabel(value);
		if ( text == null ) {
			text = "";
		}
		txt.setText(text);
		handleValueChanged();
	}
	
	public Object getValue() {
		return value;
	}

	public void setFreeText(boolean freeText) {
		this.freeText = freeText;
	}
	
	public void setEditable(boolean editable) {
		lbl.setEnabled(editable);
		txt.setEditable(editable);
		if ( btn != null ) {
			btn.setEnabled(editable);
		}
	}	
		
	private void createComponent(Composite parent, String title, String browse, FormToolkit toolkit) {
		lbl = toolkit.createLabel(parent, title);
		lbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		
		txt = toolkit.createText(parent, "", SWT.SINGLE | SWT.BORDER);
		txt.addKeyListener( new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if ( freeText ) {
					switch ( e.character ) {
					case '\r': handleValueChanged(); 
					}
				}
				else {
					switch ( e.character ) {
					case '\b': 
						setValue(null); 
						handleValueChanged(); 
					default:
						e.doit = false;
						break;
					}
				}
			}
		});
		txt.addFocusListener( new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				handleValueChanged();
			}
		});
		
		if ( browse != null ) {
			btn = toolkit.createButton(parent, browse, SWT.PUSH);
			btn.addSelectionListener( new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					handleBrowseSelected();
				}
			});
		}
		
		fillIntoGrid(parent);
	}

	private void handleBrowseSelected() {
		if ( listener != null ) {
			listener.browseButtonSelected(this);
		}
	}

	private void handleValueChanged() {
		if ( freeText ) {
			this.value = descriptor.getValue(txt.getText());
		}
		if ( listener != null ) {
			listener.textValueChanged(this);
		}
	}

	private void fillIntoGrid(Composite parent) {
		if ( parent.getLayout() instanceof GridLayout ) {
			GridLayout layout = (GridLayout) parent.getLayout();
			
			int cols = layout.numColumns;
			
			lbl.setLayoutData( new GridData(SWT.LEFT, SWT.CENTER, false, false) );
			
			if ( btn == null ) {
				txt.setLayoutData( new GridData(SWT.FILL, SWT.CENTER, true, false, Math.max(1, cols - 1), 1 ) );
			}
			else {
				txt.setLayoutData( new GridData(SWT.FILL, SWT.CENTER, true, false, Math.max(1, cols - 2), 1 ) );
			}
		}
	}
}
