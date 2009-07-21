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

package org.apache.felix.sigil.ui.eclipse.ui.editors.completion;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;


class CompletionProposal implements ICompletionProposal
{

    private int fReplacementOffset;
    private int fReplacementLength;
    private int fCursorPosition;
    private String fReplacementString;
    private String fAdditionalProposalInfo;
    private IContextInformation fContextInformation;
    private String fDisplayString;
    private Image fImage;


    /**
     * Creates a new completion proposal based on the provided information. The replacement string is
     * considered being the display string too. All remaining fields are set to <code>null</code>.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     */
    public CompletionProposal( String replacementString, int replacementOffset, int replacementLength,
        int cursorPosition )
    {
        this( replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null );
    }


    /**
     * Creates a new completion proposal. All fields are initialized based on the provided information.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     * @param image the image to display for this proposal
     * @param displayString the string to be displayed for the proposal
     * @param contextInformation the context information associated with this proposal
     * @param additionalProposalInfo the additional information associated with this proposal
     */
    public CompletionProposal( String replacementString, int replacementOffset, int replacementLength,
        int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
        String additionalProposalInfo )
    {
        fReplacementString = replacementString;
        fReplacementOffset = replacementOffset;
        fReplacementLength = replacementLength;
        fCursorPosition = cursorPosition;
        fImage = image;
        fDisplayString = displayString;
        fContextInformation = contextInformation;
        fAdditionalProposalInfo = additionalProposalInfo;
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
     */
    public void apply( IDocument document )
    {
        try
        {
            document.replace( fReplacementOffset, fReplacementLength, fReplacementString );
        }
        catch ( BadLocationException x )
        {
            // ignore
        }
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
     */
    public String getAdditionalProposalInfo()
    {
        return fAdditionalProposalInfo;
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
     */
    public IContextInformation getContextInformation()
    {
        return fContextInformation;
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
     */
    public String getDisplayString()
    {
        if ( fDisplayString != null )
            return fDisplayString;
        return fReplacementString;
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
     */
    public Image getImage()
    {
        return fImage;
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
     */
    public Point getSelection( IDocument document )
    {
        return new Point( fReplacementOffset + fCursorPosition, 0 );
    }

}