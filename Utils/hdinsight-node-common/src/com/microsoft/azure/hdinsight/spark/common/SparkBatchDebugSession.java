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

package com.microsoft.azure.hdinsight.spark.common;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import rx.Subscription;

import java.io.File;
import java.net.UnknownServiceException;
import java.util.Arrays;

/*
 * Spark Batch Job debug session with SSH tunnel
 */
public class SparkBatchDebugSession implements ILogger{
    private Session portForwardingSession;
    private JSch jsch;
    private Subscription logSubscription;

    SparkBatchDebugSession(JSch jsch, Session portForwardingSession) {
        this.jsch = jsch;
        this.portForwardingSession = portForwardingSession;
    }

    /**
     * Getter of the log subscription
     *
     * @return Subscription instance
     */
    public Subscription getLogSubscription() {
        return logSubscription;
    }

    /**
     * Setter of the log subscription
     *
     * @param logSubscription log Rx Subscription instance
     */
    public void setLogSubscription(Subscription logSubscription) {
        this.logSubscription = logSubscription;
    }

    /**
     * Getter of the jsch instance
     *
     * @return jsch instance
     */
    public JSch getJsch() {
        return jsch;
    }

    /**
     * Getter of the port forwarding session instance
     *
     * @return portForwardingSession instance
     */
    public Session getPortForwardingSession() {
        return portForwardingSession;
    }

    /**
     * Specify the private key file for establish SSH connection
     *
     * @param file The private key file
     * @return the current instance for chain calling
     * @throws JSchException JSch operation exceptions
     */
    public SparkBatchDebugSession setPrivateKeyFile(File file) throws JSchException {
        this.getJsch().addIdentity(file.getPath());

        return this;
    }

    /**
     * Specify the password for SSH connection user
     *
     * @param password password to set
     * @return the current instance for chain calling
     */
    public SparkBatchDebugSession setPassword(String password) {
        this.getPortForwardingSession().setPassword(password);

        return this;
    }

    /**
     * Close the SSH port forwarding session
     *
     * @return the current instance for chain calling
     */
    public SparkBatchDebugSession close() {
        if (getLogSubscription() != null) {
            getLogSubscription().unsubscribe();
        }

        this.getPortForwardingSession().disconnect();

        return this;
    }

    /**
     * Open the SSH port forwarding session
     *
     * @return the current instance for chain calling
     * @throws JSchException JSch operation exceptions
     */
    public SparkBatchDebugSession open() throws JSchException {
        this.getPortForwardingSession().connect();

        return this;
    }

    /**
     * Create an ephemeral Forward to remote host and port
     *
     * @param remoteHost the remote host in SSH server network
     * @param remotePort the remote port of host
     * @return the current instance for chain calling
     * @throws JSchException JSch operation exceptions
     */
    public SparkBatchDebugSession forwardToRemotePort(String remoteHost, int remotePort) throws JSchException {
        // 0 means to select the local automatically
        this.getPortForwardingSession().setPortForwardingL(0, remoteHost, remotePort);

        return this;
    }

    /**
     * Get the local ephemeral port to be forward by remote host and port
     *
     * @param remoteHost the remote host in SSH server network
     * @param remotePort the remote port of host
     * @return the local ephemeral port
     * @throws JSchException JSch operation exceptions
     * @throws UnknownServiceException if no local port found
     */
    public int getForwardedLocalPort(String remoteHost, int remotePort)
            throws JSchException,
                   UnknownServiceException {
        String localPort = Arrays.stream(this.getPortForwardingSession().getPortForwardingL())
                .filter((forwarding) -> forwarding.matches("\\d+:" + remoteHost + ":" + remotePort))
                .findFirst()
                .map((forwarding) -> forwarding.split(":")[0])
                .orElseThrow(() -> new UnknownServiceException(
                        "No local forwarded port found for " + remoteHost + ":" + remotePort));

        return Integer.parseInt(localPort);
    }

    /**
     * Create a SparkBatchDebugSession instance for specified host and user
     *
     * @param host The SSH host
     * @param user The SSH user
     * @return an SparkBatchDebugSession instance
     * @throws JSchException JSch operation exceptions
     */
    static public SparkBatchDebugSession factory(String host, String user) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host);

        java.util.Properties config = new java.util.Properties() {{
            put("StrictHostKeyChecking", "no");
        }};

        session.setConfig(config);

        return new SparkBatchDebugSession(jsch, session);
    }
}
