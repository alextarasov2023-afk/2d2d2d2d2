package org.alexdlc.feature.impl.visual;

import org.alexdlc.feature.Feature;
import org.alexdlc.feature.FeatureCategory;
import org.alexdlc.feature.setting.BindSetting;

public final class ItemPhysicsFeature extends Feature {
    public ItemPhysicsFeature() {
        super("ItemPhysics", "Dropped items lie on the ground", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }
}
