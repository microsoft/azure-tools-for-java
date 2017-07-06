/**
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

package com.microsoft.azuretools.container.ui.wizard.publish;

import org.eclipse.jface.wizard.Wizard;

public class PublishWizard extends Wizard {
    private static final String TEXT_WINDOW_TITLE = "Publish to Web App on Linux";
    private StepOnePage p1;
    private StepTwoPage p2;

    /**
     *  Constructor.
     */
    public PublishWizard() {
        setWindowTitle(TEXT_WINDOW_TITLE);
        this.setNeedsProgressMonitor(false);
        p1 = new StepOnePage();
        p2 = new StepTwoPage();
    }

    @Override
    public void addPages() {
        addPage(p1);
        addPage(p2);
    }

    @Override
    public boolean performFinish() {
        return p1.isPageComplete() && p2.isPageComplete();
    }

}
