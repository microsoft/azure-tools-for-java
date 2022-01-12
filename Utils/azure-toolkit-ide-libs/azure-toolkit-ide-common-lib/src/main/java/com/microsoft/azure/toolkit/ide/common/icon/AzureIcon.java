/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.icon;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class AzureIcon {
    private String iconPath;
    private List<Modifier> modifierList;

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
