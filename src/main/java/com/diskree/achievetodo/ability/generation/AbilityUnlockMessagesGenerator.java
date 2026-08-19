package com.diskree.achievetodo.ability.generation;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.client.gui.DesignCodePalette;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.util.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class AbilityUnlockMessagesGenerator implements DataProvider {

    protected final FabricPackOutput dataOutput;

    protected AbilityUnlockMessagesGenerator(FabricPackOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        return CompletableFuture.runAsync(() -> {
            try {
                createFunctions(writer);
            } catch (IOException e) {
                AchieveToDoMod.logger.error("Error while generating AbilityUnlockMessages:", e);
            }
        }, Util.backgroundExecutor());
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    private void createFunctions(CachedOutput dataWriter) throws IOException {
        for (AbilityType abilityType : AbilityType.values()) {
            String function = buildFunction(abilityType);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
            try (BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8))
            ) {
                bufferedWriter.write(function);
            }
            Path functionsPath = dataOutput
                .createPathProvider(PackOutput.Target.DATA_PACK, "function")
                .file(AbilityAdvancementsGenerator.buildAdvancementId(abilityType), "mcfunction");
            dataWriter.writeIfNeeded(functionsPath, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
        }
    }

    private @NotNull String buildFunction(@NotNull AbilityType ability) {
        String function = """
            tellraw @a {
                "translate":"{MOD_ID}.ability_unlocked_chat_message",
                "with":[
                    {
                        "selector":"@s"
                    },
                    {
                        "color":"{COLOR}",
                        "text":"["
                    },
                    {
                        "color":"{COLOR}",
                        "translate":"{MOD_ID}.ability.{NAME}.name",
                        "clickEvent":{
                            "action":"run_command",
                            "value":"/advancementssearch highlight {ADVANCEMENT_ID} obtained_status"
                        },
                        "hoverEvent":{
                            "action":"show_text",
                            "contents":{
                                "color":"{COLOR}",
                                "translate":"{MOD_ID}.ability.{NAME}.name",
                                "extra":[
                                    {
                                        "text":"\\n"
                                    },
                                    {
                                        "color":"{COLOR}",
                                        "translate":"{MOD_ID}.ability.{NAME}.description"
                                    },
                                    {
                                        "text":"\\n\\n"
                                    },
                                    {
                                        "color":"gray",
                                        "italic":true,
                                        "translate":"%1$s tab",
                                        "with":[
                                            {
                                                "text":"{MOD_NAME}"
                                            }
                                        ]
                                    }
                                ]
                            }
                        }
                    },
                    {
                        "color":"{COLOR}",
                        "text":"]"
                    }
                ]
            }
            """
            .replace("{NAME}", ability.getName())
            .replace("{ADVANCEMENT_ID}", AbilityAdvancementsGenerator.buildAdvancementId(ability).toString())
            .replace("{COLOR}", DesignCodePalette.TEXT_COLOR_NAME)
            .replace("{MOD_NAME}", BuildConfig.MOD_NAME)
            .replace("{MOD_ID}", BuildConfig.MOD_ID);
        return String.join("", Arrays.stream(function.split("\\R")).map(String::trim).toArray(String[]::new));
    }
}
