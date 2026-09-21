package org.alexdlc.feature;

import com.google.gson.JsonObject;
import org.alexdlc.feature.impl.pve.PveManagerFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PveManagerConfigMigrationTest {
    private final PveManagerFeature manager = PveManagerFeature.INSTANCE;

    @BeforeEach
    @AfterEach
    void resetManager() {
        this.manager.rotate.reset();
        this.manager.serverProfile.reset();
        this.manager.homeName.reset();
        this.manager.clanName.reset();
        this.manager.anarchy.reset();
        this.manager.minimumHealth.reset();
        this.manager.minimumToolDurability.reset();
        this.manager.pauseNearPlayers.reset();
        this.manager.playerRadius.reset();
        this.manager.debugLogging.reset();
        this.manager.currentState.reset();
    }

    @Test
    void migratesLegacySharedSettingsAndUsesSaferThresholds() {
        JsonObject features = new JsonObject();
        addSetting(features, "Nuker", "Rotate", false);
        addSetting(features, "BaseFinder", "Server Mode", "Auto");
        addSetting(features, "MineHelper", "Server Mode", "FunTime");
        addSetting(features, "AppleFarmer", "Home Name", "farm");
        addSetting(features, "BaseFinder", "Anarchy", 404.0D);
        addSetting(features, "AutoMine", "Minimum Health", 9.0D);
        addSetting(features, "BaseFinder", "Minimum Health", 14.0D);
        addSetting(features, "AutoMine", "Minimum Pickaxe Durability", 10.0D);
        addSetting(features, "MineHelper", "Pickaxe Guard", 25.0D);
        addSetting(features, "AutoMine", "Pause Near Players", false);
        addSetting(features, "AutoMine", "Avoid Players", 8.0D);
        addSetting(features, "BaseFinder", "Avoid Players", 24.0D);
        addSetting(features, "AutoMine", "Debug Logging", false);

        assertTrue(PveManagerConfigMigration.migrate(features, this.manager));
        assertFalse(this.manager.rotate.getValue());
        assertEquals(PveManagerFeature.SERVER_FUNTIME,
                this.manager.serverProfile.getValue());
        assertEquals("farm", this.manager.resolvedHomeName());
        assertEquals(404, this.manager.resolvedAnarchy());
        assertEquals(14.0D, this.manager.minimumHealth.getValue());
        assertEquals(25.0D, this.manager.minimumToolDurability.getValue());
        assertTrue(this.manager.pauseNearPlayers.getValue());
        assertEquals(24.0D, this.manager.playerRadius.getValue());
        assertFalse(this.manager.debugLogging.getValue());
    }

    @Test
    void doesNotOverwriteAnExistingPveManagerSection() {
        JsonObject features = new JsonObject();
        features.add(this.manager.getName(), new JsonObject());
        addSetting(features, "AppleFarmer", "Home Name", "legacy");
        this.manager.homeName.setValue("current");

        assertFalse(PveManagerConfigMigration.migrate(features, this.manager));
        assertEquals("current", this.manager.resolvedHomeName());
    }

    private static void addSetting(JsonObject features,
                                   String featureName,
                                   String settingName,
                                   Object value) {
        JsonObject feature = features.has(featureName)
                ? features.getAsJsonObject(featureName)
                : new JsonObject();
        JsonObject settings = feature.has("settings")
                ? feature.getAsJsonObject("settings")
                : new JsonObject();
        if (value instanceof Boolean booleanValue) {
            settings.addProperty(settingName, booleanValue);
        } else if (value instanceof Number numberValue) {
            settings.addProperty(settingName, numberValue);
        } else {
            settings.addProperty(settingName, String.valueOf(value));
        }
        feature.add("settings", settings);
        features.add(featureName, feature);
    }
}
