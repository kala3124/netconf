/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.streams.listeners;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * {@link Notificator} is responsible to create, remove and find {@link ListenerAdapter} listener.
 */
public class Notificator {

    private static Map<String, ListenerAdapter> listenersByStreamName = new ConcurrentHashMap<>();
    private static Map<YangInstanceIdentifier, ListenerAdapter> listenersByInstanceIdentifier = new ConcurrentHashMap<>();
    private static final Lock lock = new ReentrantLock();

    private Notificator() {
    }

    /**
     * Returns list of all stream names
     */
    public static Set<String> getStreamNames() {
        return listenersByStreamName.keySet();
    }

    /**
     * Gets {@link ListenerAdapter} specified by stream name.
     *
     * @param streamName
     *            The name of the stream.
     * @return {@link ListenerAdapter} specified by stream name.
     */
    public static ListenerAdapter getListenerFor(String streamName) {
        return listenersByStreamName.get(streamName);
    }

    /**
     * Gets {@link ListenerAdapter} listener specified by {@link YangInstanceIdentifier} path.
     *
     * @param path
     *            Path to data in data repository.
     * @return ListenerAdapter
     */
    public static ListenerAdapter getListenerFor(YangInstanceIdentifier path) {
        return listenersByInstanceIdentifier.get(path);
    }

    /**
     * Checks if the listener specified by {@link YangInstanceIdentifier} path exist.
     *
     * @param path
     *            Path to data in data repository.
     * @return True if the listener exist, false otherwise.
     */
    public static boolean existListenerFor(YangInstanceIdentifier path) {
        return listenersByInstanceIdentifier.containsKey(path);
    }

    /**
     * Creates new {@link ListenerAdapter} listener from {@link YangInstanceIdentifier} path and stream name.
     *
     * @param path
     *            Path to data in data repository.
     * @param streamName
     *            The name of the stream.
     * @return New {@link ListenerAdapter} listener from {@link YangInstanceIdentifier} path and stream name.
     */
    public static ListenerAdapter createListener(YangInstanceIdentifier path, String streamName) {
        ListenerAdapter listener = new ListenerAdapter(path, streamName);
        try {
            lock.lock();
            listenersByInstanceIdentifier.put(path, listener);
            listenersByStreamName.put(streamName, listener);
        } finally {
            lock.unlock();
        }
        return listener;
    }

    /**
     * Looks for listener determined by {@link YangInstanceIdentifier} path and removes it.
     *
     * @param path
     *            InstanceIdentifier
     */
    public static void removeListener(YangInstanceIdentifier path) {
        ListenerAdapter listener = listenersByInstanceIdentifier.get(path);
        deleteListener(listener);
    }

    /**
     * Creates String representation of stream name from URI. Removes slash from URI in start and end position.
     *
     * @param uri
     *            URI for creation stream name.
     * @return String representation of stream name.
     */
    public static String createStreamNameFromUri(String uri) {
        if (uri == null) {
            return null;
        }
        String result = uri;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length()-1);
        }
        return result;
    }

    /**
     * Removes all listeners.
     */
    public static void removeAllListeners() {
        for (ListenerAdapter listener : listenersByInstanceIdentifier.values()) {
            try {
                listener.close();
            } catch (Exception e) {
            }
        }
        try {
            lock.lock();
            listenersByStreamName = new ConcurrentHashMap<>();
            listenersByInstanceIdentifier = new ConcurrentHashMap<>();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if listener has at least one subscriber. In case it doesn't have any, delete listener.
     *
     * @param listener
     *            ListenerAdapter
     */
    public static void removeListenerIfNoSubscriberExists(ListenerAdapter listener) {
        if (!listener.hasSubscribers()) {
            deleteListener(listener);
        }
    }

    /**
     * Delete {@link ListenerAdapter} listener specified in parameter.
     *
     * @param listener
     *            ListenerAdapter
     */
    private static void deleteListener(ListenerAdapter listener) {
        if (listener != null) {
            try {
                listener.close();
            } catch (Exception e) {
            }
            try {
                lock.lock();
                listenersByInstanceIdentifier.remove(listener.getPath());
                listenersByStreamName.remove(listener.getStreamName());
            } finally {
                lock.unlock();
            }
        }
    }

}
