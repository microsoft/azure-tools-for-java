package com.microsoft.azuretools.azureexplorer.actions;

import java.util.function.Predicate;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azureexplorer.forms.AddNewClusterForm;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

public class AddNewHDInsightReaderClusterAction extends NodeActionListener {
	private HDInsightRootModule hdinsightRootModule;
	private ClusterDetail selectedClusterDetail;
	
	public AddNewHDInsightReaderClusterAction(@NotNull HDInsightRootModule hdinsightRootModule, @NotNull ClusterDetail selectedClusterDetail) {
		this.hdinsightRootModule = hdinsightRootModule;
		this.selectedClusterDetail = selectedClusterDetail;
	}

	@Override
	protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
		String defaultStorageRootPath = selectedClusterDetail.getDefaultStorageRootPath();
		
		AddNewClusterForm form = new AddNewClusterForm(PluginUtil.getParentShell(), hdinsightRootModule) {
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
		};
		form.open();
	}

}
