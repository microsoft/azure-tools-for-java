/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.collaboration.ui.codereview.comment.RoundedPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.ActionLink;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.PositionTracker;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class RatePopup {
    public static final JBColor BACKGROUND_COLOR =
        JBColor.namedColor("StatusBar.hoverBackground", new JBColor(15595004, 4606541));
    public static final int MOST_TIMES = 5;
    private static Balloon balloon;
    private final AzureConfiguration config;

    @Getter
    private JPanel contentPanel;
    private JLabel logo;
    private JLabel star0;
    private JLabel star1;
    private JLabel star2;
    private JLabel star3;
    private JLabel star4;
    private ActionLink issueLink;
    private ActionLink marketplaceLink;
    private ActionLink notNow;
    private ActionLink featureLink;

    private static final TailingDebouncer popup = new TailingDebouncer(() -> AzureTaskManager.getInstance().runLater(() -> popup(null)), 15000);

    public RatePopup() {
        super();
        $$$setupUI$$$();
        init();
        this.config = Azure.az().config();
    }

    private void createUIComponents() {
        final int arc = NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get();
        //noinspection UnstableApiUsage
        this.contentPanel = new RoundedPanel(new GridLayoutManager(4, 1), arc);
        this.contentPanel.setPreferredSize(new Dimension(338, 112));
        this.contentPanel.setBackground(BACKGROUND_COLOR);
        this.contentPanel.setBorder(BorderFactory.createEmptyBorder());
    }

    private void init() {
        this.initStars();
        this.initActions();
    }

    private void initActions() {
        this.marketplaceLink.addActionListener(this::reviewInMarketplace);
        this.issueLink.addActionListener(this::reportIssue);
        this.featureLink.addActionListener(this::requestFeature);
        this.notNow.addActionListener(e -> rateNextTime());
    }

    @AzureOperation(name = "user/feedback.rate_next_time")
    private static void rateNextTime() {
        final int times = Azure.az().config().get(RateManager.POPPED_TIMES, 0);
        popDaysLater(15 * times);
    }

    @AzureOperation(name = "user/feedback.report_issue")
    private void reportIssue(ActionEvent e) {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://aka.ms/azure-ij-new-issue");
        popDaysLater(90);
    }

    @AzureOperation(name = "user/feedback.request_feature")
    private void requestFeature(ActionEvent e) {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://aka.ms/azure-ij-new-feature");
        popDaysLater(90);
    }

    @AzureOperation(name = "user/feedback.review_marketplace")
    private void reviewInMarketplace(ActionEvent e) {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://aka.ms/azure-ij-new-review");
        popDaysLater(-1);
    }

    private void initStars() {
        this.logo.setIcon(IconLoader.getIcon("/icons/Common/Azure.svg", RatePopup.class));
        final float scale = 1.625f;
        final Icon outlined = IconUtil.scale(AllIcons.Nodes.NotFavoriteOnHover, this.contentPanel, scale);
        final Icon filled = IconUtil.scale(AllIcons.Nodes.Favorite, this.contentPanel, scale);
        final JLabel[] stars = {this.star0, this.star1, this.star2, this.star3, this.star4};
        Arrays.stream(stars).forEach(star -> star.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)));
        Arrays.stream(stars).forEach(star -> star.setIcon(outlined));
        Arrays.stream(stars).forEach(star -> star.addMouseListener(new MouseAdapter() {
            @Override
            @AzureOperation(name = "user/feedback.rate_in_popup")
            public void mouseClicked(MouseEvent e) {
                final int index = ArrayUtils.indexOf(stars, star);
                OperationContext.current().setTelemetryProperty("score", String.valueOf(index + 1));
                config.set(RateManager.RATED_AT, System.currentTimeMillis());
                config.set(RateManager.RATED_SCORE, index + 1);
                if (index >= 3) {
                    AzureMessager.getMessager().success("Thank you for the feedback!");
                    popDaysLater(-1);
                } else {
                    final Project project = ProjectUtils.getProject();
                    MonkeySurvey.openInIDE(project, index + 1);
                    popDaysLater(180);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                for (final JLabel s : stars) {
                    s.setIcon(filled);
                    if (s == star) {
                        break;
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Arrays.stream(stars).forEach(s -> s.setIcon(outlined));
            }
        }));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    @AzureOperation(name = "internal/feedback.try_popup_rating")
    public static synchronized boolean tryPopup(@Nullable Project project) {
        final int times = Azure.az().config().get(RateManager.POPPED_TIMES, 0);
        if (times < MOST_TIMES) {
            final long nextPopAfter = Azure.az().config().get(RateManager.NEXT_POP_AFTER, 0L);
            if (nextPopAfter >= 0 && System.currentTimeMillis() > nextPopAfter) {
                popup.debounce();
                return true;
            }
        }
        return false;
    }

    @AzureOperation(name = "boundary/feedback.show_popup_rating")
    public static synchronized void popup(@Nullable Project project) {
        if (RatePopup.balloon == null || RatePopup.balloon.isDisposed()) {
            final JPanel rateUsPanel = new RatePopup().getContentPanel();
            RatePopup.balloon = JBPopupFactory.getInstance().createBalloonBuilder(rateUsPanel)
                .setFadeoutTime(10000)
                .setAnimationCycle(200)
                .setShowCallout(false)
                .setShadow(false)
                .setBorderColor(ColorUtil.withAlpha(RatePopup.BACKGROUND_COLOR, 0))
                .setBorderInsets(JBUI.emptyInsets())
                .setFillColor(ColorUtil.withAlpha(RatePopup.BACKGROUND_COLOR, 0))
                .setHideOnClickOutside(false)
                .setHideOnFrameResize(false)
                .setHideOnKeyOutside(false)
                .setHideOnAction(false)
                .setHideOnLinkClick(true)
                .setCloseButtonEnabled(true)
                .setBlockClicksThroughBalloon(true)
                .createBalloon();
        }
        final AzureConfiguration config = Azure.az().config();
        final int times = config.get(RateManager.POPPED_TIMES, 0) + 1;
        config.set(RateManager.POPPED_AT, System.currentTimeMillis());
        config.set(RateManager.POPPED_TIMES, times);
        OperationContext.current().setTelemetryProperty("popped_times", String.valueOf(times));
        // popup 3 days later if the popup is faded out automatically
        popDaysLater(3);

        final JFrame frame = ((JFrame) ProjectUtils.getWindow(project));
        if (RatePopup.balloon.isDisposed()) {
            return;
        }
        RatePopup.balloon.show(new PositionTracker<>(frame.getRootPane()) {
            @Override
            public RelativePoint recalculateLocation(@NotNull Balloon balloon) {
                final Dimension frameSize = frame.getSize();
                final Dimension balloonSize = balloon.getPreferredSize();
                return new RelativePoint(frame, new Point(frameSize.width - balloonSize.width / 2 - 37, frameSize.height - balloonSize.height / 2 - 60));
            }
        }, Balloon.Position.above);
    }

    private static void popDaysLater(int x) {
        balloon.hide();
        final AzureConfiguration config = Azure.az().config();
        if (x == -1) {
            config.set(RateManager.NEXT_POP_AFTER, -1L);
        } else {
            config.set(RateManager.NEXT_POP_AFTER, System.currentTimeMillis() + x * DateUtils.MILLIS_PER_DAY);
        }
    }
}
