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

package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azure.toolkit.intellij.appservice.component.AzureFormInputComponent;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelper;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import rx.Observable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public abstract class AzureComboBox<T> extends ComboBox<T> implements AzureFormInputComponent<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final String ERROR_LOADING_ITEMS = "Failed to list resources";
    private AzureComboBoxEditor loadingSpinner;
    private AzureComboBoxEditor inputEditor;
    @Getter
    @Setter
    private boolean required;

    public AzureComboBox() {
        this(true);
    }

    public AzureComboBox(boolean refresh) {
        super();
        this.init();
        if (refresh) {
            DefaultLoader.getIdeHelper().invokeLater(this::refreshItems);
        }
    }

    @Override
    public JComponent getInputComponent() {
        return this;
    }

    protected void init() {
        this.loadingSpinner = new AzureComboBoxLoadingSpinner();
        this.inputEditor = new AzureComboBoxEditor();
        this.setEditable(true);
        this.setEditor(this.inputEditor);
        this.setRenderer(new SimpleListCellRenderer<T>() {
            @Override
            public void customize(final JList l, final Object o, final int i, final boolean b, final boolean b1) {
                setText(getItemText(o));
                setIcon(getItemIcon(o));
            }
        });
        if (isFilterable()) {
            this.addPopupMenuListener(new AzureComboBoxPopupMenuListener());
        }
    }

    public void refreshItems() { // TODO: @wangmi add debouncing
        this.setLoading(true);
        this.loadItemsAsync()
            .subscribe(items -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                this.removeAllItems();
                setItems(items);
                this.setLoading(false);
            }), (e) -> {
                this.handleLoadingError(e);
                this.setLoading(false);
            });
    }

    protected void setLoading(final boolean loading) {
        if (loading) {
            this.setEnabled(false);
            this.setEditor(this.loadingSpinner);
        } else {
            this.setEnabled(true);
            this.setEditor(this.inputEditor);
        }
        this.repaint();
    }

    @Override
    public T getValue() {
        return (T) this.getSelectedItem();
    }

    protected String getItemText(Object item) {
        if (item == null) {
            return StringUtils.EMPTY;
        }
        return item.toString();
    }

    @Nullable
    protected Icon getItemIcon(Object item) {
        return null;
    }

    @Nullable
    protected ExtendableTextComponent.Extension getExtension() {
        return null;
    }

    protected void setItems(final List<? extends T> items) {
        items.forEach(this::addItem);
        final T defaultValue = this.getDefaultValue();
        if (defaultValue != null && items.contains(defaultValue)) {
            this.setSelectedItem(defaultValue);
        }
    }

    protected Observable<? extends List<? extends T>> loadItemsAsync() {
        return Observable.fromCallable(this::loadItems).subscribeOn(getSchedulerProvider().io());
    }

    @NotNull
    protected abstract List<? extends T> loadItems() throws Exception;

    @Nullable
    protected T getDefaultValue() {
        return null;
    }

    protected SchedulerProvider getSchedulerProvider() {
        return SchedulerProviderFactory.getInstance().getSchedulerProvider();
    }

    protected void handleLoadingError(Throwable e) {
        final MvpUIHelper uiHelper = MvpUIHelperFactory.getInstance().getMvpUIHelper();
        if (uiHelper != null) {
            uiHelper.showException(ERROR_LOADING_ITEMS, (Exception) e);
        }
    }

    protected boolean isFilterable() {
        return true;
    }

    public void clear() {
        this.removeAllItems();
        this.setSelectedItem(null);
    }

    class AzureComboBoxEditor extends BasicComboBoxEditor {

        private Object item;

        @Override
        public void setItem(Object item) {
            this.item = item;
            if (!AzureComboBox.this.isPopupVisible()) {
                this.editor.setText(getItemText(item));
            }
        }

        @Override
        public Object getItem() {
            return this.item;
        }

        @Override
        protected JTextField createEditorComponent() {
            final ExtendableTextField textField = new ExtendableTextField();
            final ExtendableTextComponent.Extension extension = this.getExtension();
            if (extension != null) {
                textField.addExtension(extension);
            }
            textField.setBorder(null);
            textField.setEditable(false);
            return textField;
        }

        protected ExtendableTextComponent.Extension getExtension() {
            return AzureComboBox.this.getExtension();
        }
    }

    class AzureComboBoxLoadingSpinner extends AzureComboBoxEditor {

        @Override
        public void setItem(Object item) {
            super.setItem(item);
            if (item == null) {
                this.editor.setText("Refreshing...");
            }
        }

        protected ExtendableTextComponent.Extension getExtension() {
            return ExtendableTextComponent.Extension.create(
                    new AnimatedIcon.Default(), null, null);
        }
    }

    class AzureComboBoxPopupMenuListener extends PopupMenuListenerAdapter {
        List<T> itemList;
        ComboFilterListener comboFilterListener;

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            getEditorComponent().setEditable(true);
            getEditorComponent().setText(StringUtils.EMPTY);
            itemList = UIUtils.listComboBoxItems(AzureComboBox.this);
            // todo: support customized combo box filter
            comboFilterListener = new ComboFilterListener(itemList,
                                                          (item, input) -> StringUtils.containsIgnoreCase(getItemText(item), input));
            getEditorComponent().getDocument().addDocumentListener(comboFilterListener);
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            getEditorComponent().setEditable(false);
            if (comboFilterListener != null) {
                getEditorComponent().getDocument().removeDocumentListener(comboFilterListener);
            }
            final Object selectedItem = AzureComboBox.this.getSelectedItem();
            AzureComboBox.this.removeAllItems();
            AzureComboBox.this.setItems(itemList);
            AzureComboBox.this.setSelectedItem(selectedItem);
            getEditorComponent().setText(getItemText(selectedItem));
        }

        private JTextField getEditorComponent() {
            return (JTextField) inputEditor.getEditorComponent();
        }
    }

    class ComboFilterListener extends DocumentAdapter {

        private final List<? extends T> list;
        private final BiPredicate<? super T, ? super String> filter;

        public ComboFilterListener(List<? extends T> list, BiPredicate<? super T, ? super String> filter) {
            super();
            this.list = list;
            this.filter = filter;
        }

        @Override
        protected void textChanged(@NotNull final DocumentEvent documentEvent) {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                try {
                    final String text = documentEvent.getDocument().getText(0, documentEvent.getDocument().getLength());
                    AzureComboBox.this.removeAllItems();
                    AzureComboBox.this.setItems(list.stream().filter(item -> filter.test(item, text)).collect(Collectors.toList()));
                } catch (BadLocationException e) {
                    // swallow exception and show all items
                }
            });
        }
    }
}
