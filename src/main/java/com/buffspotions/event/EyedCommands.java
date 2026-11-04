package com.buffspotions.event;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.effect.EyedDisplay;
import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.ParameterDoses;
import com.buffspotions.registry.ModEffects;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class EyedCommands {
    private static final SimpleCommandExceptionType NOT_PARAMETERIZED = new SimpleCommandExceptionType(
            Component.translatable("commands.buffs_potions.effect.not_parameterized")
    );
    private static final SimpleCommandExceptionType GIVE_FAILED = new SimpleCommandExceptionType(
            Component.translatable("commands.effect.give.failed")
    );
    private static final SimpleCommandExceptionType NOT_BLOCK = new SimpleCommandExceptionType(
            Component.translatable("commands.buffs_potions.effect.not_block")
    );

    private EyedCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var context = event.getBuildContext();
        event.getDispatcher().register(
                Commands.literal("effect")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("give")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT))
                                                .then(Commands.literal("block")
                                                        .then(subject(context)))
                                                .then(Commands.literal("item")
                                                        .then(subject(context)))
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                                .then(Commands.argument("hideParticles", BoolArgumentType.bool())
                                                                        .then(Commands.argument("block", ResourceArgument.resource(context, Registries.ITEM))
                                                                                .executes(EyedCommands::giveFull))))))))
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> subject(net.minecraft.commands.CommandBuildContext context) {
        return duration(Commands.argument("block", ResourceArgument.resource(context, Registries.ITEM)).executes(EyedCommands::giveDefault));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> duration(ArgumentBuilder<CommandSourceStack, ?> block) {
        return block.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                .executes(EyedCommands::giveSeconds)
                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                        .executes(EyedCommands::giveAmplifier)
                        .then(Commands.argument("hideParticles", BoolArgumentType.bool())
                                .executes(EyedCommands::giveFull))));
    }

    private static int giveDefault(CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        return give(command, null, 0, false);
    }

    private static int giveSeconds(CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        return give(command, IntegerArgumentType.getInteger(command, "seconds"), 0, false);
    }

    private static int giveAmplifier(CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        return give(
                command,
                IntegerArgumentType.getInteger(command, "seconds"),
                IntegerArgumentType.getInteger(command, "amplifier"),
                false
        );
    }

    private static int giveFull(CommandContext<CommandSourceStack> command) throws CommandSyntaxException {
        return give(
                command,
                IntegerArgumentType.getInteger(command, "seconds"),
                IntegerArgumentType.getInteger(command, "amplifier"),
                BoolArgumentType.getBool(command, "hideParticles")
        );
    }

    private static int give(CommandContext<CommandSourceStack> command, Integer seconds, int amplifier, boolean hideParticles)
            throws CommandSyntaxException {
        return give(
                command.getSource(),
                EntityArgument.getEntities(command, "targets"),
                ResourceArgument.getMobEffect(command, "effect"),
                ResourceArgument.getResource(command, "block", Registries.ITEM),
                seconds,
                amplifier,
                hideParticles
        );
    }

    private static int give(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            Holder<MobEffect> effect,
            Holder<Item> item,
            Integer seconds,
            int amplifier,
            boolean hideParticles
    ) throws CommandSyntaxException {
        boolean sense = effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey());
        boolean eater = effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey());
        boolean vomiting = effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey());
        boolean seeking = effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey());
        boolean incarnation = effect.unwrapKey().equals(ModEffects.INSTANT_INCARNATION.unwrapKey());
        boolean formation = effect.unwrapKey().equals(ModEffects.INSTANT_FORMATION.unwrapKey());
        if (!sense && !eater && !vomiting && !seeking && !incarnation && !formation) {
            throw NOT_PARAMETERIZED.create();
        }
        if ((sense || formation) && !(item.value() instanceof BlockItem)) {
            throw NOT_BLOCK.create();
        }
        int duration = seconds == null ? 600 : seconds * 20;
        int count = 0;
        EssenceSubject subject = EssenceSubject.ofItem(item);
        for (Entity target : targets) {
            Eyedentification.beginSubject(subject);
            boolean applied;
            try {
                applied = target instanceof LivingEntity living
                        && living.addEffect(new MobEffectInstance(effect, duration, amplifier, false, !hideParticles), source.getEntity());
            } finally {
                Eyedentification.endApply();
            }
            if (!applied && target instanceof Player player && ParameterDoses.has(player, effect, subject)) {
                applied = true;
            }
            if (applied) {
                count++;
            }
        }
        if (count == 0) {
            throw GIVE_FAILED.create();
        }
        int reported = duration / 20;
        Component effectName = appliedName(effect, subject, amplifier);
        if (targets.size() == 1) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.effect.give.success.single",
                            effectName,
                            targets.iterator().next().getDisplayName(),
                            reported
                    ),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.effect.give.success.multiple",
                            effectName,
                            targets.size(),
                            reported
                    ),
                    true
            );
        }
        return count;
    }

    private static Component appliedName(Holder<MobEffect> effect, EssenceSubject subject, int amplifier) {
        EyedDisplay.TOOLTIP.set(subject);
        try {
            Component named = EyedDisplay.effectLine(effect, amplifier);
            return named != null ? named : effect.value().getDisplayName();
        } finally {
            EyedDisplay.TOOLTIP.remove();
        }
    }
}
