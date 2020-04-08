/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.SerializationFilterBase;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AzureRunConfigurationBase<T> extends LocatableConfigurationBase<T> implements LocatableConfiguration {
    private boolean firstTimeCreated = true;

    protected AzureRunConfigurationBase(@NotNull Project project, @NotNull ConfigurationFactory factory, @Nullable String name) {
        super(project, factory, name);
    }

    public abstract T getModel();

    public abstract String getTargetName();

    public abstract String getTargetPath();

    public abstract String getSubscriptionId();

    public abstract void validate() throws ConfigurationException;

    public final boolean isFirstTimeCreated() {
        return firstTimeCreated;
    }

    public final void setFirstTimeCreated(boolean firstTimeCreated) {
        this.firstTimeCreated = firstTimeCreated;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        firstTimeCreated = Comparing.equal(element.getAttributeValue("default"), "true");
        XmlSerializer.deserializeInto(getModel(), element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        XmlSerializer.serializeInto(getModel(), element, new SerializationFilterBase() {
            @Override
            protected boolean accepts(@NotNull Accessor accessor, @NotNull Object bean, @Nullable Object beanValue) {
                if (accessor == null || bean == null) {
                    return false;
                }
                return !(accessor.getName() instanceof String && accessor.getName().equalsIgnoreCase("password"));
            }
        });
    }

    @Override
    public void checkConfiguration() {
    }
}
