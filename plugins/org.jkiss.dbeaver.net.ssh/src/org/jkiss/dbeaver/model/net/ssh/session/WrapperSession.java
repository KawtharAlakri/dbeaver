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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.AbstractSession;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class WrapperSession<T extends AbstractSession> extends DelegateSession {

    protected final ShareableSession<T> inner;

    public WrapperSession(@NotNull ShareableSession<T> inner) {
        super(inner.destination);
        this.inner = inner;
    }

    @Override
    public void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration destination,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        inner.connect(monitor, destination, configuration);
    }

    @Override
    public void disconnect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        inner.disconnect(monitor, configuration, timeout);
    }

    @NotNull
    @Override
    public SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        return inner.setupPortForward(configuration);
    }

    @Override
    public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        inner.removePortForward(configuration);
    }

    @NotNull
    @Override
    public AbstractSession getSession() {
        return inner;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer[] getDataSources() {
        return inner.getDataSources();
    }

    public ShareableSession<T> getInner() {
        return inner;
    }
}
