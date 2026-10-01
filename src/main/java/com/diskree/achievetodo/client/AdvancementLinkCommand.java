package com.diskree.achievetodo.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.OpenUrl;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.BiFunction;

public final class AdvancementLinkCommand {
    public enum Result { OPENED, ABSENT, UNAVAILABLE }

    public interface Navigation {
        boolean installed();
        boolean highlight(Identifier advancementId);
    }

    private AdvancementLinkCommand() {}

    public static Result navigate(String argument, Navigation navigation, Consumer<Result> failure) {
        Identifier id = Identifier.tryParse(argument);
        Result result = id == null ? Result.UNAVAILABLE
            : !navigation.installed() ? Result.ABSENT
            : navigation.highlight(id) ? Result.OPENED : Result.UNAVAILABLE;
        if (result != Result.OPENED) failure.accept(result);
        return result;
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        register(dispatcher, AdvancementLinkCommand::execute);
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
        BiFunction<FabricClientCommandSource, String, Integer> action) {
        dispatcher.register(ClientCommands.literal("advancementssearch")
            .then(ClientCommands.literal("highlight")
                .then(ClientCommands.argument("advancement", IdentifierArgument.id())
                    .executes(context -> action.apply(context.getSource(), context.getArgument("advancement", Identifier.class).toString()))
                    .then(ClientCommands.literal("obtained_status")
                        .executes(context -> action.apply(context.getSource(), context.getArgument("advancement", Identifier.class).toString()))))));
    }

    private static int execute(FabricClientCommandSource source, String argument) {
        Result result = navigate(argument, new Navigation() {
            public boolean installed() {
                return FabricLoader.getInstance().isModLoaded(OptionalAdvancementSearch.MOD_ID);
            }

            public boolean highlight(Identifier id) {
                Minecraft client = source.getClient();
                if (client.player == null || AchieveToDoClient.isNotReady()) return false;
                var advancements = client.player.connection.getAdvancements();
                var node = advancements.getTree().get(id);
                if (node == null) return false;
                var screen = new AdvancementsScreen(advancements);
                client.gui.setScreen(screen);
                if (client.gui.screen() != screen || screen.getAdvancementWidget(node) == null) return false;
                advancements.setSelectedTab(node.root().holder(), true);
                return OptionalAdvancementSearch.requestHighlight(screen, id);
            }
        }, failure -> {
            if (failure == Result.ABSENT) {
                source.sendFeedback(AchieveToDoClient.translate("suggest_install_advancements_search_mod")
                    .append(Component.literal("AdvancementsSearch").withStyle(style -> style
                        .withClickEvent(new OpenUrl(URI.create("https://modrinth.com/mod/advancementssearch")))
                        .withUnderlined(true).withColor(ChatFormatting.GOLD))));
            } else {
                source.sendError(AchieveToDoClient.translate("advancement_navigation_unavailable"));
            }
        });
        // A recognized client command is always consumed; no server parse/fallback follows.
        return result == Result.OPENED ? 1 : 0;
    }
}
