/*
 * Copyright (c) Microsoft Corporation
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
 */

package com.microsoft.azure.hdinsight.spark.mock;


import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
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
    private boolean useWindowsFileStatus = System.getProperty("os.name").toLowerCase().startsWith("windows");

    @Override
    public URI getUri() {
        try {
            return new URI("mockfs:///");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getWorkingDirectory() {
        Path workingDir = new Path("/user/current");

        // Default URI is wasb:///
        return workingDir.makeQualified(URI.create("wasb:///"), workingDir);
    }

    @Override
    protected void checkPath(Path path) { }

    @Override
    public FileStatus getFileStatus(Path f) throws IOException {
        return getFileLinkStatusInternal(f, true);
    }

    @Override
    public FileStatus getFileLinkStatus(final Path f) throws IOException {
        FileStatus fi = getFileLinkStatusInternal(f, false);
        // getFileLinkStatus is supposed to return a symlink with a
        // qualified path
        if (fi.isSymlink()) {
            Path targetQualifiedSymlink = FSLinkResolver.qualifySymlinkTarget(this.getUri(),
                    fi.getPath(), fi.getSymlink());
            fi.setSymlink(targetQualifiedSymlink);
        }

        return fi;
    }

    @Override
    public Path getLinkTarget(Path f) throws IOException {
        FileStatus fi = getFileLinkStatusInternal(f, false);
        // return an unqualified symlink target
        return fi.getSymlink();
    }

    // Refer to RawLocalFileSystem, override private codes
    // Get File or Link status from supported native file system
    private FileStatus getNativeFileLinkStatus(final Path f,
                                               boolean dereference) throws IOException {
        MockStat stat = new MockStat(f, getDefaultBlockSize(f), dereference, this);
        FileStatus status = stat.getFileStatus();
        status.setPath(f);

        return status;
    }

    private FileStatus getFileLinkStatusInternal(final Path f,
                                                 boolean dereference) throws IOException {
        if (!useWindowsFileStatus) {
            return getNativeFileLinkStatus(f, dereference);
        } else if (dereference) {
            File path = pathToFile(f);
            if (path.exists()) {
                return new WindowsRawLocalFileStatus(pathToFile(f), getDefaultBlockSize(f), f);
            } else {
                throw new FileNotFoundException("File " + f + " does not exist");
            }
        } else {
            return getWindowsFileLinkStatusInternal(f);
        }
    }

    private FileStatus getWindowsFileLinkStatusInternal(final Path f)
            throws IOException {
        String target = FileUtil.readLink(new File(f.toString()));

        try {
            FileStatus fs = getFileStatus(f);
            // If f refers to a regular file or directory
            if (target.isEmpty()) {
                return fs;
            }
            // Otherwise f refers to a symlink
            return new FileStatus(fs.getLen(),
                    false,
                    fs.getReplication(),
                    fs.getBlockSize(),
                    fs.getModificationTime(),
                    fs.getAccessTime(),
                    fs.getPermission(),
                    fs.getOwner(),
                    fs.getGroup(),
                    new Path(target),
                    f);
        } catch (FileNotFoundException e) {
            /* The exists method in the File class returns false for dangling
             * links so we can get a FileNotFoundException for links that exist.
             * It's also possible that we raced with a delete of the link. Use
             * the readBasicFileAttributes method in java.nio.file.attributes
             * when available.
             */
            if (!target.isEmpty()) {
                return new FileStatus(0, false, 0, 0, 0, 0, FsPermission.getDefault(),
                        "", "", new Path(target), f);
            }
            // f refers to a file or directory that does not exist
            throw e;
        }
    }

    @Override
    public FileStatus[] listStatus(Path f) throws IOException {
        File localFile = pathToFile(f);
        FileStatus[] results;

        if (!localFile.exists()) {
            throw new FileNotFoundException("File " + f + " does not exist");
        }
        if (localFile.isFile()) {
            if (!useWindowsFileStatus) {
                return new FileStatus[] { getFileStatus(f) };
            }
            return new FileStatus[] { new WindowsRawLocalFileStatus(localFile, getDefaultBlockSize(f), f) };
        }

        String[] names = localFile.list();
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

            Path fsRoot = Optional.ofNullable(originUri.getAuthority())
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

    static class WindowsRawLocalFileStatus extends FileStatus {
        File file;
        private boolean isPermissionLoaded() {
            return !super.getOwner().isEmpty();
        }

        WindowsRawLocalFileStatus(File f, long defaultBlockSize, Path p) {
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
                if (permission.length() > FsPermission.MAX_PERMISSION_LENGTH) {
                    //files with ACLs might have a '+'
                    permission = permission.substring(0,
                            FsPermission.MAX_PERMISSION_LENGTH);
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

    class MockStat extends Stat {
        private Path originPath;

        public MockStat(Path path, long blockSize, boolean deref, FileSystem fs) throws IOException {
            super(path, blockSize, deref, fs);

            this.originPath = path;
        }

        @Override
        protected String[] getExecString() {
            String[] execArgs = super.getExecString();

            if (execArgs.length > 1) {
                // Override the wasb or mockfs path with the converted local file path
                execArgs[execArgs.length - 1] = pathToFile(this.originPath).getPath();
            }

            return execArgs;
        }
    }
}