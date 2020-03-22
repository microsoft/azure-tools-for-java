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
package com.microsoft.azuretools.core.ui;

import java.net.URI;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureDialogWrapper;

public class LoginWindow implements com.microsoft.azuretools.adauth.IWebUi {
    private static ILog LOG = Activator.getDefault().getLog();

    private String res = null;

    private void setResult(String res) {
        this.res = res;
    }

    private String getResult() {
        return res;
    }

    public LoginWindow(){
        //System.out.println("==> SwtBrowserWIndow ctor---------------");
    }

    @Override
    //public Future<String> authenticateAsync(URI requestUri, URI redirectUri) {
    public String authenticate(URI requestUri, URI redirectUri) {

        System.out.println("==> run authenticateAsync ---------------");

        final String redirectUriStr = redirectUri.toString();
        final String requestUriStr = requestUri.toString();
        System.out.println("\tredirectUriStr: " + redirectUriStr);
        System.out.println("\trequestUriStr: " + requestUriStr);

        if (!requestUriStr.contains("login_hint")) {
            System.out.println("\t--> Browser.clearSessions()");
            Browser.clearSessions();
        }

        final Runnable gui = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("==> run gui ---------------");
                    Display display = Display.getDefault();
                    final Shell activeShell = display.getActiveShell();
                    LoginDialog dlg = new LoginDialog(activeShell, redirectUriStr, requestUriStr);
                    dlg.open();
                    setResult(dlg.getResult());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@Runnable@LoginWindow", ex));
                }
            }
        };
        Display.getDefault().syncExec(gui);

//            final Callable<String> worker = new Callable<String>() {
//                @Override
//                public String call() {
//                    return getResult();
//                }
//            };
        // just to return future to comply interface
        //return Executors.newSingleThreadExecutor().submit(worker);

        return getResult();
    }
}

class LoginDialog extends AzureDialogWrapper {

    final String redirectUriStr;
    final String requestUriStr;

    private String res = null;

    private void setResult(String res) {
        this.res = res;
    }

    public String getResult() {
        return res;
    }

    public LoginDialog(Shell parentShell, String redirectUri, String requestUri) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.redirectUriStr = redirectUri;
        this.requestUriStr = requestUri;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Azure Login Dialog");
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        return ctrl;
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
      GridLayout layout = (GridLayout)parent.getLayout();
      layout.marginHeight = 0;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 750);
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite) super.createDialogArea(parent);

        FillLayout fillLayout = new FillLayout();
        fillLayout.type = SWT.VERTICAL;
        container.setLayout(fillLayout);

        createDlgBody(container);
        return container;
    }

    private void createDlgBody(Composite container) {
        final Browser browser = new Browser(container, SWT.NONE);

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent locationEvent) {
                System.out.println("\t--> locationEvent.location: " + locationEvent.location);
                if(locationEvent.location.startsWith(redirectUriStr)) {
                    setResult(locationEvent.location);
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            //Browser.clearSessions();
                            close();
                        }
                    });
                }
            }
        });

//        String[] headers = new String[] {
//                "User-Agent: SwtBrowser"
//        };
//        browser.setUrl(requestUriStr, null, headers);
        browser.setUrl(requestUriStr);
    }
}
