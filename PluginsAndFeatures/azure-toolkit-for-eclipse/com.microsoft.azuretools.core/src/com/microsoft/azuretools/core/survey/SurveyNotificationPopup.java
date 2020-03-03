/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.core.survey;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.microsoft.azuretools.core.Activator;

public class SurveyNotificationPopup extends AbstractNotificationPopup {
	private static final String SURVEY_WINDOW_TITLE = "Azure Toolkit Survey";
	private static final String SURVEY_TEXT_HEADER = "Enjoy Azure Toolkits?";
	private static final String SURVEY_TEXT_BODY = "Your feedback is important, take a minute to fill out our survey.";
	private static final String PUT_OFF_BUTTON_TEXT = "Not now";
	private static final String TAKE_SURVEY_BUTTON_TEXT = "Give feedback";
	private static final String NEVER_SHOW_AGAIN_BUTTON_TEXT = "Don't show again";

	private Runnable takeSurveyAction;
	private Runnable putOffAction;
	private Runnable neverShowAgainAction;
	private boolean isActionTaken = false;

	public SurveyNotificationPopup(Display display, Runnable takeSurveyAction, Runnable putOffAction, Runnable neverShowAgainAction) {
		super(display);
		this.takeSurveyAction = takeSurveyAction;
		this.putOffAction = putOffAction;
		this.neverShowAgainAction = neverShowAgainAction;
	}

	@Override
	protected void createContentArea(Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		container.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		container.setLayout(new GridLayout(1, false));

		final Label surveyLogo = new Label(container, SWT.NONE);

		final Image azureSmallLogo = Activator.getImageDescriptor("icons/azure_small.png").createImage();
		surveyLogo.setImage(azureSmallLogo);
		surveyLogo.setSize(50, 50);
		final GridData logoLayoutData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		logoLayoutData.widthHint = 50;
		surveyLogo.setLayoutData(logoLayoutData);

		final Label surveryHeader = new Label(container, SWT.NULL);
		surveryHeader.setText(SURVEY_TEXT_HEADER);
		surveryHeader.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

		new Label(container, SWT.NONE);

		final Label surveyBody = new Label(container, SWT.NONE | SWT.WRAP);
		surveyBody.setText(SURVEY_TEXT_BODY);

		if (surveyBody.getFont().getFontData().length >= 0) {
			FontData fontData = surveyBody.getFont().getFontData()[0];
			Font font = new Font(Display.getCurrent(),
					new FontData(fontData.getName(), fontData.getHeight(), SWT.BOLD));
			surveryHeader.setFont(font);
		}
		final GridData surveyBodyLayoutData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		surveyBodyLayoutData.widthHint = 200;
		surveyBody.setLayoutData(surveyBodyLayoutData);

		final Button takeSurveyButton = new Button(container, SWT.PUSH);
		takeSurveyButton.setText(TAKE_SURVEY_BUTTON_TEXT);
		takeSurveyButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		takeSurveyButton.getShell().setDefaultButton(takeSurveyButton);

		final Button putOffButton = new Button(container, SWT.PUSH);
		putOffButton.setText(PUT_OFF_BUTTON_TEXT);
		putOffButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

		final Button neverShowAgainButton = new Button(container, SWT.PUSH);
		neverShowAgainButton.setText(NEVER_SHOW_AGAIN_BUTTON_TEXT);
		neverShowAgainButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

		takeSurveyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isActionTaken = true;
				takeSurveyAction.run();
				close();
			}
		});

		putOffButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				close();
			}
		});


		neverShowAgainButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				isActionTaken = true;
				neverShowAgainAction.run();
				close();
			}
		});

	}

	@Override
	public boolean close() {
		// default to putOff if we don't get option from user
		if (!isActionTaken) {
			putOffAction.run();
		}
		return super.close();
	}

	@Override
	protected String getPopupShellTitle() {
		return SURVEY_WINDOW_TITLE;
	}
}

