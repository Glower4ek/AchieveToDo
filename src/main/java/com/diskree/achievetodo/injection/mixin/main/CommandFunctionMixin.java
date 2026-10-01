package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.client.LegacyChatText;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;

@Mixin(CommandFunction.class)
public interface CommandFunctionMixin {
    @ModifyVariable(method = "fromLines", at = @At("HEAD"), argsOnly = true)
    private static List<String> achievetodo$restoreAdvancementHover(List<String> lines, @Local(argsOnly = true) Identifier id) {
        boolean advancementMessages = id.getNamespace().equals("bacap_rewards")
            && (id.getPath().startsWith("msg/") || id.getPath().endsWith("/root"));
        boolean abilityMessages = id.getNamespace().equals("achievetodo");
        if (advancementMessages || abilityMessages) return lines.stream().map(LegacyChatText::migrateCommand).toList();
        if (id.getNamespace().equals("blazeandcave")) return lines.stream()
            .map(line -> line.contains("/advancementssearch highlight ") ? LegacyChatText.migrateCommand(line) : line).toList();
        return lines;
    }
}
