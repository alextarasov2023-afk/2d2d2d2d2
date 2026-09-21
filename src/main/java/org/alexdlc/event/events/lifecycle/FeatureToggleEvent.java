package org.alexdlc.event.events.lifecycle;

import lombok.Getter;
import org.alexdlc.event.Event;
import org.alexdlc.feature.Feature;

@Getter
public final class FeatureToggleEvent extends Event {
    private final Feature feature;
    private final boolean enabled;

    public FeatureToggleEvent(Feature feature, boolean enabled) {
        this.feature = feature;
        this.enabled = enabled;
    }

    public boolean isDisabled() {
        return !this.enabled;
    }
}
