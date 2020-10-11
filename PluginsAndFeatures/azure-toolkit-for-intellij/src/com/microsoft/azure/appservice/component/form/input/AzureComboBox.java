package com.microsoft.azure.appservice.component.form.input;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azure.appservice.component.form.AzureFormInput;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelper;
import com.microsoft.azuretools.core.mvp.ui.base.MvpUIHelperFactory;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang.StringUtils;
import rx.Observable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.util.List;
import java.util.Objects;

public abstract class AzureComboBox<T> extends ComboBox<T> implements AzureFormInput<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final String ERROR_LOADING_ITEMS = "Failed to list resources";
    private ComboBoxEditor loadingSpinner;
    private ComboBoxEditor inputEditor;

    public AzureComboBox() {
        super();
        this.init();
        this.refreshItems();
    }

    protected void init() {
        final ExtendableTextComponent.Extension extension = this.getExtension();
        if (Objects.nonNull(extension)) {
            this.setEditable(true);
            this.setEditor(new AzureComboBoxEditor());
        }
        this.loadingSpinner = new AzureComboBoxLoadingSpinner();
        this.inputEditor = this.getEditor();
        this.setRenderer(new SimpleListCellRenderer<T>() {
            @Override
            public void customize(final JList l, final Object o, final int i, final boolean b, final boolean b1) {
                setText(getItemText(o));
                setIcon(getItemIcon(o));
            }
        });
    }

    public void refreshItems() {
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
            this.setEditable(true);
            this.setEnabled(false);
            this.setEditor(this.loadingSpinner);
        } else {
            this.setEditable(true);
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

    private Observable<? extends List<? extends T>> loadItemsAsync() {
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

    public void clear() {
        this.removeAllItems();
        this.setSelectedItem(null);
    }

    class AzureComboBoxEditor extends BasicComboBoxEditor {
        private Object item;
        private ExtendableTextField extendableEditor;

        @Override
        public void setItem(Object item) {
            this.item = item;
            extendableEditor.setText(getItemText(this.item));
            extendableEditor.getAccessibleContext().setAccessibleName(extendableEditor.getText());
            extendableEditor.getAccessibleContext().setAccessibleDescription(extendableEditor.getText());
        }

        @Override
        public Object getItem() {
            return item;
        }

        @Override
        protected JTextField createEditorComponent() {
            final ExtendableTextComponent.Extension extension = this.getExtension();
            if (extendableEditor == null && extension != null) {
                synchronized (this) {
                    // init extendableTextField here as intellij may call `createEditorComponent` before constructor done
                    if (extendableEditor == null) {
                        extendableEditor = new ExtendableTextField();
                        extendableEditor.addExtension(extension);
                        extendableEditor.setBorder(null);
                    }
                }
            }
            return extendableEditor;
        }

        protected ExtendableTextComponent.Extension getExtension() {
            return AzureComboBox.this.getExtension();
        }
    }

    class AzureComboBoxLoadingSpinner extends AzureComboBoxEditor {
        protected ExtendableTextComponent.Extension getExtension() {
            return ExtendableTextComponent.Extension.create(
                    new AnimatedIcon.Default(), null, null);
        }
    }
}
