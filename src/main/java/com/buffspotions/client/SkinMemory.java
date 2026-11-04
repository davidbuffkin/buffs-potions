package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.mixin.client.TextureManagerAccess;
import com.buffspotions.network.SkinOfferPayload;
import com.buffspotions.network.SkinRememberedPayload;
import com.buffspotions.skin.SkinPixels;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import org.jspecify.annotations.Nullable;

/**
 * The client copy of this world's skin cache. Disguise and player heads check here before any lookup.
 */
@EventBusSubscriber(modid = BuffsPotions.MOD_ID, value = Dist.CLIENT)
public final class SkinMemory {
    private static final Map<String, PlayerSkin> BY_NAME = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerSkin> BY_ID = new ConcurrentHashMap<>();
    private static final Set<String> OFFERED = ConcurrentHashMap.newKeySet();
    private static int tick;

    private SkinMemory() {
    }

    public static @Nullable PlayerSkin skin(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static @Nullable PlayerSkin skin(ResolvableProfile profile) {
        PlayerSkin named = profile.name().map(SkinMemory::skin).orElse(null);
        if (named != null) {
            return named;
        }
        UUID id = profile.partialProfile().id();
        if (id == null || id.equals(Util.NIL_UUID)) {
            return null;
        }
        return BY_ID.get(id);
    }

    public static void accept(SkinRememberedPayload payload) {
        if (!SkinPixels.skin(payload.skin()) || !SkinPixels.extra(payload.cape()) || !SkinPixels.extra(payload.elytra())) {
            return;
        }
        PlayerSkin skin = register(payload.name(), payload.slim(), payload.skin(), payload.cape(), payload.elytra());
        if (skin == null) {
            return;
        }
        keepLocal(payload.name(), payload.id(), skin);
    }

    /** Copies a skin that is already loaded on this client into the world cache. */
    public static void keep(String name, @Nullable UUID id, PlayerSkin skin) {
        if (OFFERED.contains(name.toLowerCase(Locale.ROOT))) {
            keepLocal(name, Optional.ofNullable(id), skin);
            return;
        }
        byte[] body = pixels(skin.body());
        if (!SkinPixels.skin(body)) {
            return;
        }
        byte[] cape = skin.cape() == null ? new byte[0] : pixels(skin.cape());
        byte[] elytra = skin.elytra() == null ? new byte[0] : pixels(skin.elytra());
        if (!SkinPixels.extra(cape) || !SkinPixels.extra(elytra)) {
            cape = new byte[0];
            elytra = new byte[0];
        }
        keepLocal(name, Optional.ofNullable(id), skin);
        OFFERED.add(name.toLowerCase(Locale.ROOT));
        ClientPacketDistributor.sendToServer(new SkinOfferPayload(name, Optional.ofNullable(id), skin.model() == PlayerModelType.SLIM, body, cape, elytra));
    }

    public static void harvest(Minecraft minecraft) {
        if (minecraft.getConnection() == null || ++tick % 20 != 0) {
            return;
        }
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (OFFERED.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            PlayerSkin skin = info.getSkin();
            if (!(skin.body() instanceof ClientAsset.DownloadedTexture)) {
                continue;
            }
            keep(name, info.getProfile().id(), skin);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public static void clear() {
        BY_NAME.clear();
        BY_ID.clear();
        OFFERED.clear();
        tick = 0;
    }

    private static void keepLocal(String name, Optional<UUID> id, PlayerSkin skin) {
        BY_NAME.put(name.toLowerCase(Locale.ROOT), skin);
        id.ifPresent(uuid -> BY_ID.put(uuid, skin));
    }

    private static @Nullable PlayerSkin register(String name, boolean slim, byte[] skin, byte[] cape, byte[] elytra) {
        ClientAsset.Texture body = texture(name, "", skin);
        if (body == null) {
            return null;
        }
        ClientAsset.Texture capeTexture = cape.length == 0 ? null : texture(name, "_cape", cape);
        ClientAsset.Texture elytraTexture = elytra.length == 0 ? null : texture(name, "_elytra", elytra);
        return PlayerSkin.insecure(body, capeTexture, elytraTexture, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE);
    }

    private static ClientAsset.@Nullable Texture texture(String name, String suffix, byte[] packed) {
        SkinPixels.Image image = SkinPixels.unpack(packed);
        if (image == null) {
            return null;
        }
        NativeImage pixels = new NativeImage(image.width(), image.height(), true);
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                pixels.setPixel(x, y, image.argb()[x + y * image.width()]);
            }
        }
        String key = name.toLowerCase(Locale.ROOT);
        Identifier path = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/skin_cache/" + key + suffix + ".png");
        try {
            Minecraft.getInstance().getTextureManager().register(path, new DynamicTexture(() -> key + suffix, pixels));
        } catch (RuntimeException ignored) {
            pixels.close();
            return null;
        }
        Identifier asset = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "skin_cache/" + key + suffix);
        return new ClientAsset.ResourceTexture(asset, path);
    }

    private static byte[] pixels(ClientAsset.Texture texture) {
        try {
            AbstractTexture loaded = ((TextureManagerAccess) Minecraft.getInstance().getTextureManager()).buffsPotions$byPath().get(texture.texturePath());
            if (!(loaded instanceof DynamicTexture dynamic)) {
                return new byte[0];
            }
            NativeImage image = dynamic.getPixels();
            int width = image.getWidth();
            int height = image.getHeight();
            int[] argb = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    argb[x + y * width] = image.getPixel(x, y);
                }
            }
            return SkinPixels.pack(width, height, argb);
        } catch (RuntimeException ignored) {
            return new byte[0];
        }
    }
}
