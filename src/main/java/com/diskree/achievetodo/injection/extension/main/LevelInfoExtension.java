package com.diskree.achievetodo.injection.extension.main;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.ChaosProgressionGenerator;
import com.diskree.achievetodo.ability.ProgressionModeType;
import com.diskree.achievetodo.ability.Progressions;
import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.server.Constants;
import com.moandjiezana.toml.Toml;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface LevelInfoExtension {
    enum ConfigurationOrigin { UNKNOWN, SAVED, FRESH }

    ConfigurationOrigin achievetodo$getConfigurationOrigin();

    void achievetodo$setConfigurationOrigin(ConfigurationOrigin origin);

    String achievetodo$getConfigName();

    void achievetodo$setConfigName(String configName);

    Map<AbilityType, Integer> achievetodo$getAbilitiesConfiguration(long seed);

    /** Configuration IO only; no player, reward or scoreboard operations. Kept in the D-S01 envelope. */
    final class ConfigurationIO {
        private ConfigurationIO() {}

        // Frozen copper-backfill/v1 MULTISET, never reconstructed from live presets.
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
        // Directory-wide coordination also serializes differently named aliases inside the supported config root.
        // Sidecars and mutexes remain stable; deleting either while waiters exist breaks coordination.
        private static final Map<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

        public static Map<AbilityType, Integer> load(Path directory, String name, long seed, ConfigurationOrigin origin) {
            try {
                return load(directory, name, seed, origin, stage -> {}, (temporary, target) -> {
                    try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Abilities configuration transaction failed: " + directory + "/" + name, e);
            }
        }

        // Per-operation seams allow disposable-file fault tests without mutable production test switches.
        private static Map<AbilityType, Integer> load(Path directory, String name, long seed, ConfigurationOrigin origin,
                                                     Consumer<String> checkpoint, BiConsumer<Path, Path> atomicPublisher) throws IOException {
            Objects.requireNonNull(origin, "Configuration origin");
            if (name == null || name.isBlank() || name.equals(".") || name.equals("..") ||
                name.endsWith(".") || name.endsWith(" ") || name.chars().anyMatch(c -> c < 32 || "\\/:*?\"<>|".indexOf(c) >= 0)) {
                throw new IllegalArgumentException("Missing or unsafe abilities configuration name: " + name);
            }
            ProgressionModeType mode = ProgressionModeType.findByName(name);
            Path absolute = directory.toAbsolutePath().normalize();
            if (Files.notExists(absolute, LinkOption.NOFOLLOW_LINKS)) {
                if (mode == null || origin != ConfigurationOrigin.FRESH) throw new IOException("Missing config directory for " + origin);
                Files.createDirectories(absolute);
            }
            Path root = absolute.toRealPath();
            if (!root.equals(absolute) || Files.isSymbolicLink(absolute) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Unsafe configuration directory: " + directory);
            }
            Path target = root.resolve(name + (mode == ProgressionModeType.CHAOS ? "_" + seed : "") + Constants.FileExtension.TOML);
            boolean missing = Files.notExists(target, LinkOption.NOFOLLOW_LINKS);
            if (!missing) {
                byte[] original = readSafe(target);
                Toml parsed = parse(original, mode);
                Map<AbilityType, Integer> effective = effective(parsed, mode == null);
                if (mode == null) {
                    for (AbilityType ability : absentCopper(parsed)) {
                        AchieveToDoMod.logger.warn("Missing custom ability '{}' in '{}'; effective value is -1", ability.getName(), target);
                    }
                    return effective;
                }
                if (absentCopper(parsed).isEmpty()) return effective; // No lock/temp/writer on read-only loads.
            } else if (mode == null || origin != ConfigurationOrigin.FRESH) {
                throw new IOException("Missing authoritative configuration for " + origin + ": " + target);
            }
            ReentrantLock mutex = LOCKS.computeIfAbsent(root, ignored -> new ReentrantLock());
            if (!mutex.tryLock()) throw new IOException("Configuration transaction busy: " + root);
            try {
                Path sidecar = root.resolve(".achievetodo-configuration.lock");
                if (Files.exists(sidecar, LinkOption.NOFOLLOW_LINKS)) attributes(sidecar);
                try (FileChannel channel = FileChannel.open(sidecar, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                     FileLock lock = channel.tryLock()) {
                    if (lock == null) throw new IOException("Configuration transaction busy: " + sidecar);
                    checkpoint.accept("LOCKED");
                    if (!root.toRealPath().equals(root)) throw new IOException("Config directory changed");
                    if (Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) {
                        if (mode == null || origin != ConfigurationOrigin.FRESH) throw new IOException("Missing authoritative configuration: " + target);
                        Map<AbilityType, Integer> generated = switch (mode) {
                            case EASY -> Progressions.getEasyProgression();
                            case NORMAL -> Progressions.getNormalProgression();
                            case HARD -> Progressions.getHardProgression();
                            case CHAOS -> ChaosProgressionGenerator.generateChaosProgression(seed);
                        };
                        StringBuilder text = new StringBuilder("version = " + mode.getVersion() + "\n\n[abilities]\n");
                        for (AbilityType ability : AbilityType.values()) {
                            Integer value = generated.get(ability);
                            if (value == null) throw new IOException("Incomplete fresh progression: " + ability);
                            text.append(ability.getName()).append(" = ").append(value).append('\n');
                        }
                        byte[] candidate = text.toString().getBytes(StandardCharsets.UTF_8);
                        if (!effective(parse(candidate, mode), false).equals(generated)) throw new IOException("Fresh candidate mismatch");
                        return publish(target, null, null, candidate, mode, checkpoint, atomicPublisher);
                    }
                    TargetIdentity identity = attributes(target);
                    byte[] original = readSafe(target);
                    Toml before = parse(original, mode);
                    Map<AbilityType, Integer> oldEffective = effective(before, false);
                    List<AbilityType> absent = absentCopper(before);
                    if (absent.isEmpty()) return oldEffective;
                    rejectAliases(before);
                    Map<String, Long> additions = new LinkedHashMap<>();
                    for (AbilityType ability : absent) {
                        int value = mode == ProgressionModeType.CHAOS ? sample(seed, ability) : switch (mode) {
                            case EASY -> ability == AbilityType.USE_COPPER_TOOLS ? 98 : 68;
                            case NORMAL -> ability == AbilityType.USE_COPPER_TOOLS ? 128 : 83;
                            case HARD -> ability == AbilityType.USE_COPPER_TOOLS ? 158 : 98;
                            case CHAOS -> throw new AssertionError();
                        };
                        additions.put(ability.getName(), (long) value);
                    }
                    byte[] candidate = splice(original, additions);
                    validateInsertion(before, candidate, mode, additions, oldEffective);
                    return publish(target, original, identity, candidate, mode, checkpoint, atomicPublisher);
                }
            } finally { mutex.unlock(); }
        }

        private record TargetIdentity(Object fileKey, long size, FileTime lastModifiedTime) {}

        private static TargetIdentity attributes(Path target) throws IOException {
            BasicFileAttributes a = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile() || a.isSymbolicLink() || !target.toRealPath().equals(target)) {
                throw new IOException("Unsafe or unidentifiable config target: " + target);
            }
            Object key = a.fileKey();
            if (target.getFileSystem().equals(FileSystems.getDefault()) && System.getProperty("os.name").startsWith("Windows")) {
                key = windowsIdentity(target);
            }
            if (key == null) throw new IOException("Provider supplies no stable file identity: " + target);
            if (Files.getFileStore(target).supportsFileAttributeView("unix") &&
                ((Number) Files.getAttribute(target, "unix:nlink", LinkOption.NOFOLLOW_LINKS)).longValue() != 1) {
                throw new IOException("Hard-linked config target: " + target);
            }
            return new TargetIdentity(key, a.size(), a.lastModifiedTime());
        }

        /** Java's Windows basic view has no fileKey/link count. Use Minecraft's existing pinned JNA, never guess. */
        private static String windowsIdentity(Path target) throws IOException {
            try {
                String path = target.toString();
                String nativePath = path.startsWith("\\\\") ? "\\\\?\\UNC\\" + path.substring(2) : "\\\\?\\" + path;
                WinNT.HANDLE handle = Kernel32.INSTANCE.CreateFile(nativePath, WinNT.FILE_READ_ATTRIBUTES,
                    WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE | WinNT.FILE_SHARE_DELETE, null,
                    WinNT.OPEN_EXISTING, WinNT.FILE_FLAG_OPEN_REPARSE_POINT, null);
                if (WinBase.INVALID_HANDLE_VALUE.equals(handle)) throw new IOException("Cannot inspect Windows file identity: " + target);
                try {
                    WinBase.FILE_STANDARD_INFO standard = new WinBase.FILE_STANDARD_INFO();
                    WinBase.FILE_ID_INFO id = new WinBase.FILE_ID_INFO();
                    if (!Kernel32.INSTANCE.GetFileInformationByHandleEx(handle, WinBase.FileStandardInfo, standard.getPointer(), new WinDef.DWORD(standard.size())) ||
                        !Kernel32.INSTANCE.GetFileInformationByHandleEx(handle, WinBase.FileIdInfo, id.getPointer(), new WinDef.DWORD(id.size()))) {
                        throw new IOException("Unsupported Windows identity/link query: " + target);
                    }
                    standard.read(); id.read();
                    if (standard.Directory || standard.DeletePending || standard.NumberOfLinks != 1) throw new IOException("Unsafe/hard-linked Windows config: " + target);
                    byte[] bytes = new byte[16];
                    for (int i = 0; i < bytes.length; i++) bytes[i] = id.FileId.Identifier[i].byteValue();
                    return Long.toUnsignedString(id.VolumeSerialNumber) + ":" + HexFormat.of().formatHex(bytes);
                } finally {
                    if (!Kernel32.INSTANCE.CloseHandle(handle)) throw new IOException("Closing Windows identity handle: " + target);
                }
            } catch (LinkageError | RuntimeException e) { throw new IOException("Windows identity unavailable", e); }
        }

        private static byte[] readSafe(Path target) throws IOException {
            TargetIdentity a = attributes(target);
            byte[] bytes;
            try (FileChannel channel = FileChannel.open(target, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                if (channel.size() > 16 * 1024 * 1024) throw new IOException("Configuration too large: " + target);
                ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(channel.size()));
                while (buffer.hasRemaining()) if (channel.read(buffer) < 0) throw new IOException("Configuration shortened during read");
                bytes = buffer.array();
            }
            TargetIdentity b = attributes(target);
            if (!a.fileKey().equals(b.fileKey()) || a.size() != b.size() || !a.lastModifiedTime().equals(b.lastModifiedTime())) {
                throw new IOException("Configuration changed during read");
            }
            return bytes;
        }

        private static String decode(byte[] bytes) throws IOException {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        }

        private static Toml parse(byte[] bytes, ProgressionModeType mode) throws IOException {
            try {
                Toml toml = new Toml().read(decode(bytes));
                if (mode != null) {
                    Object version = toml.toMap().get(Constants.ConfigKey.VERSION);
                    if (!(version instanceof Long v) || v < mode.getVersion()) throw new IOException("Missing/incompatible built-in config version");
                }
                if (toml.getTable(Constants.ConfigKey.ABILITIES_TABLE) == null) throw new IOException("Absent [abilities] table");
                effective(toml, mode == null); // Validate every recognized explicit value before a possible write.
                return toml;
            } catch (RuntimeException e) { throw new IOException("Invalid abilities TOML", e); }
        }

        private static Map<AbilityType, Integer> effective(Toml toml, boolean custom) throws IOException {
            Map<AbilityType, Integer> result = new EnumMap<>(AbilityType.class);
            for (var entry : toml.getTable(Constants.ConfigKey.ABILITIES_TABLE).toMap().entrySet()) {
                AbilityType ability = AbilityType.findByName(entry.getKey());
                if (ability != null) {
                    if (!(entry.getValue() instanceof Long value)) throw new IOException("Non-integer ability: " + entry.getKey());
                    final int count;
                    try { count = Math.toIntExact(value); }
                    catch (ArithmeticException e) { throw new IOException("Ability integer overflow: " + entry.getKey(), e); }
                    result.put(ability, Math.min(count, Constants.TOTAL_ADVANCEMENTS_COUNT));
                }
            }
            for (AbilityType ability : AbilityType.values()) result.putIfAbsent(ability, custom && COPPER.contains(ability) ? -1 : 0);
            return result;
        }

        private static List<AbilityType> absentCopper(Toml toml) {
            Set<AbilityType> present = EnumSet.noneOf(AbilityType.class);
            for (String key : toml.getTable(Constants.ConfigKey.ABILITIES_TABLE).toMap().keySet()) {
                AbilityType ability = AbilityType.findByName(key);
                if (ability != null) present.add(ability);
            }
            return COPPER.stream().filter(a -> !present.contains(a)).toList();
        }

        private static void rejectAliases(Toml toml) throws IOException {
            Set<AbilityType> seen = EnumSet.noneOf(AbilityType.class);
            for (String key : toml.getTable(Constants.ConfigKey.ABILITIES_TABLE).toMap().keySet()) {
                AbilityType ability = AbilityType.findByName(key);
                if (ability != null && !seen.add(ability)) throw new IOException("Colliding ability aliases: " + key);
            }
        }

        private static byte[] digest(long seed, AbilityType ability) {
            if (!COPPER.contains(ability)) throw new IllegalArgumentException("Not a copper sampler key");
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update("ATD\0phase-d/copper-backfill/v1\0".getBytes(StandardCharsets.UTF_8));
                digest.update(ByteBuffer.allocate(8).putLong(seed).array());
                digest.update((byte) 0);
                return digest.digest(ability.getName().getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
        }

        private static int sample(long seed, AbilityType ability) {
            Random random = new Random(ByteBuffer.wrap(digest(seed, ability)).getLong());
            int candidate = COPPER_POOL[random.nextInt(153)];
            int priority = ability == AbilityType.USE_COPPER_TOOLS ? 70 : 60;
            int anchor = ability == AbilityType.USE_COPPER_TOOLS ? 158 : 98;
            int magnitude = random.nextInt(Math.round((100 - priority) * 0.5f));
            int shifted = Math.clamp(candidate + (random.nextBoolean() ? magnitude : -magnitude), 2, 1000);
            double ratio = (priority / 100.0) * random.nextDouble();
            int value = (int) Math.round(shifted + ratio * (anchor - shifted));
            if (random.nextDouble() * 100.0 < 1.0) return 0;
            if (random.nextDouble() * 100.0 < 0.2) return -1;
            return value;
        }

        /** Conservative line lexer: quotes/escapes/comments are stateful, complex statements are refused. */
        private static byte[] splice(byte[] original, Map<String, Long> additions) throws IOException {
            String text = decode(original);
            if (text.startsWith("\uFEFF")) throw new IOException("BOM unsupported for insertion");
            String newline = text.contains("\r\n") ? "\r\n" : "\n";
            String stripped = text.replace("\r\n", "");
            if (stripped.contains("\r") || (newline.equals("\r\n") && stripped.contains("\n"))) throw new IOException("Mixed newlines");
            boolean abilities = false, found = false;
            int end = -1, offset = 0;
            for (String line : text.split("(?<=\n)", -1)) {
                String statement = lexicalStatement(line).trim();
                if (!statement.isEmpty()) {
                    if (statement.charAt(0) == '[') {
                        if (!statement.matches("\\[[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*\\]")) throw new IOException("Unsupported table header");
                        if (abilities && end < 0) end = offset;
                        String table = statement.substring(1, statement.length() - 1);
                        if (table.startsWith("abilities.")) throw new IOException("Abilities subtables unsupported");
                        abilities = table.equals("abilities");
                        if (abilities) {
                            if (found) throw new IOException("Duplicate abilities header");
                            found = true;
                        }
                    } else {
                        int equals = statement.indexOf('=');
                        if (equals < 1 || !statement.substring(0, equals).trim().matches("[A-Za-z0-9_-]+")) {
                            throw new IOException("Quoted/dotted/ambiguous assignment key unsupported");
                        }
                        String value = statement.substring(equals + 1).trim();
                        if (value.isEmpty()) throw new IOException("Missing scalar");
                        char first = value.charAt(0);
                        if (first == '\'' || first == '"') {
                            if (value.length() < 2 || value.charAt(value.length() - 1) != first || value.startsWith("\"\"\"") || value.startsWith("'''")) {
                                throw new IOException("Multiline/uncertain string unsupported");
                            }
                        } else if (!value.matches("[A-Za-z0-9_+:.TZ-]+")) throw new IOException("Complex value unsupported");
                    }
                }
                offset += line.length();
            }
            if (!found) throw new IOException("Explicit bare [abilities] required");
            if (end < 0) end = text.length();
            StringBuilder block = new StringBuilder();
            if (end > 0 && text.charAt(end - 1) != '\n') block.append(newline);
            for (var addition : additions.entrySet()) block.append(addition.getKey()).append(" = ").append(addition.getValue()).append(newline);
            // Strict UTF8 round-trip gives a byte splice at the validated lexical boundary, with no normalization.
            byte[] prefix = text.substring(0, end).getBytes(StandardCharsets.UTF_8);
            byte[] suffix = text.substring(end).getBytes(StandardCharsets.UTF_8);
            if (!Arrays.equals(original, ByteBuffer.allocate(prefix.length + suffix.length).put(prefix).put(suffix).array())) throw new IOException("Byte round-trip failed");
            byte[] insertion = block.toString().getBytes(StandardCharsets.UTF_8);
            return ByteBuffer.allocate(original.length + insertion.length).put(prefix).put(insertion).put(suffix).array();
        }

        private static String lexicalStatement(String line) throws IOException {
            char quote = 0;
            boolean escaped = false, closed = false;
            int start = -1;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (quote != 0) {
                    if (c == '\n' || c == '\r') throw new IOException("Multiline string unsupported");
                    if (escaped) { escaped = false; continue; }
                    if (quote == '"' && c == '\\') { escaped = true; continue; }
                    if (c == quote) { quote = 0; closed = true; }
                } else {
                    if (c == '#') return line.substring(0, i);
                    if (closed && !Character.isWhitespace(c)) throw new IOException("Uncertain string boundary");
                    if (c == '\'' || c == '"') { quote = c; start = i; }
                    if (start >= 0 && quote != 0 && i + 2 < line.length() && line.charAt(i + 1) == c && line.charAt(i + 2) == c) {
                        throw new IOException("Multiline string unsupported");
                    }
                }
            }
            if (quote != 0 || escaped) throw new IOException("Unterminated string");
            return line;
        }

        private static void validateInsertion(Toml before, byte[] candidate, ProgressionModeType mode,
                                               Map<String, Long> additions, Map<AbilityType, Integer> oldEffective) throws IOException {
            Toml after = parse(candidate, mode);
            Map<String, Object> expected = new HashMap<>(before.toMap());
            Map<String, Object> table = new HashMap<>(before.getTable(Constants.ConfigKey.ABILITIES_TABLE).toMap());
            for (var addition : additions.entrySet()) {
                if (table.putIfAbsent(addition.getKey(), addition.getValue()) != null) throw new IOException("Insertion replaced explicit value");
            }
            expected.put(Constants.ConfigKey.ABILITIES_TABLE, table);
            if (!expected.equals(after.toMap())) throw new IOException("Parsed paths/types/values changed outside copper additions");
            Map<AbilityType, Integer> newEffective = effective(after, false);
            for (AbilityType ability : AbilityType.values()) {
                if (!additions.containsKey(ability.getName()) && !oldEffective.get(ability).equals(newEffective.get(ability))) {
                    throw new IOException("Effective ability changed: " + ability);
                }
            }
        }

        private static Map<AbilityType, Integer> publish(Path target, byte[] original, TargetIdentity identity,
                                                         byte[] candidate, ProgressionModeType mode, Consumer<String> checkpoint,
                                                         BiConsumer<Path, Path> atomicPublisher) throws IOException {
            Path temporary = Files.createTempFile(target.getParent(), "." + target.getFileName() + ".", ".tmp");
            try {
                checkpoint.accept("TEMP_CREATED");
                try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                    ByteBuffer buffer = ByteBuffer.wrap(candidate);
                    while (buffer.hasRemaining()) channel.write(buffer);
                    checkpoint.accept("WRITTEN");
                    channel.force(true);
                    checkpoint.accept("FORCED");
                }
                byte[] reread = readSafe(temporary);
                if (!Arrays.equals(candidate, reread)) throw new IOException("Temporary bytes mismatch");
                parse(reread, mode);
                checkpoint.accept("TEMP_VALIDATED");
                if (Thread.currentThread().isInterrupted()) throw new IOException("Configuration transaction interrupted");
                if (original == null) {
                    if (!Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Fresh config appeared before publication");
                } else {
                    TargetIdentity current = attributes(target);
                    if (!identity.fileKey().equals(current.fileKey()) || !identity.lastModifiedTime().equals(current.lastModifiedTime()) ||
                        !Arrays.equals(original, readSafe(target))) throw new IOException("Authoritative config changed before publication");
                }
                checkpoint.accept("BEFORE_MOVE");
                atomicPublisher.accept(temporary, target); // Provider-supported atomic replacement ONLY; no fallback.
                checkpoint.accept("AFTER_MOVE");
                byte[] published = readSafe(target);
                if (!Arrays.equals(candidate, published)) throw new IOException("Published config mismatch");
                return effective(parse(published, mode), false);
            } finally {
                Files.deleteIfExists(temporary); // Only this operation's unique temporary; never the stable sidecar.
            }
        }
    }
}
