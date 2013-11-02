/*
 * Copyright 2008-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.runtime.core.view;

import griffon.core.GriffonApplication;
import griffon.core.view.WindowDisplayHandler;
import griffon.core.view.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 * @since 2.0.0
 */
public abstract class AbstractWindowManager<W> implements WindowManager<W> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractWindowManager.class);
    protected static final String ERROR_NAME_BLANK = "Argument 'name' cannot be blank";
    protected static final String ERROR_WINDOW_NULL = "Argument 'window' cannot be null";
    private final Map<String, W> windows = Collections.synchronizedMap(new LinkedHashMap<String, W>());

    private final GriffonApplication application;
    private final WindowDisplayHandler<W> windowDisplayHandler;

    @Inject
    public AbstractWindowManager(@Nonnull GriffonApplication application, @Nonnull WindowDisplayHandler<W> windowDisplayHandler) {
        this.application = requireNonNull(application, "Argument 'application' cannot be null");
        requireNonNull(application.getApplicationConfiguration(), "Argument 'application.configuration' cannot be null");
        requireNonNull(application.getUIThreadManager(), "Argument 'application.uiThreadManager' cannot be null");
        this.windowDisplayHandler = requireNonNull(windowDisplayHandler, "Argument 'windowDisplayHandler' cannot be null");
    }

    protected GriffonApplication getApplication() {
        return application;
    }


    @Override
    @Nullable
    public W findWindow(@Nonnull String name) {
        requireNonBlank(name, ERROR_NAME_BLANK);
        return windows.get(name);
    }

    @Override
    @Nullable
    public W getAt(int index) {
        synchronized (windows) {
            int size = windows.size();
            if (index < 0 || index >= size) {
                throw new ArrayIndexOutOfBoundsException(index);
            }

            int i = 0;
            for (W window : windows.values()) {
                if (index == i++) {
                    return window;
                }
            }
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    @Nullable
    public W getStartingWindow() {
        W window = null;
        Object value = application.getApplicationConfiguration().get("windowManager.startingWindow", null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("windowManager.startingWindow configured to " + value);
        }

        if (value instanceof String) {
            String windowName = (String) value;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Selecting window " + windowName + " as starting window");
            }
            window = findWindow(windowName);
        } else if (value instanceof Number) {
            int index = ((Number) value).intValue();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Selecting window at index " + index + " as starting window");
            }
            window = getAt(index);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No startingWindow configured, selecting the first one from the windows list");
            }
            window = getAt(0);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting Window is " + window);
        }

        return window;
    }

    @Override
    @Nonnull
    public Collection<W> getWindows() {
        return unmodifiableCollection(windows.values());
    }

    @Override
    public void attach(@Nonnull String name, @Nonnull W window) {
        requireNonBlank(name, ERROR_NAME_BLANK);
        requireNonNull(window, ERROR_WINDOW_NULL);
        if (windows.containsKey(name)) {
            W window2 = windows.get(name);
            if (window2 != window) {
                detach(name);
            }
        }

        doAttach(window);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Attaching window with name: '" + name + "' at index " + windows.size() + " " + window);
        }
        windows.put(name, window);
    }

    protected abstract void doAttach(@Nonnull W window);

    @Override
    public void detach(@Nonnull String name) {
        requireNonBlank(name, ERROR_NAME_BLANK);
        if (windows.containsKey(name)) {
            W window = windows.get(name);

            doDetach(window);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Detaching window with name: '" + name + "' " + window);
            }
            windows.remove(name);
        }
    }

    protected abstract void doDetach(@Nonnull W window);

    @Override
    public void show(@Nonnull final W window) {
        requireNonNull(window, ERROR_WINDOW_NULL);
        if (!windows.containsValue(window)) {
            return;
        }

        String windowName = null;
        int windowIndex = -1;
        synchronized (windows) {
            int i = 0;
            for (Map.Entry<String, W> entry : windows.entrySet()) {
                if (entry.getValue() == window) {
                    windowName = entry.getKey();
                    windowIndex = i++;
                    break;
                }
            }
        }

        final String name = windowName;
        final int index = windowIndex;

        application.getUIThreadManager().runInsideUIAsync(new Runnable() {
            public void run() {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Showing window with name: '" + name + "' at index " + index + " " + window);
                }
                resolveWindowDisplayHandler().show(name, window);
            }
        });
    }

    @Override
    public void show(@Nonnull String name) {
        requireNonBlank(name, ERROR_NAME_BLANK);
        W window = findWindow(name);
        if (window != null) {
            show(window);
        }
    }

    @Override
    public void hide(@Nonnull final W window) {
        requireNonNull(window, ERROR_WINDOW_NULL);
        if (!windows.containsValue(window)) {
            return;
        }

        String windowName = null;
        int windowIndex = -1;
        synchronized (windows) {
            int i = 0;
            for (Map.Entry<String, W> entry : windows.entrySet()) {
                if (entry.getValue() == window) {
                    windowName = entry.getKey();
                    windowIndex = i++;
                    break;
                }
            }
        }

        final String name = windowName;
        final int index = windowIndex;

        application.getUIThreadManager().runInsideUIAsync(new Runnable() {
            public void run() {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Hiding window with name: '" + name + "' at index " + index + " " + window);
                }
                resolveWindowDisplayHandler().hide(name, window);
            }
        });
    }

    @Nonnull
    protected WindowDisplayHandler<W> resolveWindowDisplayHandler() {
        return windowDisplayHandler;
    }

    @Override
    public void hide(@Nonnull String name) {
        requireNonBlank(name, ERROR_NAME_BLANK);
        W window = findWindow(name);
        if (window != null) {
            hide(window);
        }
    }

    @Override
    public boolean canShutdown(@Nonnull GriffonApplication app) {
        return true;
    }

    @Override
    public void onShutdown(@Nonnull GriffonApplication app) {
        for (W window : windows.values()) {
            if (isWindowVisible(window)) hide(window);
        }
    }

    protected abstract boolean isWindowVisible(@Nonnull W window);

    @Override
    public int countVisibleWindows() {
        int visibleWindows = 0;
        for (W window : windows.values()) {
            if (isWindowVisible(window)) {
                visibleWindows++;
            }
        }
        return visibleWindows;
    }

    @Override
    public boolean isAutoShutdown() {
        return application.getApplicationConfiguration().getAsBoolean("application.autoShutdown", true);
    }
}
