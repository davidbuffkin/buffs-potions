package com.buffspotions.mixin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapLevelAccess {
    @Accessor("level")
    ServerLevel buffsPotions$level();

    @Invoker("readChunk")
    CompletableFuture<Optional<CompoundTag>> buffsPotions$readChunk(ChunkPos pos);
}
