package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import org.jdesktop.swingx.HorizontalLayout;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AzdTemplatesLibrary extends JPanel {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Color activeColor = new Color(100, 150, 255);
    private final Color inactiveColor = Color.GRAY;
    private final Project project;
    private final List<AzdTemplate> templates;
    private final List<JBCheckBox> selectedFilters = new ArrayList<>();
    private final DialogWrapper dialogWrapper;

    public AzdTemplatesLibrary(Project project, DialogWrapper dialogWrapper) {
        super();
        this.project = project;
        this.dialogWrapper = dialogWrapper;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));
        this.templates = readFromGitHub("https://raw.githubusercontent.com/Azure/awesome-azd/refs/heads/main/website/static/templates.json");

        final List<String> allTags = topKTags(templates, 10);

        final JPanel tilesPanel = new JPanel();
        final JPanel filterTagsPanel = new JPanel(new HorizontalLayout(JBUI.scale(10)));
        filterTagsPanel.setBorder(JBUI.Borders.emptyBottom(10)); // Adds 10px space below the panel
        final JBLabel filterLabel = new JBLabel("Filter by tags:");
        filterTagsPanel.add(filterLabel);

        // Create scroll pane
        addFilters(filterTagsPanel, tilesPanel, allTags);

        final JScrollPane tagsScrollPane = ScrollPaneFactory.createScrollPane(filterTagsPanel);
        add(tagsScrollPane, BorderLayout.NORTH);

        // Create a scroll pane for the tiles
        tilesPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, true, true));

        tilesPanel.setBorder(JBUI.Borders.empty(10));

        // Add scroll pane with tiles panel
        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tilesPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        add(scrollPane, BorderLayout.CENTER);

        // Load data
        loadData(tilesPanel, Collections.emptyList());
    }

    public static List<AzdTemplate> readFromGitHub(String githubUrl) {
        try {
            // Read JSON from URL directly into the Repository model
            return OBJECT_MAPPER.readValue(new URL(githubUrl), new TypeReference<List<AzdTemplate>>() {
            });
        } catch (IOException e) {
            System.err.println("Error reading JSON from GitHub URL: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void addFilters(JPanel filterTagsPanel, JPanel tilesPanel, List<String> allTags) {
        allTags.forEach(tag -> {
            final JBCheckBox tagCheckbox = new JBCheckBox(tag);
            tagCheckbox.addActionListener(e -> {
                final List<String> selectedTags = selectedFilters.stream()
                        .filter(AbstractButton::isSelected)
                        .map(AbstractButton::getText)
                        .collect(Collectors.toList());

                loadData(tilesPanel, selectedTags);
            });
            filterTagsPanel.add(tagCheckbox);
            selectedFilters.add(tagCheckbox);
        });
    }

    /**
     * Load data from command execution and create tiles
     */
    private void loadData(JPanel tilesPanel, List<String> tags) {
        final List<AzdTemplate> javaTemplates = templates.stream()
                .filter(template -> template.getLanguages() != null && template.getLanguages().contains("java"))
                .filter(template -> tags == null || tags.isEmpty() || template.getTags().containsAll(tags))
                .toList();

        tilesPanel.removeAll();
        for (final AzdTemplate item : javaTemplates) {
            createTemplateTile(item, tilesPanel);
        }
        tilesPanel.revalidate();
        tilesPanel.repaint();
    }

    private void createTemplateTile(AzdTemplate item, JPanel tilesPanel) {
        // Title at the top
        final JBLabel titleLabel = new JBLabel(item.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 2));
        titleLabel.setBorder(JBUI.Borders.empty(0, 0, 3, 0));
        tilesPanel.add(titleLabel);

        final JBLabel label = new JBLabel("<html><p style='width: 900px;'>" + item.getDescription() + "<p></html>");
        tilesPanel.add(label);

        final HyperlinkLabel githubLink = new HyperlinkLabel(item.getAuthorUrl());
        githubLink.addHyperlinkListener(e -> BrowserUtil.browse(((HyperlinkLabel) ((HyperlinkEvent) e).getSource()).getText()));
        githubLink.setBorder(JBUI.Borders.empty(2, 0, 3, 0));

        tilesPanel.add(githubLink);

        final JBPanel commandPanel = new JBPanel(new HorizontalLayout(JBUI.scale(5)));
        commandPanel.setBorder(JBUI.Borders.emptyBottom(15));
        commandPanel.add(new JLabel((AllIcons.Debugger.Console)));

        final JBLabel commandLabel = new JBLabel("Command: ");
        commandPanel.add(commandLabel);
        final JTextField textBox = new JTextField(100);
        textBox.setMaximumSize(new Dimension(200, 30));
        textBox.setText("azd init -t " + item.getSource());

        textBox.setFont(textBox.getFont().deriveFont(Font.TRUETYPE_FONT, textBox.getFont().getSize()));
        commandPanel.add(textBox);

        final AzureActionButton<Void> runButton = new AzureActionButton<>();
        runButton.setText("Run");
        runButton.requestFocusInWindow();
        runButton.addActionListener(e -> {
            final String command = textBox.getText();
            if (command.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Command cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Show confirmation dialog
            final ConfirmAndRunDialog createDialog = new ConfirmAndRunDialog(project, "Create from template")
                    .setLabel("Create a new project from the selected template?")
                    .setEventName("create-from-template")
                    .setOnOkAction(() -> {
                        dialogWrapper.close(DialogWrapper.OK_EXIT_CODE);
                        AzdUtils.executeInTerminal(project, command);
                    })
                    .setOkButtonText("Create");

            createDialog.show();

        });
        commandPanel.add(runButton);

        tilesPanel.add(commandPanel);

        final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        tilesPanel.add(separator);
    }

    public static List<String> topKTags(List<AzdTemplate> input, int k) {
        // Count occurrences
        final Map<String, Integer> frequencyMap = new HashMap<>();
        final List<String> tags = input.stream()
                .filter(template -> template.getLanguages() != null && template.getLanguages().contains("java"))
                .flatMap(template -> template.getTags().stream())
                .toList();

        for (final String str : tags) {
            frequencyMap.put(str, frequencyMap.getOrDefault(str, 0) + 1);
        }

        // Sort by frequency in descending order
        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}