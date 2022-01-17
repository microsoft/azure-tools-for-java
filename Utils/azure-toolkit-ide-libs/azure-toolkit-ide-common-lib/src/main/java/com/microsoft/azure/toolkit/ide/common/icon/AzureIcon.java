/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.icon;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class AzureIcon {
    private String iconPath;
    private List<Modifier> modifierList;

    public static final String getIconPathWithModifier(@Nonnull final AzureIcon azureIcon) {
        if (CollectionUtils.isEmpty(azureIcon.getModifierList())) {
            return azureIcon.getIconPath();
        }
        final File parent = new File(azureIcon.getIconPath()).getParentFile();
        final String iconName = azureIcon.getModifierList().stream().filter(Objects::nonNull).map(Modifier::getIconPath).collect(Collectors.joining("-"));
        final String path = new File(parent, String.format("%s.svg", iconName)).getPath();
        return FilenameUtils.separatorsToUnix(path);
    }

    @Getter
    @EqualsAndHashCode
    public static class Modifier {
        private final String iconPath;
        private final ModifierLocation location;
        private int horizontalShift;
        private int verticalShift;

        public Modifier(@Nonnull final String iconPath, @Nonnull final ModifierLocation location) {
            assert location != ModifierLocation.OTHER;
            this.iconPath = iconPath;
            this.location = location;
        }

        public Modifier(@Nonnull final String iconPath, int horizontalShift, int verticalShift) {
            this.iconPath = iconPath;
            this.location = ModifierLocation.OTHER;
            this.horizontalShift = horizontalShift;
            this.verticalShift = verticalShift;
        }
    }

    public enum ModifierLocation {
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        TOP_RIGHT,
        TOP_LEFT,
        OTHER
    }
}
