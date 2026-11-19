package com.diskree.achievetodo.core;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.adapters.BukkitRestrictionResolver;
import com.diskree.achievetodo.adapters.BukkitRestrictionTargets;
import com.diskree.achievetodo.landmark.LandmarkQueryResult;
import com.diskree.achievetodo.landmark.LandmarkRecord;
import com.diskree.achievetodo.landmark.bukkit.BukkitLandmarkRuntime;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.ResolvedAbilityState;
import com.diskree.achievetodo.restriction.RestrictionDecision;
import com.diskree.achievetodo.restriction.RestrictionEngine;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.diskree.achievetodo.tracking.TrackedNearbyEntitiesType;
import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class AchieveToDoInspectCommand implements BasicCommand {

    private final BukkitLandmarkRuntime landmarkRuntime;
    private final BukkitProgressionRuntime progressionRuntime;
    private final RestrictionEngine restrictionEngine;

    AchieveToDoInspectCommand(BukkitLandmarkRuntime landmarkRuntime, BukkitProgressionRuntime progressionRuntime) {
        this.landmarkRuntime = Objects.requireNonNull(landmarkRuntime, "landmarkRuntime");
        this.progressionRuntime = Objects.requireNonNull(progressionRuntime, "progressionRuntime");
        this.restrictionEngine = new RestrictionEngine(landmarkRuntime.engine());
    }

    @Override
    public void execute(CommandSourceStack sourceStack, String[] args) {
        CommandSender sender = sourceStack.getSender();
        if (!sender.hasPermission("achievetodo.inspect")) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("player")) {
            sender.sendMessage("Usage: /atdinspect player <player> [ability]");
            return;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            sender.sendMessage("Player not found: " + args[1]);
            return;
        }
        Optional<AbilityType> ability = Optional.empty();
        if (args.length >= 3) {
            ability = AbilityType.findByName(args[2]);
            if (ability.isEmpty()) {
                sender.sendMessage("Unknown ability: " + args[2]);
                return;
            }
        }
        inspectPlayer(sender, player, ability.orElse(null));
    }

    @Override
    public String permission() {
        return "achievetodo.inspect";
    }

    private void inspectPlayer(CommandSender sender, Player player, AbilityType ability) {
        Optional<RestrictionTarget> target = BukkitRestrictionTargets.fromEntity(player);
        if (target.isEmpty()) {
            sender.sendMessage("Unable to resolve restriction target for " + player.getName());
            return;
        }
        PlayerProgressionState state = progressionRuntime.getProgressionState(player.getUniqueId());
        RestrictionTarget restrictionTarget = target.get();
        LandmarkQueryResult landmarkQuery = landmarkRuntime.engine().query(restrictionTarget.world(), restrictionTarget.blockBox());
        RestrictionDecision landmarkDecision = restrictionEngine.checkLandmark(
            state,
            restrictionTarget.world(),
            restrictionTarget.blockBox(),
            BukkitRestrictionResolver.resolveActorMode(player.getGameMode())
        );
        sender.sendMessage("[AchieveToDo] player=" + player.getName() +
            " world=" + player.getWorld().getName() +
            " pos=" + formatPosition(player));
        sender.sendMessage("[AchieveToDo] progression ready=" + state.isReady() +
            " obtained=" + state.getObtainedAdvancementCount() +
            " objective=" + progressionRuntime.getDetectedObjectiveDescription().orElse("none") +
            " nearbyScannerRunning=" + progressionRuntime.isNearbyScannerRunning());
        sender.sendMessage("[AchieveToDo] nearby " + formatNearbySnapshot(progressionRuntime.getNearbySnapshot(player.getUniqueId())));
        sender.sendMessage("[AchieveToDo] targetBox=" + restrictionTarget.blockBox() +
            " landmarks=" + formatLandmarkMatches(landmarkQuery.matches()));
        sender.sendMessage("[AchieveToDo] landmarkDecision kind=" + landmarkDecision.kind().name().toLowerCase(Locale.ROOT) +
            " reason=" + landmarkDecision.reason() +
            " primary=" + (landmarkDecision.landmark() == null ? "none" : landmarkDecision.landmark().getName()) +
            " denied=" + formatDeniedLandmarks(landmarkDecision.deniedLandmarks()));
        if (ability != null) {
            ResolvedAbilityState abilityState = state.getAbilityState(ability);
            sender.sendMessage("[AchieveToDo] ability " + ability.getName() +
                " required=" + state.getRequirements().getRequiredCount(ability) +
                " snapshot=" + abilityState.snapshot().name().toLowerCase(Locale.ROOT) +
                " kind=" + abilityState.kind().name().toLowerCase(Locale.ROOT) +
                " demystified=" + abilityState.demystified());
        }
    }

    private String formatPosition(Player player) {
        Location location = player.getLocation();
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private String formatNearbySnapshot(Map<TrackedNearbyEntitiesType, Integer> snapshot) {
        List<String> parts = new ArrayList<>();
        for (TrackedNearbyEntitiesType type : TrackedNearbyEntitiesType.values()) {
            parts.add(type.name().toLowerCase(Locale.ROOT) + "=" + snapshot.getOrDefault(type, 0));
        }
        return String.join(", ", parts);
    }

    private String formatLandmarkMatches(Iterable<LandmarkRecord> matches) {
        List<String> records = new ArrayList<>();
        for (LandmarkRecord record : matches) {
            var blockBox = record.blockBox();
            records.add(record.landmarkType().getName() +
                "@source=" + record.source().sourceChunk().x() + "," + record.source().sourceChunk().z() +
                "@box=" + blockBox.minX() + "," + blockBox.minY() + "," + blockBox.minZ() +
                ".." + blockBox.maxX() + "," + blockBox.maxY() + "," + blockBox.maxZ());
        }
        if (records.isEmpty()) {
            return "none";
        }
        return records.stream()
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.joining("; "));
    }

    private String formatDeniedLandmarks(Iterable<?> deniedLandmarks) {
        List<String> names = new ArrayList<>();
        for (Object deniedLandmark : deniedLandmarks) {
            names.add(String.valueOf(deniedLandmark).toLowerCase(Locale.ROOT));
        }
        if (names.isEmpty()) {
            return "none";
        }
        names.sort(Comparator.naturalOrder());
        return String.join(",", names);
    }
}
