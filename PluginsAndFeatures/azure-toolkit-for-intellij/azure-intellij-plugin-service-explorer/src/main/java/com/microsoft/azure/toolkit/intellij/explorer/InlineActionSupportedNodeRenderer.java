package com.microsoft.azure.toolkit.intellij.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.hover.TreeHoverListener;
import com.microsoft.azure.toolkit.ide.common.component.ActionNode;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.Tree;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.common.component.TreeUtils.INLINE_ACTION_ICON_WIDTH;
import static com.microsoft.azure.toolkit.intellij.common.component.TreeUtils.KEY_SCROLL_PANE;

public class InlineActionSupportedNodeRenderer extends NodeRenderer {
    private List<Icon> inlineActionIcons = new ArrayList<>();
    private Rectangle viewportRect;
    private int scrolledWidth;

    @Override
    public void customizeCellRenderer(@Nonnull JTree jtree, final Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
        if (value instanceof Tree.TreeNode<?> node) {
            //noinspection UnstableApiUsage
            final int hoveredRow = TreeHoverListener.getHoveredRow(jtree);
            this.inlineActionIcons = node.getInlineActionViews().stream()
                .map(av -> IntelliJAzureIcons.getIcon(av.getIconPath()))
                .filter(icon -> hoveredRow == row || icon == AllIcons.Nodes.Favorite).toList();
            this.viewportRect = Optional.ofNullable((JBScrollPane) jtree.getClientProperty(KEY_SCROLL_PANE))
                .map(JBScrollPane::getViewport).map(JViewport::getViewRect).orElse(null);
            if (node.getInner() instanceof ActionNode) {
                TreeUtils.renderActionNode(jtree, node, selected, this);
            } else {
                TreeUtils.renderMyTreeNode(jtree, node, selected, this);
            }
            return;
        } else if (value instanceof Tree.LoadMoreNode node) {
            this.inlineActionIcons = Collections.emptyList();
            TreeUtils.renderLoadModeNode(jtree, node, selected, this);
            return;
        }
        super.customizeCellRenderer(jtree, value, selected, expanded, isLeaf, row, focused);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // WARNING: `this.getX()` is `0` when painting hovered floating overflow node.
        if (this.getX() > 0 && CollectionUtils.isNotEmpty(this.inlineActionIcons) && Objects.nonNull(this.viewportRect)) {
            paintInlineActionIcons(g);
        }
    }

    private void paintInlineActionIcons(Graphics g) {
        // WARNING: `this.getX()` is `0` when painting hovered floating overflow node.
        int prevIconBoxOffset = this.viewportRect.x + this.viewportRect.width - this.getX() - TreeUtils.NODE_PADDING;
        for (final Icon icon : inlineActionIcons) {
            final int iconBoxWidth = INLINE_ACTION_ICON_WIDTH + TreeUtils.INLINE_ACTION_ICON_MARGIN;
            final int iconBoxOffset = prevIconBoxOffset - iconBoxWidth;
            if (iconBoxOffset > 0) {
                final int iconOffset = iconBoxOffset + TreeUtils.INLINE_ACTION_ICON_MARGIN / 2;
                g.setColor(getBackground());
                g.fillRect(iconBoxOffset, 0, iconBoxWidth, getHeight());
                paintIcon(g, icon, iconOffset);
                prevIconBoxOffset = iconBoxOffset;
            }
        }
    }
}