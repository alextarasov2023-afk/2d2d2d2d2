package org.alexdlc.utils.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.PiercingWeapon;
import org.alexdlc.feature.impl.visual.HoldMyItemsCompat;
import org.alexdlc.context.PlayerContext;

public class AttackWindow implements PlayerContext {

    public static void attack(Entity target) {
        if (mc.gameMode != null && mc.player != null) {
            HoldMyItemsCompat.beginMainHandAttack(mc.player);

            PiercingWeapon piercing = mc.player.getWeaponItem().get(DataComponents.PIERCING_WEAPON);
            if (piercing != null) {
                mc.gameMode.piercingAttack(piercing);
            } else {
                mc.gameMode.attack(mc.player, target);
            }
            mc.player.swing(InteractionHand.MAIN_HAND);

            SprintManager.onAttack();
        }
    }
}
