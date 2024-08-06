/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextDelegate;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.LRUStack;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.accessibility.AccessibleContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

@SuppressWarnings("unchecked")
public class AzureComboBox<T> extends ComboBox<T> implements AzureFormInputComponent<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final int DEBOUNCE_DELAY = 500;
    private final TailingDebouncer reloader;
    private AzureComboBoxEditor myEditor;
    private boolean valueNotSet = true;
    private boolean loading = false;
    protected Object value;
    protected boolean enabled = true;
    @Getter
    @Setter
    private Supplier<? extends List<? extends T>> itemsLoader;
    private final TailingDebouncer valueDebouncer;

    public AzureComboBox() {
        this(true);
    }

    public AzureComboBox(boolean refresh) {
        super();
        this.init();
        this.reloader = new TailingDebouncer(this::doReloadItems, DEBOUNCE_DELAY);
        this.valueDebouncer = new TailingDebouncer(this::fireValueChangedEvent, DEBOUNCE_DELAY);
        setSwingPopup(false);
        this.myEditor = new AzureComboBoxEditor();
        this.setEditable(true);
        this.setEditor(this.myEditor);
        ComponentUtil.putClientProperty(this, ANIMATION_IN_RENDERER_ALLOWED, true);
        if (refresh) {
            this.reloadItems();
        }
    }

    public AzureComboBox(@Nonnull Supplier<? extends List<? extends T>> itemsLoader) {
        this(itemsLoader, true);
    }

    public AzureComboBox(@Nonnull Supplier<? extends List<? extends T>> itemsLoader, boolean refresh) {
        this(refresh);
        this.itemsLoader = itemsLoader;
    }

    protected void init() {
        this.setLoading(false);
        this.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@Nonnull final JList<? extends T> l, final T t, final int i, final boolean b,
                                  final boolean b1) {
                final String itemText = getItemText(t);
                setText(itemText);
                setIcon(getItemIcon(t));
                getAccessibleContext().setAccessibleDescription(itemText);
            }
        });
        this.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.refreshValue();
            }
            this.valueDebouncer.debounce();
        });
        this.trackValidation();
    }

    @Nullable
    @Override
    public T getValue() {
        return ((T) this.getSelectedItem());
    }

    @Nullable
    public Object getRawValue() {
        return this.value;
    }

    @Override
    public void setValue(final T val) {
        this.setValue(val, null);
    }

    public void setValue(final T val, final Boolean fixed) {
        UIUtil.invokeLaterIfNeeded(() -> {
            Optional.ofNullable(fixed).ifPresent(f -> {
                this.setEnabled(!f);
                this.setEditable(!f);
            });
            this.valueNotSet = false;
            this.value = val;
            if (Objects.nonNull(this.value)) {
                final LRUStack<T> history = (LRUStack<T>) CacheManager.getUsageHistory(value.getClass());
                history.push((T) value);
            }
            this.refreshValue();
        });
    }

    public void setValue(Predicate<T> predicate) {
        this.setValue(new ItemReference<>(predicate));
    }

    public void setValue(final ItemReference<T> val) {
        this.setValue(val, null);
    }

    public void setValue(final ItemReference<T> val, final Boolean fixed) {
        UIUtil.invokeLaterIfNeeded(() -> {
            Optional.ofNullable(fixed).ifPresent(f -> {
                this.setEnabled(!f);
                this.setEditable(!f);
            });
            this.valueNotSet = false;
            this.value = val;
            this.refreshValue();
        });
    }

    protected void refreshValue() {
        if (this.valueNotSet) {
            if (this.getItemCount() > 0 && this.getSelectedIndex() != 0) {
                super.setSelectedItem(this.getDefaultValue());
            }
        } else {
            final Object selected = this.getSelectedItem();
            if (Objects.equals(selected, this.value) || (this.value instanceof ItemReference && ((ItemReference<?>) this.value).is(selected))) {
                return;
            }
            final List<T> items = this.getItems();
            if (this.value instanceof AzureComboBox.ItemReference) {
                items.stream().filter(i -> ((ItemReference<?>) this.value).is(i)).findFirst().ifPresent(this::setValue);
            } else if (Objects.nonNull(this.value) && items.contains(this.value)) {
                super.setSelectedItem(items.get(items.indexOf(this.value))); // set the equivalent item in the list as selected.
            } else {
                super.setSelectedItem(null);
            }
            this.valueDebouncer.debounce();
        }
    }

    @Override
    public T getDefaultValue() {
        final List<T> items = this.getItems();
        final T value = doGetDefaultValue();
        final int index = items.indexOf(value);
        if (Objects.nonNull(value) && index > -1) {
            return items.get(index);
        } else {
            return this.getModel().getElementAt(0);
        }
    }

    @Nullable
    protected T doGetDefaultValue() {
        final List<T> items = this.getItems();
        //noinspection unchecked
        final LRUStack<T> history = (LRUStack<T>) CacheManager.getUsageHistory(items.get(0).getClass());
        return history.peek();
    }

    @Override
    public void setSelectedItem(final Object value) {
        this.setValue((T) value);
    }

    protected void refreshItems() {
        this.reloadItems();
    }

    public void reloadItems() {
        this.setLoading(true);
        this.reloader.debounce();
    }

    @AzureOperation(name = "internal/common.load_combobox_items.type", params = {"this.getLabel()"})
    private void doReloadItems() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.setLoading(true);
            this.setItems(this.loadItemsInner());
            this.setLoading(false);
        });
    }

    public List<T> getItems() {
        final List<T> result = new ArrayList<>();
        for (int i = 0; i < this.getItemCount(); i++) {
            result.add(this.getItemAt(i));
        }
        return result;
    }

    protected synchronized void setItems(final List<? extends T> items) {
        SwingUtilities.invokeLater(() -> {
            final DefaultComboBoxModel<T> model = (DefaultComboBoxModel<T>) this.getModel();
            final List<? extends T> oldItems = this.getItems();
            final List<? extends T> newItems = ObjectUtils.firstNonNull(items, Collections.emptyList());
            final List<? extends T> toRemove = ListUtils.removeAll(oldItems, newItems);
            final List<? extends T> toAdd = ListUtils.removeAll(newItems, oldItems);
            final T item = (T) model.getSelectedItem();
            toRemove.forEach(model::removeElement);
            model.addAll(toAdd);
            if (Objects.nonNull(item) && !newItems.contains(item)) {
                model.setSelectedItem(null);
            }
            this.refreshValue();
        });
    }

    public void clear() {
        this.value = null;
        this.valueNotSet = true;
        this.removeAllItems();
        this.refreshValue();
    }

    protected void setLoading(final boolean loading) {
        this.loading = loading;
        SwingUtilities.invokeLater(() -> {
            Optional.ofNullable(this.myEditor).ifPresent(AzureComboBoxEditor::rerender);
            Optional.ofNullable(this.myEditor).map(BasicComboBoxEditor::getEditorComponent).ifPresent(c -> c.setEnabled(AzureComboBox.this.isEnabled()));
            this.repaint();
        });
    }

    @Override
    public void setEnabled(boolean b) {
        this.enabled = b;
        super.setEnabled(b);
        this.setLoading(this.loading);
    }

    @Override
    public boolean isEnabled() {
        return !this.loading && this.enabled;
    }

    protected String getItemText(Object item) {
        if (item == null) {
            return StringUtils.EMPTY;
        } else if (item instanceof AzResource) {
            return ((AzResource) item).getName();
        }
        return item.toString();
    }

    @Nullable
    protected Icon getItemIcon(Object item) {
        return null;
    }

    @Nonnull
    protected List<Extension> getExtensions() {
        final ArrayList<Extension> list = new ArrayList<>();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK);
        final String tooltip = String.format("Refresh (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension refreshEx = Extension.create(AllIcons.Actions.Refresh, tooltip, this::refreshItems);
        this.registerShortcut(keyStroke, refreshEx);
        list.add(refreshEx);
        return list;
    }

    protected void registerShortcut(@Nonnull final KeyStroke keyStroke, @Nonnull final Extension ex) {
        // Add shot cut for extension, refers https://github.com/JetBrains/intellij-community/blob/idea/212.4746.92/platform/platform-api/
        // src/com/intellij/ui/components/fields/ExtendableTextField.java#L117
        final Runnable action = () -> {
            this.hidePopup();
            ex.getActionOnClick().run();
        };
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                action.run();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), this);

    }

    protected final List<? extends T> loadItemsInner() {
        try {
            if (Objects.nonNull(this.itemsLoader)) {
                return this.itemsLoader.get();
            } else {
                return this.loadItems();
            }
        } catch (final Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (!(rootCause instanceof InterruptedIOException) && !(rootCause instanceof InterruptedException)) {
                return Collections.emptyList();
            }
            this.handleLoadingError(e);
            return Collections.emptyList();
        }
    }

    @Nonnull
    protected List<? extends T> loadItems() throws Exception {
        return Collections.emptyList();
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
            this.editor.setText(getItemText(item));
        }

        @Override
        public Object getItem() {
            return this.item;
        }

        @Override
        protected JTextField createEditorComponent() {
            final ExtendableTextField textField = new ExtendableTextField() {
                @Override
                public synchronized AccessibleContext getAccessibleContext() {
                    final AccessibleContext context = super.getAccessibleContext();
                    final ExtendableTextField extendableTextField = this;
                    return new AccessibleContextDelegate(context) {
                        @Override
                        public String getAccessibleDescription() {
                            final String requiredDescription = AzureComboBox.this.isRequired() ? "Required" : StringUtils.EMPTY;
                            final String visibleDescription = AzureComboBox.this.isPopupVisible() ? "Expanded" : "Collapsed";
                            return Stream.of(super.getAccessibleDescription(), requiredDescription, visibleDescription)
                                    .filter(StringUtils::isNoneBlank)
                                    .collect(Collectors.joining(StringUtils.SPACE));

                        }

                        @Override
                        protected Container getDelegateParent() {
                            return AzureComboBox.this;
                        }
                    };
                }
            };
            textField.setEditable(false);
            final List<Extension> extensions = ObjectUtils.firstNonNull(this.getExtensions(), Collections.emptyList());
            extensions.stream().filter(Objects::nonNull).forEach(textField::addExtension);
            textField.setBorder(null);
            textField.setEditable(false);
            return textField;
        }

        @Nullable
        protected List<Extension> getExtensions() {
            if (!AzureComboBox.this.enabled) {
                return Collections.emptyList();
            }
            if (AzureComboBox.this.loading) {
                return List.of(Extension.create(new AnimatedIcon.Default(), "Loading...", null));
            }
            return AzureComboBox.this.getExtensions().stream()
                .map(e -> Extension.create(e.getIcon(false), e.getIcon(true), e.getTooltip(), () -> {
                    AzureComboBox.this.hidePopup(); // hide popup before action.
                    e.getActionOnClick().run();
                })).collect(Collectors.toList());
        }

        public void rerender() {
            final ExtendableTextField editor = (ExtendableTextField) this.getEditorComponent();
            editor.setText(getItemText(this.item));
            editor.setExtensions(this.getExtensions());
            if (AzureComboBox.this.enabled && AzureComboBox.this.loading) {
                editor.setToolTipText("Loading...");
                editor.getEmptyText().setText("Loading...");
            } else {
                editor.setToolTipText("");
                editor.getEmptyText().setText("");
            }
        }
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
}
