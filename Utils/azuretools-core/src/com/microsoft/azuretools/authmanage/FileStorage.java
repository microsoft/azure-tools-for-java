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

package com.microsoft.azuretools.authmanage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.microsoft.azuretools.adauth.StringUtils;

/**
 * Created by shch on 10/5/2016.
 */
public class FileStorage {
    private static final String DefaultDir = ".msauth4j";
    private Path filePath;
    private ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

    public FileStorage(String filename, String baseDirPath) throws IOException {

        if (StringUtils.isNullOrEmpty(filename)) {
            throw new IllegalArgumentException("filename is null or empty");
        }

        Path baseDir = (!StringUtils.isNullOrEmpty(baseDirPath))
                ? Paths.get(baseDirPath)
                : Paths.get(System.getProperty("user.home"), DefaultDir)
                ;

        //Path dirPath = Paths.get(baseDir, WorkingDir);
        if (!Files.exists(baseDir)) {
            Files.createDirectory(baseDir);
        }

        filePath = Paths.get(baseDir.toString(), filename);
        //log.info("filePath = '" + filePath + "'");

        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }

    public byte[] read() throws IOException {
        try {
            rwlock.readLock().lock();
            return Files.readAllBytes(filePath);

        } finally {
            rwlock.readLock().unlock();
        }
    }

    public void write(byte[] data) throws IOException {
        try {
            rwlock.writeLock().lock();
            Files.write(filePath, data);

        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void cleanFile() throws IOException {
        write(new byte[]{});
    }

    public void append(byte[] data) throws IOException {
        try {
            rwlock.writeLock().lock();
            Files.write(filePath, data, StandardOpenOption.APPEND);

        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void append(String string) throws IOException {
        append(string.getBytes());
    }

    public void appendln(String string) throws IOException {
        append(string+"\n");
    }
}
