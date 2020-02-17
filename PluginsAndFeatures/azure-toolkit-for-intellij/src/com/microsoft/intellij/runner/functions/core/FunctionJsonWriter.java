package com.microsoft.intellij.runner.functions.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FunctionJsonWriter {
    public static void writeFunctionJsonFile(File file, FunctionConfigurationV2 config) throws IOException {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("scriptFile", config.getScriptFile());
        json.put("entryPoint", config.getEntryPoint());
        List<Map<String, Object>> lists = new ArrayList<>();
        if (config.getBindings() != null) {
            for (BindingV2 binding : config.getBindings()) {
                Map<String, Object> bindingJson = new LinkedHashMap<>();
                bindingJson.put("type", binding.getType());
                bindingJson.put("direction", binding.getDirection());
                bindingJson.put("name", binding.getName());
                final Map<String, Object> attributes = binding.getBindingAttributes();
                for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
                    // Skip 'name' property since we have serialized before the for-loop
                    if (bindingJson.containsKey(entry.getKey())) {
                        continue;
                    }
                    bindingJson.put(entry.getKey(), entry.getValue());
                }
                lists.add(bindingJson);
            }
            json.put("bindings", lists.toArray());
        }

        FileUtils.write(file, GSON.toJson(json), "utf8");
    }

    private static final Gson GSON =  new GsonBuilder().setPrettyPrinting().create();
}
