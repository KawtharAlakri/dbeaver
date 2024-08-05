/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.net.ssh.session;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.AbstractSession;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class DelegateSession extends AbstractSession {

    private static final Log log = Log.getLog(DelegateSession.class);

    protected final SSHHostConfiguration destination;

    public DelegateSession(@NotNull SSHHostConfiguration destination) {
        this.destination = destination;
    }

    @Override
    public void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration destination,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        log.debug("SSHSessionController: Connecting session to " + destination);
        getSession().connect(monitor, destination, configuration);
    }

    @Override
    public void disconnect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        log.debug("SSHSessionController: Disconnecting session to " + destination);
        getSession().disconnect(monitor, configuration, timeout);
    }

    @NotNull
    @Override
    public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        log.debug("SSHSessionController: Set up port forwarding " + configuration);
        return getSession().setupPortForward(configuration);
    }

    @Override
    public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        log.debug("SSHSessionController: Remove port forwarding " + configuration);
        getSession().removePortForward(configuration);
    }

    @Override
    public void getFile(
        @NotNull String src,
        @NotNull OutputStream dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException {
        getSession().getFile(src, dst, monitor);
    }

    @Override
    public void putFile(
        @NotNull InputStream src,
        @NotNull String dst,
        @NotNull DBRProgressMonitor monitor
    ) throws DBException, IOException {
        getSession().putFile(src, dst, monitor);
    }

    @NotNull
    @Override
    public String getClientVersion() {
        return getSession().getClientVersion();
    }

    @NotNull
    @Override
    public String getServerVersion() {
        return getSession().getServerVersion();
    }

    @NotNull
    protected abstract AbstractSession getSession();

    @NotNull
    public abstract DBPDataSourceContainer[] getDataSources();

    public SSHHostConfiguration getDestination() {
        return destination;
    }
}
