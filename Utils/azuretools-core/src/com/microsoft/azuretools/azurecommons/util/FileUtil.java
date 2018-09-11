/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.azuretools.azurecommons.util;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtil {
	
	private static final int BUFF_SIZE = 1024;
    
    /**
     * Method writes contents of file.
     * @param inStream
     * @param outStream
     * @throws IOException
     */
    public static void writeFile(InputStream inStream, OutputStream outStream)
    		throws IOException {

        try {
            byte[] buf = new byte[BUFF_SIZE];
            int len = inStream.read(buf);
            while (len > 0) {
                outStream.write(buf, 0, len);
                len = inStream.read(buf);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
        }
    }
    
	/**
	 * Copies jar file from zip 
	 * @throws IOException 
	 */
	public static boolean copyFileFromZip(File zipResource, String fileName, File destFile) 
	throws IOException {
		
		boolean success = false;
		
		ZipFile zipFile = new ZipFile(zipResource);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		
		File destParentFile = destFile.getParentFile();
		
		// create parent directories if not existing
		if (destFile != null && destParentFile != null	&& !destParentFile.exists()) {
			destParentFile.mkdir();
		}

		while (entries.hasMoreElements()) {
	            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
	            if (zipEntry.getName().equals(fileName)) {
	            	writeFile(zipFile.getInputStream(zipEntry), new BufferedOutputStream(new FileOutputStream(destFile)));
	            	success = true; 
	            	break;
	            }
		}
		zipFile.close();
		
		return success;
	}
	
	/**
	 * Utility method to check for null conditions or empty strings.
	 * @param name 
	 * @return true if null or empty string
	 */
	public static boolean isNullOrEmpty(final String name) {
		boolean isValid = false;
		if (name == null || name.matches("\\s*")) {
			isValid = true;
		}
		return isValid;
	}

    /**
     * Utility method to zip the given source file to the destination file.
     * @param sourceFile source file
     * @param targetZipFile ZIP file that will be created or overwritten
     */
    public static void zipFile(final @NotNull File sourceFile, final @NotNull File targetZipFile) throws Exception {
        if (!sourceFile.exists()) {
            throw new Exception("The source file to zip does not exist.");
        }

        final String targetZipFileName = targetZipFile.getName();
        final String targetZipFileExtension = targetZipFileName.substring(targetZipFileName.lastIndexOf(".")+1);
        if (!targetZipFileExtension.equalsIgnoreCase("zip")) {
            throw new Exception("The target file should be a .zip file.");
        }

        final FileOutputStream fos = new FileOutputStream(targetZipFile);
        final ZipOutputStream zipOut = new ZipOutputStream(fos);
        final FileInputStream inputStream = new FileInputStream(sourceFile);
        try {
            final ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
            zipOut.putNextEntry(zipEntry);
            final byte[] bytes = new byte[BUFF_SIZE];
            int length;
            while ((length = inputStream.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        } finally {
            if(zipOut != null) {
                zipOut.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

}
