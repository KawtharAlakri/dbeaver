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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.AbstractSession;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class DirectSession<T extends AbstractSession> extends WrapperSession<T> {
    private SSHPortForwardConfiguration portForward;

    public DirectSession(
        @NotNull ShareableSession<T> inner,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        super(inner);
        this.portForward = portForward;
    }

    @Override
    public synchronized void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration destination,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        super.connect(monitor, destination, configuration);

        if (portForward != null) {
            portForward = super.setupPortForward(portForward);
        }
    }

    @Override
    public synchronized void disconnect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        if (portForward != null) {
            super.removePortForward(portForward);
        }

        super.disconnect(monitor, configuration, timeout);
    }
}

