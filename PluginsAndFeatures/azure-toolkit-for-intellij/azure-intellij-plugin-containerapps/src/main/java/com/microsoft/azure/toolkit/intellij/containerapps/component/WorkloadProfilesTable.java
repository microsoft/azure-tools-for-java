/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WorkloadProfilesTable extends JBTable implements AzureFormInputComponent<List<WorkloadProfile>> {
    public WorkloadProfilesTable() {
        super();
        setModel(new WorkloadProfilesTableModel());
        setRowSelectionAllowed(true);
        setCellSelectionEnabled(false);
        setAutoscrolls(true);
    }

    @Override
    public List<WorkloadProfile> getValue() {
        final WorkloadProfilesTableModel model = (WorkloadProfilesTableModel) getModel();
        final ArrayList<WorkloadProfile> result = new ArrayList<>(model.getData());
        if (result.contains(WorkloadProfile.CONSUMPTION_PROFILE)) {
            result.add(WorkloadProfile.CONSUMPTION_PROFILE);
        }
        return result;
    }

    @Override
    public void setValue(List<WorkloadProfile> val) {
        final WorkloadProfilesTableModel model = (WorkloadProfilesTableModel) getModel();
        // remove consumption as we will always show it
        final List<WorkloadProfile> profiles = Objects.isNull(val) ? Collections.emptyList() : val.stream()
                .filter(profile -> !StringUtils.equalsIgnoreCase(profile.getWorkloadProfileType(), WorkloadProfile.CONSUMPTION))
                .toList();
        model.setData(profiles);
        this.repaint();
    }

    public void addWorkloadProfile(@Nonnull WorkloadProfile profile) {
        final WorkloadProfilesTableModel model = (WorkloadProfilesTableModel) getModel();
        model.addWorkloadProfile(profile);
        this.repaint();
    }

    public void removeWorkloadProfile(@Nonnull WorkloadProfile profile) {
        this.removeWorkloadProfile(profile.getName());
    }

    public void removeWorkloadProfile(@Nonnull String profile) {
        final WorkloadProfilesTableModel model = (WorkloadProfilesTableModel) getModel();
        model.removeWorkloadProfile(profile);
        this.repaint();
    }

    public void removeSelectedProfile() {
        final int selectedRow = this.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        final String target = (String)getModel().getValueAt(selectedRow, 0);
        removeWorkloadProfile(target);
    }

    static class WorkloadProfilesTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Name", "Scaling", "WorkloadProfileSize"};
        public static final String CONSUMPTION_PROFILE_SIZE = "Up to 4 vCPUs / 8 Gi";

        @Getter
        private final List<WorkloadProfile> data = new ArrayList<>();

        @Nullable
        @Override
        public String getColumnName(int column) {
            return column < 0 || column >= COLUMNS.length ? super.getColumnName(column) : COLUMNS[column];
        }

        @Override
        public int getRowCount() {
            return data.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Nullable
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex > data.size() || columnIndex < 0 || columnIndex >= COLUMNS.length) {
                return null;
            }
            final WorkloadProfile workloadProfile = rowIndex == 0 ? WorkloadProfile.CONSUMPTION_PROFILE : data.get(rowIndex -1);
            return switch (columnIndex) {
                case 0 -> workloadProfile.getName();
                case 1 -> workloadProfile == WorkloadProfile.CONSUMPTION_PROFILE ? "-" : String.format("%s-%s", workloadProfile.getMinimumCount(), workloadProfile.getMaximumCount());
                case 2 -> workloadProfile == WorkloadProfile.CONSUMPTION_PROFILE ? "Up to 4 vCpus / 8 Gi" : String.format("vCPUs: %s, Memory: %s Gi", workloadProfile.getMinimumCount(), workloadProfile.getMaximumCount());
                default -> null;
            };
        }

        public void removeWorkloadProfile(@Nonnull final String profile) {
            data.stream()
                    .filter(p -> StringUtils.equalsIgnoreCase(p.getName(), profile))
                    .findFirst()
                    .ifPresent(data::remove);
        }

        public void addWorkloadProfile(@Nonnull final WorkloadProfile profile) {
            final WorkloadProfile workloadProfile = data.stream()
                    .filter(p -> StringUtils.equalsIgnoreCase(p.getName(), profile.getName()))
                    .findFirst().orElse(null);
            if (Objects.nonNull(workloadProfile)) {
                workloadProfile.setMaximumCount(profile.getMaximumCount());
                workloadProfile.setMinimumCount(profile.getMinimumCount());
                workloadProfile.setType(profile.getType());
            } else {
                data.add(profile);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void setData(List<WorkloadProfile> profiles) {
            data.clear();
            data.addAll(profiles);
        }
    }
}
