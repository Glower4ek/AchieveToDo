package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ProgressionConfigService {
    private final ProgressionConfigTomlCodec codec = new ProgressionConfigTomlCodec();
    // Frozen Fabric ConfigurationIO copper-backfill/v1 multiset, not a live preset lookup.
    private static final int[] COPPER_POOL = {
        2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,46,50,54,58,62,66,70,74,
        78,82,86,90,94,98,98,102,106,110,114,118,122,126,130,134,138,142,146,150,154,158,158,
        162,166,170,174,178,182,186,192,198,204,210,216,222,228,234,240,246,252,258,264,270,
        276,282,288,294,300,306,312,318,324,330,336,342,348,354,360,366,372,378,384,390,396,
        404,412,420,428,436,444,452,460,468,476,484,492,500,508,516,524,532,540,548,556,564,
        572,580,588,596,604,612,620,628,636,644,652,660,668,676,684,692,700,708,716,724,732,
        740,748,756,764,772,780,788,796,804,812,820,828,836,844,852,860
    };
    private static final List<AbilityType> COPPER = List.of(AbilityType.USE_COPPER_TOOLS, AbilityType.EQUIP_COPPER_ARMOR);

    /** Explicit new selection. Existing selected configurations use fresh=false. */
    public ResolvedProgressionConfig resolve(WorldProgressionConfig selection, long seed, ProgressionConfigRepository repository) {
        return resolve(selection, seed, repository, true);
    }
    public ResolvedProgressionConfig resolve(WorldProgressionConfig selection, long seed, ProgressionConfigRepository repository, boolean fresh) {
        var mode = ProgressionModeType.findByName(selection.configName()).orElse(null);
        String id = mode == null ? selection.configName() : mode == ProgressionModeType.CHAOS ? "chaos_" + seed : mode.getName();
        String file = id + ".toml";
        Optional<String> original = repository.read(file);
        String content;
        boolean generated = original.isEmpty();
        if (generated) {
            if (mode == null || !fresh) throw new IllegalStateException("Missing authoritative configuration: " + file);
            Map<AbilityType,Integer> values = switch (mode) {
                case EASY -> Progressions.getEasyProgression();
                case NORMAL -> Progressions.getNormalProgression();
                case HARD -> Progressions.getHardProgression();
                case CHAOS -> ChaosProgressionGenerator.generateChaosProgression(seed);
            };
            content = codec.write(mode.getVersion(), values);
        } else content = original.orElseThrow();
        ParsedProgressionConfig parsed = validate(content, mode);
        if (mode != null) {
            Map<AbilityType,Integer> additions = new LinkedHashMap<>();
            for (AbilityType copper : COPPER) if (parsed.rawAbilities().keySet().stream().noneMatch(k -> AbilityType.findByName(k).orElse(null) == copper)) {
                int value = mode == ProgressionModeType.CHAOS ? sampleCopper(seed, copper) : switch (mode) {
                    case EASY -> copper == AbilityType.USE_COPPER_TOOLS ? 98 : 68;
                    case NORMAL -> copper == AbilityType.USE_COPPER_TOOLS ? 128 : 83;
                    case HARD -> copper == AbilityType.USE_COPPER_TOOLS ? 158 : 98;
                    default -> throw new AssertionError();
                };
                additions.put(copper, value);
            }
            if (!additions.isEmpty()) content = insertCopper(content, additions);
            parsed = validate(content, mode);
        }
        var requirements = effective(parsed, mode == null);
        if (generated || !content.equals(original.orElseThrow())) repository.compareAndWrite(file, original, content);
        return new ResolvedProgressionConfig(selection, file, new ProgressionConfigDefinition(id, parsed.version(), mode, requirements), generated);
    }
    private ParsedProgressionConfig validate(String text, ProgressionModeType mode) {
        var parsed = codec.parse(text);
        if (!parsed.hasAbilitiesTable() || mode != null && parsed.version() < mode.getVersion()) {
            throw new IllegalStateException("Missing abilities table or incompatible progression version");
        }
        return parsed;
    }
    private AbilityRequirements effective(ParsedProgressionConfig parsed, boolean custom) {
        Map<AbilityType,Integer> values = new EnumMap<>(AbilityType.class);
        parsed.rawAbilities().forEach((key,value) -> AbilityType.findByName(key).ifPresent(a -> values.put(a, Math.min(value, 1152))));
        for (AbilityType a : AbilityType.values()) values.putIfAbsent(a, custom && COPPER.contains(a) ? -1 : 0);
        return AbilityRequirements.from(values);
    }
    private String insertCopper(String original, Map<AbilityType,Integer> additions) {
        // Canonical migration envelope: preserve every original byte, refuse ambiguous quoted/dotted statements.
        String newline = original.contains("\r\n") ? "\r\n" : "\n";
        String stripped = original.replace("\r\n", "");
        if (stripped.contains("\r") || newline.equals("\r\n") && stripped.contains("\n")) throw new IllegalArgumentException("Mixed newlines");
        boolean inside = false;
        int insertion = original.length(), offset = 0;
        for (String line : original.split("(?<=\n)", -1)) {
            String statement = ProgressionConfigTomlCodec.statement(line).trim();
            if (statement.startsWith("[")) {
                if (inside && insertion == original.length()) insertion = offset;
                inside = statement.equals("[abilities]");
            } else if (!statement.isEmpty() && !statement.substring(0, statement.indexOf('=')).trim().matches("[A-Za-z0-9_-]+")) {
                throw new IllegalArgumentException("Ambiguous copper insertion key");
            }
            offset += line.length();
        }
        StringBuilder block = new StringBuilder(insertion > 0 && original.charAt(insertion-1) != '\n' ? newline : "");
        additions.forEach((a,v) -> block.append(a.getName()).append(" = ").append(v).append(newline));
        return original.substring(0,insertion) + block + original.substring(insertion);
    }
    private int sampleCopper(long seed, AbilityType ability) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("ATD\0phase-d/copper-backfill/v1\0".getBytes(StandardCharsets.UTF_8));
            digest.update(ByteBuffer.allocate(8).putLong(seed).array());
            digest.update((byte) 0);
            Random random = new Random(ByteBuffer.wrap(digest.digest(ability.getName().getBytes(StandardCharsets.UTF_8))).getLong());
            int candidate = COPPER_POOL[random.nextInt(153)];
            int priority = ability == AbilityType.USE_COPPER_TOOLS ? 70 : 60;
            int anchor = ability == AbilityType.USE_COPPER_TOOLS ? 158 : 98;
            int magnitude = random.nextInt(Math.round((100-priority)*0.5f));
            int shifted = Math.clamp(candidate + (random.nextBoolean() ? magnitude : -magnitude),2,1000);
            int value = (int) Math.round(shifted + priority/100.0*random.nextDouble()*(anchor-shifted));
            if (random.nextDouble()*100 < 1.0) return 0;
            if (random.nextDouble()*100 < 0.2) return -1;
            return value;
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
