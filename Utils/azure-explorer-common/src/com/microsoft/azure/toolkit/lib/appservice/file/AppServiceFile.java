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

package com.microsoft.azure.toolkit.lib.appservice.file;

import com.microsoft.azure.management.appservice.WebAppBase;
import lombok.Data;

import java.util.Objects;

@Data
public class AppServiceFile {
    private String name;
    private long size;
    private String mtime;
    private String crtime;
    private String mime;
    private String href;
    private String path;
    private WebAppBase app;

    public String getId() {
        return String.format("<%s>/%s", this.getApp().id(), this.getPath());
    }

    public String getFullName() {
        return String.format("<%s>/%s", this.getApp().name(), this.getName());
    }

    public Type getType() {
        return Objects.equals("inode/directory", this.mime) ? Type.DIRECTORY : Type.FILE;
    }

    public enum Type {
        DIRECTORY, FILE
    }
}
