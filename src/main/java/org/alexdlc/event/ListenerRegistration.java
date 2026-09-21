package org.alexdlc.event;

record ListenerRegistration(
        Class<? extends Event> eventType,
        RegisteredListener listener
) {
}
