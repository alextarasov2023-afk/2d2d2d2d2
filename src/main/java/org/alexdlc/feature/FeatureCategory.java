package org.alexdlc.feature;

import lombok.Getter;

@Getter
public enum FeatureCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visual"),
    PLAYER("Player"),
    MISC("Misc"),
    PVE("PVE");

    private final String displayName;

    FeatureCategory(String displayName) {
        this.displayName = displayName;
    }
}
