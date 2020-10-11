package com.microsoft.azure.appservice.component.form.input;

import com.intellij.openapi.ui.ComboBox;
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
    private static final String ERROR_LOADING_ITEMS = "Failed to list resources";

    public AzureComboBox() {
        super();
        this.init();
        this.refresh();
    }

    protected void init() {
        final ExtendableTextComponent.Extension extension = this.getExtension();
        if (Objects.nonNull(extension)) {
            this.setEditable(true);
            this.setEditor(new AzureComboBoxEditor());
        }
        this.setRenderer(new SimpleListCellRenderer<T>() {
            @Override
            public void customize(final JList l, final Object o, final int i, final boolean b, final boolean b1) {
                setText(getItemText(o));
                setIcon(getItemIcon(o));
            }
        });
    }

    public void refresh() {
        this.loadItemsAsync()
            .subscribe(items -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                this.removeAllItems();
                setItems(items);
            }), this::errorOnLoadingItems);

    }

    @Override
    public T getValue() {
        final T item = (T) this.getSelectedItem();
        return item;
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

    @Nullable
    protected T getDefaultValue() {
        return null;
    }

    protected SchedulerProvider getSchedulerProvider() {
        return SchedulerProviderFactory.getInstance().getSchedulerProvider();
    }

    protected void errorOnLoadingItems(Throwable e) {
        final MvpUIHelper uiHelper = MvpUIHelperFactory.getInstance().getMvpUIHelper();
        if (uiHelper != null) {
            uiHelper.showException(ERROR_LOADING_ITEMS, (Exception) e);
        }
    }

    @NotNull
    protected abstract List<? extends T> loadItems() throws Exception;


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
            final ExtendableTextComponent.Extension extension = getExtension();
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
    }
}
