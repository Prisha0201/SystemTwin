package com.systemtwin.events;

@FunctionalInterface
public interface EventConsumer<T> {
    void onEvent(T event);
}
