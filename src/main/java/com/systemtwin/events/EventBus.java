package com.systemtwin.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * High-performance, concurrent EventBus supporting typed publish/subscribe.
 * Handles both synchronous distribution and background asynchronous dispatch.
 */
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final Map<Class<?>, Set<EventConsumer<?>>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "systemtwin-event-bus-worker");
        t.setDaemon(true);
        return t;
    });

    public EventBus() {}

    /**
     * Subscribe a consumer to events of class eventType or subclasses.
     */
    public <T> void subscribe(Class<T> eventType, EventConsumer<T> consumer) {
        if (eventType == null || consumer == null) return;
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(consumer);
    }

    /**
     * Unsubscribe a consumer.
     */
    public <T> void unsubscribe(Class<T> eventType, EventConsumer<T> consumer) {
        if (eventType == null || consumer == null) return;
        Set<EventConsumer<?>> set = listeners.get(eventType);
        if (set != null) {
            set.remove(consumer);
        }
    }

    /**
     * Synchronously dispatch event to all matching subscribers.
     */
    @SuppressWarnings("unchecked")
    public void publish(Object event) {
        if (event == null) return;
        Class<?> eventClass = event.getClass();

        for (Map.Entry<Class<?>, Set<EventConsumer<?>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventClass)) {
                for (EventConsumer<?> consumer : entry.getValue()) {
                    try {
                        ((EventConsumer<Object>) consumer).onEvent(event);
                    } catch (Exception ex) {
                        log.error("Error processing event in consumer: {}", ex.getMessage(), ex);
                    }
                }
            }
        }
    }

    /**
     * Asynchronously dispatch event on worker thread pool.
     */
    public void publishAsync(Object event) {
        if (event == null) return;
        asyncExecutor.submit(() -> publish(event));
    }

    public void clear() {
        listeners.clear();
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }
}
