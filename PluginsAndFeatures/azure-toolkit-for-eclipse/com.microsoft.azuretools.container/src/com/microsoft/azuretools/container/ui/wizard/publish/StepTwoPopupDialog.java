/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.container.ui.wizard.publish;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;
import com.microsoft.azuretools.container.presenters.StepTwoPopupDialogPresenter;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;

public class StepTwoPopupDialog extends AzureTitleAreaDialogWrapper implements MvpView {

	private Text textAppName;
	private Combo comboResourceGroup;
	private final StepTwoPopupDialogPresenter<StepTwoPopupDialog> presenter;
	private Combo comboSubscription;
	private Text textResourceGroupName;
	private Button btnResourceGroupCreateNew;
	private Button btnResourceGroupUseExisting;

	public void fillSubscriptions(List<SubscriptionDetail> sdl) {
		if (sdl == null || sdl.size() <= 0) {
			System.out.println("sdl is null");
			return;
		}

		comboSubscription.removeAll();
		;
		for (SubscriptionDetail sd : sdl) {
			comboSubscription.add(sd.getSubscriptionName());
		}
		if (comboSubscription.getItemCount() > 0) {
			comboSubscription.select(0);
		}
	}

	public void fillResourceGroups(List<ResourceGroup> rgl) {
		if (rgl == null || rgl.size() <= 0) {
			System.out.println("rgl is null");
			return;
		}
		comboResourceGroup.removeAll();
		for (ResourceGroup rg : rgl) {
			comboResourceGroup.add(rg.name());
		}
		if (comboResourceGroup.getItemCount() > 0) {
			comboResourceGroup.select(0);
		}
	}

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public StepTwoPopupDialog(Shell parentShell) {
		super(parentShell);
		presenter = new StepTwoPopupDialogPresenter<StepTwoPopupDialog>();
		presenter.onAttachView(this);
		this.setTitle("Create Web App on Linux");
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

		Group grpAppService = new Group(container, SWT.NONE);
		grpAppService.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		grpAppService.setLayout(new GridLayout(3, false));

		Label lblAppName = new Label(grpAppService, SWT.NONE);
		lblAppName.setText("Enter name");

		textAppName = new Text(grpAppService, SWT.BORDER);
		textAppName.addListener(SWT.FocusIn, event -> onTextAppNameFocus());
		textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textAppName.setMessage("<enter name>");

		Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
		lblazurewebsitescom.setText(".azurewebsites.net");

		Label lblSubscription = new Label(grpAppService, SWT.NONE);
		lblSubscription.setText("Subscription");

		comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
		comboSubscription.addListener(SWT.Selection, event -> onComboSubscriptionSelection());
		comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// == RG
		Composite compositeResourceGroup = new Composite(container, SWT.NONE);
		compositeResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		compositeResourceGroup.setLayout(new GridLayout(2, false));

		btnResourceGroupCreateNew = new Button(compositeResourceGroup, SWT.RADIO);
		btnResourceGroupCreateNew.addListener(SWT.Selection, event -> radioResourceGroupLogic());
		btnResourceGroupCreateNew.setSelection(true);
		btnResourceGroupCreateNew.setText("Create new");

		textResourceGroupName = new Text(compositeResourceGroup, SWT.BORDER);
		textResourceGroupName.addListener(SWT.FocusIn, event -> onTextResourceGroupNameFocus());
		textResourceGroupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textResourceGroupName.setBounds(0, 0, 64, 19);
		textResourceGroupName.setMessage("<enter name>");

		btnResourceGroupUseExisting = new Button(compositeResourceGroup, SWT.RADIO);
		btnResourceGroupUseExisting.addListener(SWT.Selection, event -> radioResourceGroupLogic());
		btnResourceGroupUseExisting.setText("Use existing");

		comboResourceGroup = new Combo(compositeResourceGroup, SWT.READ_ONLY);
		comboResourceGroup.setEnabled(false);
		comboResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboResourceGroup.setBounds(0, 0, 26, 22);

		DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
		String date = df.format(new Date());
		textAppName.setText("linuxwebapp-" + date);
		textResourceGroupName.setText("rg-webapp-" + date);

		onLoading();

		return area;
	}

	public void setWidgetsEnabledStatus(boolean enableStatus) {
		btnResourceGroupCreateNew.setEnabled(enableStatus);
		btnResourceGroupCreateNew.setEnabled(enableStatus);
		comboResourceGroup.setEnabled(enableStatus);

		textResourceGroupName.setEditable(enableStatus);
		textAppName.setEditable(enableStatus);
	}

	@Override
	protected void okPressed() {
		Button btnOK = getButton(IDialogConstants.OK_ID);
		btnOK.setEnabled(false);
		setWidgetsEnabledStatus(false);
		this.setMessage("Creating ... ");
		try {
			if (btnResourceGroupCreateNew.getSelection()) {
				presenter.onDeployNew(textAppName.getText(), comboSubscription.getSelectionIndex(),
						textResourceGroupName.getText(), true);
			} else if (btnResourceGroupUseExisting.getSelection()) {
				presenter.onDeployNew(textAppName.getText(), comboSubscription.getSelectionIndex(),
						comboResourceGroup.getText(), false);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void onComboSubscriptionSelection() {
		int index = comboSubscription.getSelectionIndex();
		presenter.onChangeSubscription(index);
	}

	private void onTextAppNameFocus() {
		// TODO
	}

	private void onTextResourceGroupNameFocus() {
		// TODO
	}

	private void onLoading() {
		presenter.onLoadSubsAndRGs();
	}

	private void radioResourceGroupLogic() {
		boolean enabled = btnResourceGroupCreateNew.getSelection();
		textResourceGroupName.setEnabled(enabled);
		comboResourceGroup.setEnabled(!enabled);
	}

	protected void finalize() throws Throwable {
		presenter.onDetachView();
		super.finalize();
	}

}
