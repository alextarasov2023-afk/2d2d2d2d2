package org.alexdlc.feature.impl.pve;

import org.alexdlc.pve.navigation.NavigationOptions;
import org.alexdlc.pve.server.ServerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PveManagerFeatureTest {
    private final PveManagerFeature manager = PveManagerFeature.INSTANCE;

    @AfterEach
    void resetSettings() {
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
    void resolvesEveryManualServerProfile() {
        this.manager.serverProfile.setValue(PveManagerFeature.SERVER_GENERIC);
        assertEquals(ServerProfile.GENERIC, this.manager.resolveServerProfile(null));

        this.manager.serverProfile.setValue(PveManagerFeature.SERVER_FUNTIME);
        assertEquals(ServerProfile.FUNTIME, this.manager.resolveServerProfile(null));

        this.manager.serverProfile.setValue(PveManagerFeature.SERVER_HOLYWORLD);
        assertEquals(ServerProfile.HOLYWORLD, this.manager.resolveServerProfile(null));

        this.manager.serverProfile.setValue(PveManagerFeature.SERVER_REALLYWORLD);
        assertEquals(ServerProfile.REALLYWORLD, this.manager.resolveServerProfile(null));
    }

    @Test
    void appliesGlobalRotationToNavigationProfiles() {
        this.manager.rotate.setValue(false);
        NavigationOptions hiddenRotation = this.manager.configureNavigation(
                NavigationOptions.breakingOnly()
        );
        assertFalse(hiddenRotation.rotateView());

        this.manager.rotate.setValue(true);
        NavigationOptions visibleRotation = this.manager.configureNavigation(
                NavigationOptions.walking()
        );
        assertTrue(visibleRotation.rotateView());
    }

    @Test
    void trimsSharedIdentityValues() {
        this.manager.homeName.setValue("  farm  ");
        this.manager.clanName.setValue("  alexdlc  ");
        this.manager.anarchy.setValue(404.0D);

        assertEquals("farm", this.manager.resolvedHomeName());
        assertEquals("alexdlc", this.manager.resolvedClanName());
        assertEquals(404, this.manager.resolvedAnarchy());
    }

    @Test
    void isSettingsOnlyAndCannotBeToggledOrBound() {
        assertFalse(this.manager.isToggleable());
        assertFalse(this.manager.supportsBinds());
        assertFalse(this.manager.currentState.getValue());

        this.manager.setEnabled(true);
        this.manager.setKeyBind(65);

        assertFalse(this.manager.isEnabled());
        assertTrue(this.manager.getBinds().isEmpty());
        assertEquals(-1, this.manager.addKeyBind(66));
    }
}
