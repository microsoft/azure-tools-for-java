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

package com.microsoft.intellij.servicebinding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Text;
import com.microsoft.azuretools.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Setter
@Getter
public class InternalBindingInfo {
    @Attribute("className")
    private String className;
    @Text
    private String jsonpText;

    public static InternalBindingInfo fromBindingInfo(@NotNull ServiceBindingInfo info) {
        InternalBindingInfo internalBindingInfo = new InternalBindingInfo();
        internalBindingInfo.setClassName(info.getClass().getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> props = mapper.convertValue(info, Map.class);
        internalBindingInfo.setJsonpText(JsonUtils.getGson().toJson(props));
        return internalBindingInfo;
    }

    @SneakyThrows
    public ServiceBindingInfo toBindingInfo() {
        Class<? extends ServiceBindingInfo> clz = (Class<? extends ServiceBindingInfo>) Class.forName(this.className);
        Map<String, Object> properties = JsonUtils.fromJsonString(this.jsonpText, Map.class);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(properties, clz);
    }
}
