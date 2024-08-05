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
package org.jkiss.dbeaver.model.net.ssh;

import com.jcraft.jsch.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCInvalidatePhase;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.net.ssh.session.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSessionController<T extends AbstractSession> implements SSHSessionController {
    private static final Log log = Log.getLog(AbstractSessionController.class);

    protected final Map<SSHHostConfiguration, ShareableSession<T>> sessions = new ConcurrentHashMap<>();
    protected AgentIdentityRepository agentIdentityRepository;

    @NotNull
    @Override
    public SSHSession acquireSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHSession origin,
        @Nullable SSHPortForwardConfiguration portForward
    ) throws DBException {
        final AbstractSession session;
        if (origin != null) {
            session = createJumpSession(getDelegateSession(origin), destination, portForward);
        } else {
            session = getOrCreateDirectSession(configuration, destination, portForward);
        }

        session.connect(monitor, destination, configuration);

        return session;
    }

    @NotNull
    public DirectSession<T> getOrCreateDirectSession(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        ShareableSession<T> session = getSharedSession(configuration, destination);
        if (session == null) {
            // Session will be registered during connect
            session = new ShareableSession<>(this, destination);
        }
        return new DirectSession<>(session, portForward);
    }

    @NotNull
    private JumpSession<T> createJumpSession(
        @NotNull DelegateSession origin,
        @NotNull SSHHostConfiguration destination,
        @Nullable SSHPortForwardConfiguration portForward
    ) {
        return new JumpSession<>(this, origin, destination, portForward);
    }

    @Override
    public void release(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        getDelegateSession(session).disconnect(monitor, configuration, timeout);
    }

    @Override
    public void invalidate(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHSession session,
        @NotNull DBCInvalidatePhase phase,
        @NotNull DBWHandlerConfiguration configuration,
        long timeout
    ) throws DBException {
        final DelegateSession delegate = getDelegateSession(session);

        if (phase == DBCInvalidatePhase.BEFORE_INVALIDATE) {
            release(monitor, delegate, configuration, timeout);
        }

        if (phase == DBCInvalidatePhase.INVALIDATE) {
            delegate.connect(monitor, delegate.getDestination(), configuration);
        }
    }

    @NotNull
    @Override
    public SSHSession[] getSessions() {
        return sessions.values().toArray(SSHSession[]::new);
    }

    @NotNull
    @Override
    public DBPDataSourceContainer[] getDependentDataSources(@NotNull SSHSession session) {
        return getDelegateSession(session).getDataSources();
    }

    @NotNull
    protected IdentityRepository createAgentIdentityRepository() throws DBException {
        if (agentIdentityRepository == null) {
            AgentConnector connector = null;

            try {
                connector = new PageantConnector();
                log.debug("SSHSessionController: connected with pageant");
            } catch (Exception e) {
                log.debug("SSHSessionController: pageant connect exception", e);
            }

            if (connector == null) {
                try {
                    connector = new SSHAgentConnector(new JUnixSocketFactory());
                    log.debug("SSHSessionController: Connected with ssh-agent");
                } catch (Exception e) {
                    log.debug("SSHSessionController: ssh-agent connection exception", e);
                }
            }

            if (connector == null) {
                throw new DBException("Unable to initialize SSH agent");
            }

            agentIdentityRepository = new AgentIdentityRepository(connector);
        }

        return agentIdentityRepository;
    }

    @NotNull
    public abstract T createSession();

    @NotNull
    protected DelegateSession getDelegateSession(@NotNull SSHSession session) {
        if (session instanceof DelegateSession delegate) {
            return delegate;
        } else {
            throw new IllegalStateException("Unexpected session type: " + session + " (" + session.getClass().getName() + ")");
        }
    }

    public void registerSession(@NotNull ShareableSession<T> session, @NotNull DBWHandlerConfiguration configuration) {
        if (canShareSessionForConfiguration(configuration)) {
            sessions.put(session.getDestination(), session);
        }
    }

    public void unregisterSession(@NotNull ShareableSession<T> session, @NotNull DBWHandlerConfiguration configuration) {
        if (canShareSessionForConfiguration(configuration)) {
            sessions.remove(session.getDestination());
        }
    }

    @Nullable
    private ShareableSession<T> getSharedSession(
        @NotNull DBWHandlerConfiguration configuration,
        @NotNull SSHHostConfiguration destination
    ) {
        if (canShareSessionForConfiguration(configuration)) {
            return sessions.get(destination);
        } else {
            return null;
        }
    }

    protected static boolean canShareSessionForConfiguration(@NotNull DBWHandlerConfiguration configuration) {
        // Data source might be null if this tunnel is used for connection testing
        return !SSHUtils.DISABLE_SESSION_SHARING
            && configuration.getDataSource() != null
            && configuration.getBooleanProperty(SSHConstants.PROP_SHARE_TUNNELS, true);
    }

}
