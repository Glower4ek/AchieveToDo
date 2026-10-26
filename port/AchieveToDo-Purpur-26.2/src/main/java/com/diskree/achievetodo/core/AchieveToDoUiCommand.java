package com.diskree.achievetodo.core;

import com.diskree.achievetodo.ui.UiTextService;
import com.diskree.achievetodo.ui.bukkit.BukkitUiRuntime;
import com.diskree.achievetodo.ui.bukkit.UiPresentationMode;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class AchieveToDoUiCommand implements BasicCommand {

    private final BukkitUiRuntime uiRuntime;
    private final UiTextService textService = new UiTextService();

    AchieveToDoUiCommand(BukkitUiRuntime uiRuntime) {
        this.uiRuntime = uiRuntime;
    }

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        CommandSender sender = sourceStack.getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(textService.resolve("en_us", "achievetodo.ui.open.players_only"));
            return;
        }
        if (args.length > 0) {
            if (handlePresentationCommand(player, args)) {
                return;
            }
            sender.sendMessage(textService.resolve(player.getLocale(), "achievetodo.ui.pack.usage"));
            return;
        }
        uiRuntime.open(player);
    }

    @Override
    public String permission() {
        return null;
    }

    private boolean handlePresentationCommand(Player player, String[] args) {
        if (!"pack".equalsIgnoreCase(args[0])) {
            return false;
        }
        String locale = player.getLocale();
        if (args.length == 1 || "status".equalsIgnoreCase(args[1])) {
            UiPresentationMode mode = uiRuntime.getPresentationMode(player);
            player.sendMessage(textService.text(locale, "achievetodo.ui.pack.status", mode.commandValue()));
            return true;
        }
        return UiPresentationMode.fromCommandValue(args[1]).map(mode -> {
            uiRuntime.setPresentationMode(player, mode);
            player.sendMessage(textService.text(locale, "achievetodo.ui.pack.updated", mode.commandValue()));
            return true;
        }).orElse(false);
    }
}
