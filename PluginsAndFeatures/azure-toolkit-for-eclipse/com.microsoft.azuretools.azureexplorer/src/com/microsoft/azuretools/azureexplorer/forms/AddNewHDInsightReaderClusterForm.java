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
 
package com.microsoft.azuretools.azureexplorer.forms;

import java.util.function.Predicate;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class AddNewHDInsightReaderClusterForm extends AddNewClusterForm {
	private ClusterDetail selectedClusterDetail;
	private String defaultStorageRootPath;
	
	public AddNewHDInsightReaderClusterForm(Shell parentShell, @NotNull HDInsightRootModule hdinsightRootModule, @NotNull ClusterDetail selectedClusterDetail) {
		super(parentShell, hdinsightRootModule);
		this.selectedClusterDetail = selectedClusterDetail;
		this.defaultStorageRootPath = selectedClusterDetail.getDefaultStorageRootPath();
	}
	
	@Override
	protected void customizeUI() {
		this.clusterNameField.setText(selectedClusterDetail.getName());
		this.clusterNameField.setEditable(false);
	}
	
	private Predicate<IClusterDetail> getSelectedLinkedHdiClusterPredicate(@NotNull String selectedClusterName) {
		return clusterDetail -> clusterDetail instanceof HDInsightAdditionalClusterDetail
				&& clusterDetail.getName().equals(selectedClusterName);
	}
	
	@Override
	protected void afterOkActionPerformed() {
		HDInsightAdditionalClusterDetail linkedCluster = 
				(HDInsightAdditionalClusterDetail) ClusterManagerEx.getInstance().findClusterDetail(
						getSelectedLinkedHdiClusterPredicate(selectedClusterDetail.getName()), true);
		if (linkedCluster != null) {
			linkedCluster.setDefaultStorageRootPath(defaultStorageRootPath);
			ClusterManagerEx.getInstance().updateHdiAdditionalClusterDetail(linkedCluster);
		}
		
		super.afterOkActionPerformed();
	}


}
