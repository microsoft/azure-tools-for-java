package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azure.toolkit.intellij.AzureFormInput;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public abstract class AzureComboBox<T> extends ComboBox<T> implements AzureFormInput<T> {
    public static final String EMPTY_ITEM = StringUtils.EMPTY;
    private static final String ERROR_LOADING_ITEMS = "Failed to list resources";
    private AzureComboBoxEditor loadingSpinner;
    private AzureComboBoxEditor inputEditor;

    public AzureComboBox() {
        super();
        this.init();
        this.refreshItems();
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

    protected boolean isFilterable() {
        return true;
    }

    public void clear() {
        this.removeAllItems();
        this.setSelectedItem(null);
    }

    class AzureComboBoxEditor extends BasicComboBoxEditor {
        private Object item;
        private ExtendableTextField textField;

        @Override
        public void setItem(Object item) {
            this.item = item;
            if (!AzureComboBox.this.isPopupVisible()) {
                this.editor.setText(getItemText(this.item));
            }
        }

        @Override
        public Object getItem() {
            return item;
        }

        @Override
        protected JTextField createEditorComponent() {
            return getTextField();
        }

        protected ExtendableTextField getTextField() {
            if (textField == null) {
                synchronized (this) {
                    // init extendableTextField here as intellij may call `createEditorComponent` before constructor done
                    if (textField == null) {
                        textField = new ExtendableTextField();
                        final ExtendableTextComponent.Extension extension = this.getExtension();
                        if (extension != null) {
                            textField.addExtension(extension);
                        }
                        textField.setBorder(null);
                        textField.setEditable(false);
                    }
                }
            }
            return textField;
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

    class AzureComboBoxPopupMenuListener extends PopupMenuListenerAdapter {
        List<T> itemList;
        ComboFilterListener listener;

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            inputEditor.getTextField().setEditable(true);
            inputEditor.getTextField().setText(StringUtils.EMPTY);
            itemList = new ArrayList<>();
            for (int i = 0; i < AzureComboBox.this.getItemCount(); i++) {
                itemList.add((T) AzureComboBox.this.getItemAt(i));
            }
            // todo: support customized combo box filter
            listener = new ComboFilterListener(itemList, (item, input) -> StringUtils.containsIgnoreCase(getItemText(item), input));
            inputEditor.getTextField().getDocument().addDocumentListener(listener);
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            inputEditor.getTextField().setEditable(false);
            if (listener != null) {
                inputEditor.getTextField().getDocument().removeDocumentListener(listener);
            }
            final Object selectedItem = AzureComboBox.this.getSelectedItem();
            AzureComboBox.this.removeAllItems();
            AzureComboBox.this.setItems(itemList);
            AzureComboBox.this.setSelectedItem(selectedItem);
            inputEditor.getTextField().setText(getItemText((T) selectedItem));
        }
    }

    class ComboFilterListener extends DocumentAdapter {

        private List<T> list;
        private BiPredicate<T, String> filter;

        public ComboFilterListener(List<T> list, BiPredicate<T, String> filter) {
            this.list = list;
            this.filter = filter;
        }

        @Override
        protected void textChanged(@NotNull final DocumentEvent documentEvent) {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                try {
                    String text = documentEvent.getDocument().getText(0, documentEvent.getDocument().getLength());
                    AzureComboBox.this.removeAllItems();
                    list.stream().filter(item -> filter.test(item, text)).forEach(item -> AzureComboBox.this.addItem(item));
                } catch (BadLocationException e) {
                    // swallow exception and show all items
                }
            });
        }
    }
}
