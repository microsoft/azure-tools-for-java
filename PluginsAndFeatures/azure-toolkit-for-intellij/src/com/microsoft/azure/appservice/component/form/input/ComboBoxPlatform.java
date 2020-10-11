package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azure.appservice.Platform;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Arrays;
import java.util.List;

public class ComboBoxPlatform extends AzureComboBox<Platform> {
    @NotNull
    @Override
    protected List<? extends Platform> loadItems() throws Exception {
        final Platform[] platforms = {
                Platform.builder().os("linux").language("java8").build(),
                Platform.builder().os("linux").language("java8").server("tomcat8.5").build(),
                Platform.builder().os("linux").language("java8").server("tomcat9").build(),
                Platform.builder().os("linux").language("java11").build(),
                Platform.builder().os("linux").language("java11").server("tomcat8.5").build(),
                Platform.builder().os("linux").language("java11").server("tomcat9").build()
        };
        return Arrays.asList(platforms);
    }
}
