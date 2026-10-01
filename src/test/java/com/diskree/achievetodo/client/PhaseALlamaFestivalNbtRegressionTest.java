package com.diskree.achievetodo.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Red-capable production compatibility regression; fix requires explicit authorization. */
class PhaseALlamaFestivalNbtRegressionTest {
    @Test void frozenLlamaCarpetPredicatesTargetTheNative26_2EquipmentBodyPath() throws Exception {
        String frozen;
        try (var zip=new ZipFile(Path.of("reference/phase_a_preservation/files/final/bacap.zip").toFile())) {
            try(var input=zip.getInputStream(zip.getEntry("data/blazeandcave/advancement/animal/llama_festival.json"))) {
                frozen=new String(input.readAllBytes(),StandardCharsets.UTF_8);
            }
        }
        Method convert=ExternalPackCompatibility.class.getDeclaredMethod("convertJson",String.class);convert.setAccessible(true);
        Object result=convert.invoke(null,frozen);Method text=result.getClass().getDeclaredMethod("text");text.setAccessible(true);
        var converted=JsonParser.parseString((String)text.invoke(result)).getAsJsonObject();
        var criteria=converted.getAsJsonObject("criteria");assertEquals(16,criteria.size());
        for(var criterion:criteria.entrySet()) {
            String actual=criterion.getValue().getAsJsonObject().getAsJsonObject("conditions").getAsJsonArray("player").get(0).getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("vehicle").get("nbt").getAsString();
            assertEquals("{equipment:{body:{id:\"minecraft:"+criterion.getKey()+"\"}}}",actual,criterion.getKey());
        }
    }
}
