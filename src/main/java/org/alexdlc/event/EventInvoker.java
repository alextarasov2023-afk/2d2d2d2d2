package org.alexdlc.event;

@FunctionalInterface
public interface EventInvoker {
    void invoke(Object target, Event event);
}
