package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.client.Utils;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent.OpenUrl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Mixin(Commands.class)
public class CommandManagerMixin {

    @Inject(
        method = "finishParsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/CommandSourceStack;sendFailure(Lnet/minecraft/network/chat/Component;)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    private static void suggestInstallAdvancementsSearchMod(
        ParseResults<CommandSourceStack> parseResults,
        @NotNull String command,
        @NotNull CommandSourceStack source,
        @NotNull CallbackInfoReturnable<ContextChain<CommandSourceStack>> cir,
        @Local @NotNull CommandSyntaxException commandSyntaxException
    ) {
        String advancementsSearchModName = "AdvancementsSearch";
        if (command.startsWith(advancementsSearchModName.toLowerCase(Locale.ROOT) + " ")) {
            ServerPlayer player = source.getPlayer();
            if (player != null) {
                player.sendSystemMessage(
                    AchieveToDoClient.translate("suggest_install_advancements_search_mod")
                        .append(Component.literal(advancementsSearchModName).withStyle(style -> style
                            .withClickEvent(new OpenUrl(URI.create(Utils.buildModrinthModUrl(advancementsSearchModName))))
                            .withUnderlined(true)
                            .withColor(ChatFormatting.GOLD)
                        ))
                );
            }
            cir.setReturnValue(null);
        }
    }
}
