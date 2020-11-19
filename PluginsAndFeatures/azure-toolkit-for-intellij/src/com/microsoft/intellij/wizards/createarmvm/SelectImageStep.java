/*
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

package com.microsoft.intellij.wizards.createarmvm;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.wizard.WizardNavigationState;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.*;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.RxJavaUtils;
import com.microsoft.intellij.wizards.VMWizardModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SelectImageStep extends AzureWizardStep<VMWizardModel> implements TelemetryProperties {
    private static final String ERROR_MESSAGE_LIST_PUBLISHER = "An error occurred while attempting to retrieve publisher list. \n%s";
    private static final String ERROR_MESSAGE_LIST_IMAGES = "An error occurred while attempting to retrieve images list. \n%s";
    private static final String ERROR_MESSAGE_FILL_SKUS = "An error occurred while attempting to retrieve skus list. \n%s";
    private static final String ERROR_MESSAGE_FILL_OFFER = "An error occurred while attempting to retrieve offers list. \n%s";

    private JPanel rootPanel;
    private JList createVmStepsList;
    private JComboBox regionComboBox;

    private JList imageLabelList;
    private JComboBox publisherComboBox;
    private JComboBox offerComboBox;
    private JComboBox skuComboBox;
    private JRadioButton knownImageBtn;
    private JRadioButton customImageBtn;
    private JComboBox knownImageComboBox;
    private JLabel publisherLabel;
    private JLabel offerLabel;
    private JLabel skuLabel;
    private JLabel versionLabel;

    private VMWizardModel model;
    private Azure azure;
    private Project project;

    private Subscription fillPublisherSubscription;
    private Subscription fillOfferSubscription;
    private Subscription fillSkuSubscription;
    private Subscription fillImagesSubscription;

    public SelectImageStep(final VMWizardModel model, Project project) {
        super("Select a Virtual Machine Image", null, null);

        this.model = model;
        this.project = project;

        model.configStepList(createVmStepsList, 1);

        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            azure = azureManager.getAzure(model.getSubscription().getSubscriptionId());
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to authenticate\n\n" + ex.getMessage(), ex);
        }
        regionComboBox.setRenderer(new ListCellRendererWrapper<Object>() {

            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o != null && (o instanceof Location)) {
                    setText("  " + ((Location) o).displayName());
                }
            }
        });
        regionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectRegion();
            }
        });
        publisherComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualMachinePublisher) {
                    setText(((VirtualMachinePublisher) o).name());
                }
            }
        });

        publisherComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillOffers();
                }
            }
        });

        offerComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualMachineOffer) {
                    setText(((VirtualMachineOffer) o).name());
                }
            }
        });

        offerComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillSkus();
                }
            }
        });

        skuComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof VirtualMachineSku) {
                    setText(((VirtualMachineSku) o).name());
                }
            }
        });

        skuComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillImages();
                }
            }
        });

        imageLabelList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object o, int i, boolean b, boolean b1) {
                String cellValue = o.toString();

                if (o instanceof VirtualMachineImage) {
                    VirtualMachineImage virtualMachineImage = (VirtualMachineImage) o;
                    cellValue = virtualMachineImage.version();
                }

                this.setToolTipText(cellValue);
                return super.getListCellRendererComponent(list, cellValue, i, b, b1);
            }
        });

        imageLabelList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                VirtualMachineImage virtualMachineImage = (VirtualMachineImage) imageLabelList.getSelectedValue();
                model.setVirtualMachineImage(virtualMachineImage);

                if (virtualMachineImage != null) {
                    model.getCurrentNavigationState().NEXT.setEnabled(true);
                }
            }
        });
        final ButtonGroup imageGroup = new ButtonGroup();
        imageGroup.add(knownImageBtn);
        imageGroup.add(customImageBtn);
        knownImageComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                model.setKnownMachineImage(knownImageComboBox.getSelectedItem());
            }
        });
        List<Object> knownImages = new ArrayList<>();
        knownImages.addAll(Arrays.asList(KnownWindowsVirtualMachineImage.values()));
        knownImages.addAll(Arrays.asList(KnownLinuxVirtualMachineImage.values()));
        knownImageComboBox.setModel(new DefaultComboBoxModel(knownImages.toArray()));
        model.setKnownMachineImage(knownImageComboBox.getSelectedItem());
        knownImageComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList list, Object o, int i, boolean b, boolean b1) {
                if (o instanceof KnownWindowsVirtualMachineImage) {
                    setText(((KnownWindowsVirtualMachineImage) o).offer() + " - " + ((KnownWindowsVirtualMachineImage) o).sku());
                } else if (o instanceof KnownLinuxVirtualMachineImage) {
                    setText(((KnownLinuxVirtualMachineImage) o).offer() + " - " + ((KnownLinuxVirtualMachineImage) o).sku());
                }
            }
        });
        final ItemListener updateListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                enableControls(!knownImageBtn.isSelected());
            }
        };
        knownImageBtn.addItemListener(updateListener);
        customImageBtn.addItemListener(updateListener);
        customImageBtn.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                fillPublishers();
            }
        });
        knownImageBtn.setSelected(true);
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();

        // will set to null if selected subscription changes
        if (model.getRegion() == null) {
            Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
            if (subscription2Location == null || subscription2Location.get(model.getSubscription()) == null) {
                final DefaultComboBoxModel<String> loadingModel = new DefaultComboBoxModel<>(new String[]{"<Loading...>"});
                regionComboBox.setModel(loadingModel);
                model.getCurrentNavigationState().NEXT.setEnabled(false);
                DefaultLoader.getIdeHelper().runInBackground(
                        project, "Loading Available Locations...", false, true,
                        "Loading Available Locations...", () -> {
                        try {
                            AzureModelController.updateSubscriptionMaps(null);
                            DefaultLoader.getIdeHelper().invokeLater(() -> fillRegions());
                        } catch (Exception ex) {
                            PluginUtil.displayErrorDialogInAWTAndLog("Error", "Error loading locations", ex);
                        }
                    });
            } else {
                fillRegions();
            }
        }
        if ((knownImageBtn.isSelected() && knownImageComboBox.getSelectedItem() == null) ||
                (customImageBtn.isSelected() && imageLabelList.getSelectedValue() == null)) {
            disableNext();
        }
        return rootPanel;
    }

    @Override
    public Map<String, String> toProperties() {
        return model.toProperties();
    }

    private void enableControls(boolean customImage) {
        model.setKnownMachineImage(knownImageBtn.isSelected());
        knownImageComboBox.setEnabled(!customImage);
        model.getCurrentNavigationState().NEXT.setEnabled(!customImage || !imageLabelList.isSelectionEmpty());
        imageLabelList.setEnabled(customImage);
        publisherComboBox.setEnabled(customImage);
        offerComboBox.setEnabled(customImage);
        skuComboBox.setEnabled(customImage);
        publisherLabel.setEnabled(customImage);
        offerLabel.setEnabled(customImage);
        skuLabel.setEnabled(customImage);
        versionLabel.setEnabled(customImage);
    }

    private void fillRegions() {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(model.getSubscription())
                .stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
        regionComboBox.setModel(new DefaultComboBoxModel(locations.toArray()));
        if (locations.size() > 0) {
            selectRegion();
        }
        enableControls(customImageBtn.isSelected());
    }

    private void selectRegion() {
        if (customImageBtn.isSelected()) {
            fillPublishers();
        }
        model.setRegion((Location) regionComboBox.getSelectedItem());
    }

    private void fillPublishers() {
        if (customImageBtn.isSelected()) {
            disableNext();

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading publishers...", false) {
                @Override
                public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);

                    final Object selectedItem = regionComboBox.getSelectedItem();
                    final Location location = selectedItem instanceof Location ? (Location) selectedItem : null;
                    if (location == null) {
                        return;
                    }
                    clearSelection(publisherComboBox, offerComboBox, skuComboBox, imageLabelList);
                    RxJavaUtils.unsubscribeSubscription(fillPublisherSubscription);
                    fillPublisherSubscription =
                            Observable.fromCallable(() -> azure.virtualMachineImages().publishers().listByRegion(location.name()))
                                      .subscribeOn(Schedulers.io())
                                      .subscribe(publisherList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                                          publisherComboBox.setModel(new DefaultComboBoxModel(publisherList.toArray()));
                                          fillOffers();
                                      }),
                                          error -> {
                                              final String msg = String.format(ERROR_MESSAGE_LIST_PUBLISHER,
                                                                               String.format(message("webappExpMsg"), error.getMessage()));
                                              handleError(msg, error);
                                          });
                }
            });
        }
    }

    private void fillOffers() {
        disableNext();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading offers...", false) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                RxJavaUtils.unsubscribeSubscription(fillOfferSubscription);
                clearSelection(offerComboBox, skuComboBox, imageLabelList);
                fillOfferSubscription =
                        Observable.fromCallable(() -> ((VirtualMachinePublisher) publisherComboBox.getSelectedItem()).offers().list())
                                  .subscribeOn(Schedulers.io())
                                  .subscribe(offerList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                                      offerComboBox.setModel(new DefaultComboBoxModel(offerList.toArray()));
                                      fillSkus();
                                  }),
                                      error -> {
                                          final String msg = String.format(ERROR_MESSAGE_FILL_SKUS,
                                                                           String.format(message("webappExpMsg"), error.getMessage()));
                                          handleError(msg, error);
                                      });
            }
        });
    }

    private void fillSkus() {
        disableNext();

        if (offerComboBox.getItemCount() > 0) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading skus...", false) {
                @Override
                public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);
                    RxJavaUtils.unsubscribeSubscription(fillSkuSubscription);
                    clearSelection(skuComboBox, imageLabelList);
                    fillSkuSubscription =
                            Observable.fromCallable(() -> ((VirtualMachineOffer) offerComboBox.getSelectedItem()).skus().list())
                                      .subscribeOn(Schedulers.io())
                                      .subscribe(skuList -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                                          skuComboBox.setModel(new DefaultComboBoxModel(skuList.toArray()));
                                          fillImages();
                                      }),
                                          error -> {
                                              String msg = String.format(ERROR_MESSAGE_FILL_SKUS,
                                                                         String.format(message("webappExpMsg"), error.getMessage()));
                                              handleError(msg, error);
                                          });
                }
            });
        } else {
            skuComboBox.removeAllItems();
            imageLabelList.setListData(new Object[]{});
        }
    }

    private void fillImages() {
        disableNext();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading images...", false) {
            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                VirtualMachineSku sku = (VirtualMachineSku) skuComboBox.getSelectedItem();
                if (sku != null) {
                    RxJavaUtils.unsubscribeSubscription(fillImagesSubscription);
                    clearSelection(imageLabelList);
                    fillImagesSubscription =
                            Observable.fromCallable(() -> sku.images().list())
                                      .subscribeOn(Schedulers.io())
                                      .subscribe(imageList -> DefaultLoader.getIdeHelper().invokeLater(() -> imageLabelList.setListData(imageList.toArray())),
                                          error -> {
                                              String msg = String.format(ERROR_MESSAGE_LIST_IMAGES,
                                                                         String.format(message("webappExpMsg"), error.getMessage()));
                                              handleError(msg, error);
                                          });
                }
            }
        });
    }

    private void clearSelection(JComponent... components) {
        for (JComponent component : components) {
            if (component instanceof JComboBox) {
                ((JComboBox) component).removeAllItems();
            } else if (component instanceof JList) {
                ((JList) component).setListData(new Object[]{});
            }
        }
    }

    private void disableNext() {
        //validation might be delayed, so lets check if we are still on this screen
        if (customImageBtn.isSelected() && model.getCurrentStep().equals(this)) {
            model.getCurrentNavigationState().NEXT.setEnabled(false);
        }
    }

    private void handleError(String errorMessage, Throwable throwable) {
        final Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        PluginUtil.displayErrorDialogInAWTAndLog(message("errTtl"), errorMessage, throwable);
    }
}
