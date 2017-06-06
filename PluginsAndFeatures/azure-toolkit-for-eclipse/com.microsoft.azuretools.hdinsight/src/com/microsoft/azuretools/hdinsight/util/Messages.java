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
package com.microsoft.azuretools.hdinsight.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME =
    		"com.microsoft.azuretools.hdinsight.util.messages";
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
    public static String SparkProjectSystemJavaCreation;
    public static String SparkProjectSystemJavaSampleCreation;
    public static String SparkProjectSystemScalaCreation;
    public static String SparkProjectSystemScalaSampleCreation;
    public static String SparkProjectSystemOtherCreation;

	public static String SparkSubmissionRightClickProject;
	public static String SparkSubmissionButtonClickEvent;
	public static String SparkSubmissionHelpClickEvent;
	public static String SparkSubmissionStopButtionClickEvent;

	public static String HDInsightExplorerHDInsightNodeExpand;
	public static String HDInsightExplorerSparkNodeExpand;
	public static String HDInsightExplorerStorageAccountExpand;
	public static String HDInsightExplorerContainerOpen;
	public static String HDInsightFeatureEnabled;
	public static String HDInsightDownloadSparkLibrary;
	public static String HDInsightJobViewOpenAction;
    /**
     * Constructor.
     */
    private Messages() {
        super();
    }
}
