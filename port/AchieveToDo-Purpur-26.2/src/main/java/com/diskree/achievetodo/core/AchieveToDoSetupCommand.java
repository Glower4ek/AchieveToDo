package com.diskree.achievetodo.core;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.Comparator;

final class AchieveToDoSetupCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        CommandSender sender = sourceStack.getSender();
        if (!sender.hasPermission("achievetodo.setup")) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("mount-nearest")) {
            sender.sendMessage("Usage: /atdsetup mount-nearest <player>");
            return;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            sender.sendMessage("Player not found: " + args[1]);
            return;
        }
        Vehicle vehicle = player.getNearbyEntities(4, 4, 4).stream()
            .filter(entity -> entity instanceof Vehicle)
            .map(entity -> (Vehicle) entity)
            .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(player.getLocation())))
            .orElse(null);
        if (vehicle == null) {
            sender.sendMessage("No nearby vehicle found for " + player.getName());
            return;
        }
        Entity currentVehicle = player.getVehicle();
        if (currentVehicle != null) {
            currentVehicle.removePassenger(player);
        }
        boolean mounted = vehicle.addPassenger(player);
        if (!mounted) {
            sender.sendMessage("Failed to mount " + player.getName() + " into " + vehicle.getType().name());
            return;
        }
        sender.sendMessage("Mounted " + player.getName() + " into " + vehicle.getType().name());
    }

    @Override
    public String permission() {
        return "achievetodo.setup";
    }
}
