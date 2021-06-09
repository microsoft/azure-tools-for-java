/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import java.awt.event.ItemEvent;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AzureComboBox<T> extends ComboBox<T> implements AzureFormInputComponent<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final int DEBOUNCE_DELAY = 500;
    private final TailingDebouncer refresher;
    private AzureComboBoxEditor loadingSpinner;
    private AzureComboBoxEditor inputEditor;
    @Getter
    @Setter
    private boolean required;
    private Object value;
    private boolean valueNotSet = true;
    private String label;
    @Nullable
    @Setter
    protected Boolean valueFixed;
    @Getter
    @Setter
    private Supplier<? extends List<? extends T>> itemsLoader;

    public AzureComboBox() {
        this(true);
    }

    public AzureComboBox(boolean refresh) {
        super();
        this.init();
        this.refresher = new TailingDebouncer(this::doRefreshItems, DEBOUNCE_DELAY);
        if (refresh) {
            AzureTaskManager.getInstance().runLater(this::refreshItems, AzureTask.Modality.ANY);
        }
    }

    public AzureComboBox(@Nonnull Supplier<? extends List<? extends T>> itemsLoader) {
        this(itemsLoader, true);
    }

    public AzureComboBox(@Nonnull Supplier<? extends List<? extends T>> itemsLoader, boolean refresh) {
        this(refresh);
        this.itemsLoader = itemsLoader;
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
        this.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull final JList<? extends T> l, final T t, final int i, final boolean b,
                                  final boolean b1) {
                setText(getItemText(t));
                setIcon(getItemIcon(t));
            }
        });
        if (isFilterable()) {
            this.addPopupMenuListener(new AzureComboBoxPopupMenuListener());
        }
        final TailingDebouncer valueDebouncer = new TailingDebouncer(() -> {
            @SuppressWarnings("unchecked")
            final ValueListener<T>[] listeners = this.listenerList.getListeners(ValueListener.class);
            for (final ValueListener<T> listener : listeners) {
                listener.onValueChanged(this.getValue());
            }
        }, DEBOUNCE_DELAY);
        this.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.refreshValue();
            }
            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                valueDebouncer.debounce();
            }
        });
    }

    @Override
    public T getValue() {
        return ((T) this.getSelectedItem());
    }

    @Override
    public void setValue(final T val) {
        this.setValue(val, null);
    }

    public void setValue(final T val, final Boolean fixed) {
        Optional.ofNullable(fixed).ifPresent(f -> {
            this.valueFixed = fixed;
            this.setEditable(!f);
        });
        this.valueNotSet = false;
        this.value = val;
        this.refreshValue();
    }

    public void setValue(final ItemReference<T> val) {
        this.setValue(val, null);
    }

    public void setValue(final ItemReference<T> val, final Boolean fixed) {
        Optional.ofNullable(fixed).ifPresent(f -> {
            this.valueFixed = fixed;
            this.setEditable(!f);
        });
        this.valueNotSet = false;
        this.value = val;
        this.refreshValue();
    }

    private void refreshValue() {
        if (Objects.equals(this.value, this.getSelectedItem())) {
            return;
        }
        final List<T> items = this.getItems();
        if (!this.valueNotSet && this.value instanceof AzureComboBox.ItemReference) {
            items.stream().filter(i -> ((ItemReference<?>) this.value).is(i)).findFirst().ifPresent(this::setValue);
        }
        if (this.valueNotSet && this.value == null && !items.isEmpty()) {
            super.setSelectedItem(items.get(0));
        } else if (items.contains(this.value)) {
            super.setSelectedItem(this.value);
        } else if (value instanceof Draft) {
            // todo: unify model for custom created resource
            super.addItem((T) value);
            super.setSelectedItem(value);
        } else {
            super.setSelectedItem(null);
        }
    }

    @Override
    public void setSelectedItem(final Object value) {
        this.setValue((T) value);
    }

    public void refreshItems() {
        this.refresher.debounce();
    }

    @AzureOperation(
        name = "common|combobox.load_items",
        params = {"this.label()"},
        type = AzureOperation.Type.ACTION
    )
    private void doRefreshItems() {
        try {
            this.setLoading(true);
            final List<? extends T> items = this.loadItemsInner();
            this.setLoading(false);
            AzureTaskManager.getInstance().runLater(() -> this.setItems(items), AzureTask.Modality.ANY);
        } catch (final Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
                return;
            }
            this.setLoading(false);
            this.handleLoadingError(e);
        }
    }

    public List<T> getItems() {
        final List<T> result = new ArrayList<>();
        for (int i = 0; i < this.getItemCount(); i++) {
            result.add(this.getItemAt(i));
        }
        return result;
    }

    protected void setItems(final List<? extends T> items) {
        this.removeAllItems();
        items.forEach(super::addItem);
        this.refreshValue();
    }

    public void clear() {
        this.setItems(Collections.emptyList());
    }

    protected void setLoading(final boolean loading) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (loading) {
                this.setEnabled(false);
                this.setEditor(this.loadingSpinner);
            } else {
                this.setEnabled(!Objects.equals(valueFixed, true));
                this.setEditor(this.inputEditor);
            }
        }, AzureTask.Modality.ANY);
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

    protected Observable<? extends List<? extends T>> loadItemsAsync() {
        return Observable.fromCallable(this::loadItemsInner).subscribeOn(Schedulers.io());
    }

    protected final List<? extends T> loadItemsInner() throws Exception {
        if (Objects.nonNull(this.itemsLoader)) {
            return this.itemsLoader.get();
        } else {
            return this.loadItems();
        }
    }

    @Nonnull
    protected List<? extends T> loadItems() throws Exception {
        return Collections.emptyList();
    }

    @Nullable
    protected T getDefaultValue() {
        return null;
    }

    protected void handleLoadingError(Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureMessager.getMessager().error(e);
    }

    protected boolean isFilterable() {
        return true;
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

        @Nullable
        protected ExtendableTextComponent.Extension getExtension() {
            final ExtendableTextComponent.Extension extension = AzureComboBox.this.getExtension();
            return extension == null ? null : ExtendableTextComponent.Extension.create(
                extension.getIcon(true), extension.getTooltip(), () -> {
                    AzureComboBox.this.hidePopup();
                    extension.getActionOnClick().run();
                });
        }
    }

    class AzureComboBoxLoadingSpinner extends AzureComboBoxEditor {

        @Override
        public void setItem(Object item) {
            // do nothing: item can not be set on loading
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
            itemList = AzureComboBox.this.getItems();
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
            if (!CollectionUtils.isEqualCollection(itemList, getItems())) {
                AzureComboBox.this.setItems(itemList);
            }
            if (!Objects.equals(selectedItem, AzureComboBox.this.getValue())) {
                AzureComboBox.this.setSelectedItem(selectedItem);
            }
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
        protected void textChanged(@Nonnull final DocumentEvent documentEvent) {
            AzureTaskManager.getInstance().runLater(() -> {
                try {
                    final String text = documentEvent.getDocument().getText(0, documentEvent.getDocument().getLength());
                    list.stream().filter(item -> filter.test(item, text) && !getItems().contains(item)).forEach(AzureComboBox.this::addItem);
                    getItems().stream().filter(item -> !filter.test(item, text)).forEach(AzureComboBox.this::removeItem);
                } catch (BadLocationException e) {
                    // swallow exception and show all items
                }
            }, AzureTask.Modality.ANY);
        }
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return Optional.ofNullable(this.label).orElse("");
    }

    private String label() {
        return Optional.ofNullable(this.label).orElse(this.getClass().getSimpleName());
    }

    public static class ItemReference<T> {
        private final Predicate<? super T> predicate;

        public ItemReference(@Nonnull Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        public ItemReference(@Nonnull Object val, Function<T, ?> mapper) {
            this.predicate = t -> Objects.equals(val, mapper.apply(t));
        }

        public boolean is(Object obj) {
            if (Objects.isNull(obj)) {
                return false;
            }
            return this.predicate.test((T) obj);
        }
    }

    public void addValueListener(ValueListener<T> listener) {
        this.listenerList.add(ValueListener.class, listener);
    }

    public void removeValueListener(ValueListener<T> listener) {
        this.listenerList.remove(ValueListener.class, listener);
    }

    @FunctionalInterface
    public interface ValueListener<T> extends EventListener {
        void onValueChanged(@Nullable T value);
    }
}
