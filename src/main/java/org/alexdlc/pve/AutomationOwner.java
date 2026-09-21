package org.alexdlc.pve;

public interface AutomationOwner {
    default String automationId() {
        return getClass().getSimpleName();
    }

    default void onAutomationRevoked(PveAutomationCoordinator.RevocationReason reason) {
    }
}
