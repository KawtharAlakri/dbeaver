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
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.AbstractSession;
import org.jkiss.dbeaver.model.net.ssh.AbstractSessionController;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHPortForwardConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ShareableSession<T extends AbstractSession> extends DelegateSession {

    private static final Log log = Log.getLog(ShareableSession.class);

    protected record PortForwardInfo(@NotNull SSHPortForwardConfiguration resolved, @NotNull AtomicInteger usages) {
    }

    protected final Map<DBPDataSourceContainer, AtomicInteger> dataSources = new HashMap<>();
    protected final Map<SSHPortForwardConfiguration, PortForwardInfo> portForwards = new HashMap<>();
    protected final AbstractSessionController<T> controller;
    protected final T session;

    public ShareableSession(@NotNull AbstractSessionController<T> controller, @NotNull SSHHostConfiguration destination) {
        super(destination);
        this.controller = controller;
        this.session = controller.createSession();
    }

    @Property(viewable = true, order = 1, name = "Destination")
    public String getDestinationInfo() {
        return destination.toDisplayString();
    }

    @Property(viewable = true, order = 2, name = "Used By")
    public String getConsumerInfo() {
        return dataSources.entrySet().stream()
            .map(entry -> "%s (%s)".formatted(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", "));
    }

    @Property(viewable = true, order = 3, name = "Port Forwards")
    public String getPortForwardingInfo() {
        return portForwards.values().stream()
            .map(info -> "%s (%d)".formatted(info.resolved.toDisplayString(), info.usages.get()))
            .collect(Collectors.joining(", "));
    }

    @Override
    public synchronized void connect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SSHHostConfiguration destination,
        @NotNull DBWHandlerConfiguration configuration
    ) throws DBException {
        if (dataSources.isEmpty()) {
            log.debug("SSHSessionController: Creating new session to " + destination);
            super.connect(monitor, destination, configuration);
            controller.registerSession(this, configuration);
        }
        final DBPDataSourceContainer container = configuration.getDataSource();
        final AtomicInteger counter = dataSources.get(container);
        if (counter == null) {
            dataSources.put(container, new AtomicInteger(1));
        } else {
            log.debug("SSHSessionController: Reusing session to " + destination + " for " + container);
            counter.incrementAndGet();
        }
    }

    @Override
    public synchronized void disconnect(@NotNull DBRProgressMonitor monitor, @NotNull DBWHandlerConfiguration configuration, long timeout) throws DBException {
        final DBPDataSourceContainer container = configuration.getDataSource();
        final AtomicInteger counter = dataSources.get(container);
        if (counter == null) {
            throw new DBException("Session is not acquired for " + container);
        }
        if (counter.decrementAndGet() == 0) {
            log.debug("SSHSessionController: Releasing session for " + container);
            dataSources.remove(container);
        }
        if (dataSources.isEmpty()) {
            controller.unregisterSession(this, configuration);
            super.disconnect(monitor, configuration, timeout);
        }
    }

    @NotNull
    @Override
    public synchronized SSHPortForwardConfiguration setupPortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        final PortForwardInfo info = portForwards.get(configuration);
        if (info != null) {
            log.debug("SSHSessionController: Reusing port forward " + configuration);
            info.usages.incrementAndGet();
            return info.resolved;
        } else {
            final SSHPortForwardConfiguration resolved = super.setupPortForward(configuration);
            portForwards.put(resolved, new PortForwardInfo(resolved, new AtomicInteger(1)));
            return resolved;
        }
    }

    @Override
    public void removePortForward(@NotNull SSHPortForwardConfiguration configuration) throws DBException {
        final PortForwardInfo info = portForwards.get(configuration);
        if (info == null) {
            throw new DBException("Port forward is not set up: " + configuration);
        }
        if (info.usages.decrementAndGet() == 0) {
            super.removePortForward(info.resolved);
            portForwards.remove(configuration);
        }
    }

    @NotNull
    @Override
    public T getSession() {
        return session;
    }

    @NotNull
    @Override
    public DBPDataSourceContainer[] getDataSources() {
        return dataSources.keySet().toArray(new DBPDataSourceContainer[0]);
    }
}
