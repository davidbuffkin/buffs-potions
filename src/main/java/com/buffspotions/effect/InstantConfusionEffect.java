package com.buffspotions.effect;

import com.buffspotions.mixin.EnchantmentMenuSeedAccess;
import com.buffspotions.mixin.PlayerEnchantmentSeedAccess;
import com.buffspotions.registry.ModCriteria;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class InstantConfusionEffect extends InstantaneousMobEffect {
    public InstantConfusionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        randomizeSeed(mob);
        randomizeLook(mob);
        return true;
    }

    @Override
    public void applyInstantaneousEffect(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity owner,
            LivingEntity mob,
            int amplification,
            double scale
    ) {
        if (source instanceof AreaEffectCloud && mob instanceof ServerPlayer player) {
            ModCriteria.CONFUSE_RAY.get().trigger(player);
        }
        randomizeSeed(mob);
        randomizeLook(mob);
    }

    private static void randomizeSeed(LivingEntity mob) {
        if (!(mob instanceof Player player)) {
            return;
        }
        ((PlayerEnchantmentSeedAccess) player).buffsPotions$setEnchantmentSeed(player.getRandom().nextInt());
        if (player.containerMenu instanceof EnchantmentMenu menu) {
            ((EnchantmentMenuSeedAccess) menu).buffsPotions$enchantmentSeed().set(player.getEnchantmentSeed());
            menu.slotsChanged(menu.getSlot(0).container);
        }
    }

    private static void randomizeLook(LivingEntity mob) {
        if (!(mob instanceof ServerPlayer player)) {
            return;
        }
        float yaw = player.getRandom().nextFloat() * 360.0F;
        float pitch = player.getRandom().nextFloat() * 180.0F - 90.0F;
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(pitch);
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.setYBodyRot(yaw);
        Set<Relative> keepPlace = Relative.union(Relative.position(true, true, true), Relative.DELTA);
        player.connection.teleport(new PositionMoveRotation(Vec3.ZERO, Vec3.ZERO, yaw, pitch), keepPlace);
    }
}
