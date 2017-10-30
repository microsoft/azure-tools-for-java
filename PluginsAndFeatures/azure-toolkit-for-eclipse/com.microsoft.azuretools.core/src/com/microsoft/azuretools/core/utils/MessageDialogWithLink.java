package com.microsoft.azuretools.core.utils;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class MessageDialogWithLink extends MessageDialog {

	public MessageDialogWithLink(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, String messageWithLink,
			int dialogImageType, int defaultIndex, String... okLabel) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, okLabel, defaultIndex);
		
		this.messageWithLink = messageWithLink;
	}

	public static boolean open(int kind, Shell parent, String title, String message, String messageWithLink, int style) {
		MessageDialogWithLink dialog = new MessageDialogWithLink(parent, title, null, message, messageWithLink, kind, 0, IDialogConstants.OK_LABEL);
		style &= SWT.SHEET;
		dialog.setShellStyle(dialog.getShellStyle() | style);
		return dialog.open() == 0;
	}
	
	public static void openError(Shell parent, String title, String message, String messageWithLink) {
		open(MessageDialog.ERROR, parent, title, message, messageWithLink, SWT.NONE);
	}
	
	public static void openInformation(Shell parent, String title, String message, String messageWithLink) {
		open(MessageDialog.INFORMATION, parent, title, message, messageWithLink, SWT.NONE);
	}
		
	protected Control createCustomArea(Composite parent) {
		Link link = new Link(parent, SWT.WRAP);
		link.setText(messageWithLink);
		
		return link;
	}
	
	private String messageWithLink;
}
