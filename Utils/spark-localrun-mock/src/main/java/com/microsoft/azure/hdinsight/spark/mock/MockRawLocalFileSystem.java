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

package com.microsoft.azure.hdinsight.spark.mock;


import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.util.StringUtils;

import java.io.DataOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

class MockRawLocalFileSystem extends RawLocalFileSystem {
    private String authority;
    private String scheme = "wasb";
    private URI uri;

    @Override
    public FileStatus getFileStatus(Path f) throws IOException {
        File path = pathToFile(f);
        if (path.exists()) {
            return new RawLocalFileStatus(pathToFile(f), getDefaultBlockSize(f), makeQualified(f));
        } else {
            throw new FileNotFoundException("File " + f + " does not exist");
        }
    }

    @Override
    public FileStatus[] listStatus(Path f) throws IOException {
        File localf = pathToFile(f);
        FileStatus[] results;

        if (!localf.exists()) {
            throw new FileNotFoundException("File " + f + " does not exist");
        }
        if (localf.isFile()) {
            return new FileStatus[] {
                    new RawLocalFileStatus(localf, getDefaultBlockSize(f), makeQualified(f)) };
        }

        String[] names = localf.list();
        if (names == null) {
            return null;
        }
        results = new FileStatus[names.length];
        int j = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                // Assemble the path using the Path 3 arg constructor to make sure
                // paths with colon are properly resolved on Linux
                results[j] = getFileStatus(new Path(f, new Path(null, null, names[i])));
                j++;
            } catch (FileNotFoundException e) {
                // ignore the files not found since the dir list may have have changed
                // since the names[] list was generated.
            }
        }
        if (j == names.length) {
            return results;
        }
        return Arrays.copyOf(results, j);
    }

    @Override
    public void initialize(URI uri, Configuration conf) throws IOException {
        super.initialize(uri, conf);

        this.authority = uri.getAuthority();
        this.scheme = uri.getScheme();

        try {
            this.uri = new URI(scheme, authority, "/", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public URI getUri() {
        return Optional.ofNullable(uri)
                .orElse(URI.create("wasb:///"));
    }

    @Override
    public Path getWorkingDirectory() {
        Path workingDir = new Path("/user/current");

        // Default URI is wasb:///
        return workingDir.makeQualified(URI.create("wasb:///"), workingDir);
    }

    @Override
    protected void checkPath(Path path) { }

    static class RawLocalFileStatus extends FileStatus {
        private final File file;

        /* We can add extra fields here. It breaks at least CopyFiles.FilePair().
         * We recognize if the information is already loaded by check if
         * onwer.equals("").
         */
        private boolean isPermissionLoaded() {
            return !super.getOwner().isEmpty();
        }

        RawLocalFileStatus(File f, long defaultBlockSize, Path p) {
            super(f.length(), f.isDirectory(), 1, defaultBlockSize, f.lastModified(), p);
            this.file = f;
        }

        @Override
        public FsPermission getPermission() {
            if (!isPermissionLoaded()) {
                loadPermissionInfo();
            }
            return super.getPermission();
        }

        @Override
        public String getOwner() {
            if (!isPermissionLoaded()) {
                loadPermissionInfo();
            }
            return super.getOwner();
        }

        @Override
        public String getGroup() {
            if (!isPermissionLoaded()) {
                loadPermissionInfo();
            }
            return super.getGroup();
        }

        /// loads permissions, owner, and group from `ls -ld`
        private void loadPermissionInfo() {
            IOException e = null;
            try {
                List<String> args = new ArrayList<>(Arrays.asList(Shell.getGetPermissionCommand()));
                args.add(this.file.getCanonicalPath());
                String output = Shell.execCommand(args.toArray(new String[0]));

                StringTokenizer t = new StringTokenizer(output, Shell.TOKEN_SEPARATOR_REGEX);
                //expected format
                //-rw-------    1 username groupname ...
                String permission = t.nextToken();
                if (permission.length() > 10) { //files with ACLs might have a '+'
                    permission = permission.substring(0, 10);
                }
                setPermission(FsPermission.valueOf(permission));
                t.nextToken();

                String owner = t.nextToken();
                // If on windows domain, token format is DOMAIN\\user and we want to
                // extract only the user name
                if (Shell.WINDOWS) {
                    int i = owner.indexOf('\\');
                    if (i != -1)
                        owner = owner.substring(i + 1);
                }
                setOwner(owner);

                setGroup(t.nextToken());
            } catch (Shell.ExitCodeException ioe) {
                if (ioe.getExitCode() != 1) {
                    e = ioe;
                } else {
                    setPermission(null);
                    setOwner(null);
                    setGroup(null);
                }
            } catch (IOException ioe) {
                e = ioe;
            } finally {
                if (e != null) {
                    throw new RuntimeException("Error while running command to get file permissions : " +
                            StringUtils.stringifyException(e));
                }
            }
        }

        @Override
        public void write(DataOutput out) throws IOException {
            if (!isPermissionLoaded()) {
                loadPermissionInfo();
            }
            super.write(out);
        }
    }

    @Override
    public File pathToFile(Path path) {
        @NotNull
        Path realPath;

        URI originUri = path.toUri();

        if (originUri.getScheme() != null &&
                (originUri.getScheme().toLowerCase().equals("file") ||
                 originUri.getScheme().toLowerCase().equals("mockfs"))) {
            realPath = path;
        } else {
            if (!path.isAbsolute()) {
                path = new Path(getWorkingDirectory(), path);
            }

            List<String> components = Arrays.stream(path.toUri().getPath().split(Path.SEPARATOR))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    // skip the driver name, like C:, D:
                    .skip((Path.WINDOWS && Path.isWindowsAbsolutePath(path.toUri().getPath(), true)) ? 1 : 0)
                    .collect(Collectors.toList());

            Path fsRoot = Optional.ofNullable(Optional.ofNullable(originUri.getAuthority())
                                                      .orElse(this.authority))
                    .filter(auth -> !auth.isEmpty())
                    .map(auth -> new Path(new Path(System.getProperty("user.dir")).getParent().getParent().getParent(), auth))
                    .orElse(new Path(System.getProperty("user.dir")).getParent().getParent());

            realPath = new Path(fsRoot,
                                Optional.of(String.join(Path.SEPARATOR, components))
                                        .filter(child -> !child.trim().isEmpty())
                                        .orElse("."));
        }

        return new File(realPath.toUri().getPath());
    }

}
