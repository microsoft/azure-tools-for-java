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
package com.microsoft.azuretools.container;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import com.microsoft.tooling.msservices.components.DefaultLoader;

public class ConsoleLogger {
    final MessageConsole console;
    final MessageConsoleStream out;
    final MessageConsoleStream err;

    private ConsoleLogger() {
        console = findConsole(Constant.CONSOLE_NAME);
        out = console.newMessageStream();
        err = console.newMessageStream();
        out.setColor(new Color(null, 0, 0, 0));
        err.setColor(new Color(null, 255, 0, 0));
        console.addPatternMatchListener(new IPatternMatchListener() {
            @Override
            public void connect(TextConsole arg0) {
            }

            @Override
            public void disconnect() {
            }

            @Override
            public void matchFound(PatternMatchEvent event) {
                try {
                    String urlText = console.getDocument().get(event.getOffset(), event.getLength());
                    IHyperlink hyperlink = new IHyperlink() {
                        @Override
                        public void linkActivated() {
                            Program.launch(urlText);
                        }

                        @Override
                        public void linkEntered() {
                        }

                        @Override
                        public void linkExited() {
                        }
                    };
                    console.addHyperlink(hyperlink, event.getOffset(), event.getLength());
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }

            @Override
            public int getCompilerFlags() {
                return 0;
            }

            @Override
            public String getLineQualifier() {
                return null;
            }

            @Override
            public String getPattern() {
                return "https?://\\S+";
            }
        });
    }

    private static class LazyHolder {
        static final ConsoleLogger INSTANCE = new ConsoleLogger();
    }

    public static ConsoleLogger getInstance() {
        return LazyHolder.INSTANCE;
    }

    public static void info(String infoMsg) {
        LazyHolder.INSTANCE.out.println(infoMsg);
        LazyHolder.INSTANCE.showConsole();
    }

    public static void error(String errorMsg) {
        LazyHolder.INSTANCE.err.println(errorMsg);
        LazyHolder.INSTANCE.showConsole();
    }

    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();

        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            System.out.println(existing[i].getType());
            if (name.equals(existing[i].getName()) && existing[i] instanceof MessageConsole) {
                return (MessageConsole) existing[i];
            }
        }
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] {myConsole});
        return myConsole;
    }

    private void showConsole() {
        DefaultLoader.getIdeHelper().invokeAndWait(() -> {
            IConsole myConsole = this.console;
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    String id = IConsoleConstants.ID_CONSOLE_VIEW;
                    IConsoleView view;
                    try {
                        view = (IConsoleView) page.showView(id);
                        view.display(myConsole);
                    } catch (PartInitException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}
