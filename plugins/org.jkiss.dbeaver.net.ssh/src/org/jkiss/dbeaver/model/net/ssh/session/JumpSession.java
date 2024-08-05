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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.AbstractSession;
import org.jkiss.dbeaver.model.net.ssh.AbstractSessionController;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;


public class JumpSession<T extends AbstractSession> extends DelegateSession {
    private final AbstractSessionController<T> controller;
    private final DelegateSession origin;
    private SSHPortForwardConfiguration portForward;
    private DelegateSession jumpDestination;
    private SSHPortForwardConfiguration jumpPortForward;
    private boolean registered;

    public JumpSession(
        @NotNull AbstractSessionController<T> controller,
        @NotNull DelegateSession origin,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        super(destination);
        this.controller = controller;
        this.origin = origin;
        this.portForward = portForward;
        this.registered = true;
    }

    @Override
    public void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration host,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
//        if (!registered) {
//            // When opening session for the first time, it will be already connected
//            // When revalidating, it's closed and then must be opened again
//            origin.connect(monitor, origin.destination, configuration);
//            registered = true;
//        }
//
//        jumpPortForward = origin.setupPortForward(new SSHPortForwardConfiguration(
//            SSHConstants.LOCAL_HOST,
//            0,
//            host.hostname(),
//            host.port()
//        ));
//
//        final SSHHostConfiguration jumpHost = new SSHHostConfiguration(
//            host.username(),
//            jumpPortForward.localHost(),
//            jumpPortForward.localPort(),
//            host.auth()
//        );
//
//        jumpDestination = controller.getOrCreateDirectSession(configuration, jumpHost, null);
//        jumpDestination.connect(monitor, jumpHost, configuration);

        AbstractSession session = ((WrapperSession) origin).inner.getSession();
        session.connectVia(monitor, host, configuration);
        SSHPortForwardConfiguration resolved = session.setupPortForward(new SSHPortForwardConfiguration(
            SSHConstants.LOCAL_HOST,
            0,
            host.hostname(),
            host.port()
        ));
        configuration.setProperty("myPort", resolved.localPort());

//        if (portForward != null) {
//            portForward = jumpDestination.setupPortForward(portForward);
//        }
    }

    @Override
    public void disconnect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
            if (portForward != null && jumpDestination!=null) {
                jumpDestination.removePortForward(portForward);
                jumpDestination.disconnect(monitor, configuration, timeout);
            }

//            origin.removePortForward(jumpPortForward);
            origin.disconnect(monitor, configuration, timeout);
//        if (portForward != null) {
//            jumpDestination.removePortForward(portForward);
//        }
//
//        jumpDestination.disconnect(monitor, configuration, timeout);
//        origin.removePortForward(jumpPortForward);

        registered = false;
        jumpDestination = null;
        jumpPortForward = null;
    }

    @NotNull
    @Override
    protected AbstractSession getSession() {
        return origin;
        //return jumpDestination;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer[] getDataSources() {
        return origin.getDataSources();
    }
}

