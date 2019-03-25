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
 * SOFTWARE.
 */

import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.maven.ExecuteKotlinScriptMojo

val mojo = ExecuteKotlinScriptMojo.INSTANCE
val log = mojo.getLog()

val targetDir = mojo.project.build.directory

if (System.getProperty("os.name").toLowerCase().contains("windows")) {
    val winutilsDir = File(Paths.get(targetDir, "tools", "winutils").toString())

    if (winutilsDir.exists()) {
        log.info("Winutils has been cloned to $winutilsDir, skip clone.")
    } else {
        log.info("Clone winutils from https://github.com/steveloughran/winutils to $winutilsDir")

        val proc = ProcessBuilder(
                    "git",
                    "clone",
                    "--depth",
                    "1",
                    "--branch",
                    "tag_2017-08-29-hadoop-2.8.1-native",
                    "https://github.com/steveloughran/winutils",
                    "$winutilsDir")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(10, TimeUnit.MINUTES)

        log.info(proc.inputStream.bufferedReader().readText())

        if (proc.exitValue() != 0) {
            log.error(proc.errorStream.bufferedReader().readText())
        } else {
            log.debug(proc.errorStream.bufferedReader().readText())
        }

        proc.exitValue()
    }
} else {
    log.info("No need to clone winutils for non-Windows platforms.")
}
