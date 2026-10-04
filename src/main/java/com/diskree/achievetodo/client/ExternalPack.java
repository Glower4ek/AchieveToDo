package com.diskree.achievetodo.client;

import com.diskree.achievetodo.server.Constants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;

public enum ExternalPack {

    BACAP(
        "BlazeandCave's Advancements Pack (BACAP)",
        ChatFormatting.AQUA,
        "https://modrinth.com/datapack/blazeandcaves-advancements-pack",
        "https://cdn.modrinth.com/data/VoVJ47kN/versions/Y2zZ5eSs/BlazeandCave%27s%20Advancements%20Pack%201.21.zip",
        null,
        "14da3f07b5467e8b59ffc0253fd8212c938cd739",
        true
    ),
    BACAP_HARDCORE(
        "BACAP (Hardcore version)",
        ChatFormatting.RED,
        "https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version",
        "https://cdn.modrinth.com/data/QEv1xmKi/versions/ZHHHw5wF/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip",
        null,
        "9c20e14bbef224d2cc4ce63c8d24de1abc7a2475",
        true
    ),
    BACAP_TERRALITH(
        "BACAP (Terralith version)",
        ChatFormatting.GREEN,
        "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/",
        "https://www.mediafire.com/file/ljb8qwofxk4dq9i/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Terralith_1.18.zip/file",
        "2699070cf5040ab519c223178ee64ee9eafe3691",
        "3d8cc170c1bf2a00460a8d7e779acbe9d5034dea",
        false
    ),
    BACAP_AMPLIFIED_NETHER(
        "BACAP (Amplified Nether version)",
        ChatFormatting.DARK_RED,
        "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/",
        "https://www.mediafire.com/file/ak5sjemiz60mzrc/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Amplified_Nether_1.18.zip/file",
        "981ff801e3cf7eace1ddc2fff8b6165c12ea52b0",
        "9956d0a7d26e0b7d166711fa4b6bf856d2993a44",
        false
    ),
    BACAP_NULLSCAPE(
        "BACAP (Nullscape version)",
        ChatFormatting.DARK_PURPLE,
        "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/",
        "https://www.mediafire.com/file/hsj4koctw778e43/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Nullscape_1.18.zip/file",
        "029c29644a9e94dd8c4111dc3ab2165e79fe4d66",
        "6a50de576558b6b9079a60ffdff73cd9e622eac1",
        false
    ),
    TERRALITH(
        "Terralith",
        ChatFormatting.GREEN,
        "https://www.planetminecraft.com/data-pack/terralith-overworld-evolved-100-biomes-caves-and-more/",
        "https://cdn.modrinth.com/data/8oi3bsk5/versions/CzijfXJQ/Terralith_26.2_v2.6.4.zip",
        null,
        "96ccd25be9ba5240ebe8150cc29240aca781f0e1",
        true
    ),
    AMPLIFIED_NETHER(
        "Amplified Nether",
        ChatFormatting.DARK_RED,
        "https://www.planetminecraft.com/data-pack/amplified-nether-1-18/",
        "https://cdn.modrinth.com/data/wXiGiyGX/versions/xIayvf8F/Amplified_Nether_v1.2.15.zip",
        null,
        "94d9604cebbfca667aeb59e4b1ac03e9c6e5cb5d",
        true
    ),
    NULLSCAPE(
        "Nullscape",
        ChatFormatting.DARK_PURPLE,
        "https://www.planetminecraft.com/data-pack/nullscape/",
        "https://cdn.modrinth.com/data/LPjGiSO4/versions/prWWpjSv/Nullscape_26.2_v1.2.20.zip",
        null,
        "bee4a2182593fdfc5da4b253a342feddf02b88b9",
        true
    );

    private final String title;
    private final ChatFormatting color;
    private final String pageUrl;
    private final String downloadUrl;
    private final String wrapperSha1;
    private final String sha1;
    private final boolean inGameDownloadSupported;

    ExternalPack(
        String title,
        ChatFormatting color,
        String pageUrl,
        String downloadUrl,
        String wrapperSha1,
        String sha1,
        boolean inGameDownloadSupported
    ) {
        this.title = title;
        this.color = color;
        this.pageUrl = pageUrl;
        this.downloadUrl = downloadUrl;
        this.wrapperSha1 = wrapperSha1;
        this.sha1 = sha1;
        this.inGameDownloadSupported = inGameDownloadSupported;
    }

    public String getTitle() {
        return title;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getWrapperSha1() {
        return wrapperSha1;
    }

    public String getSha1() {
        return sha1;
    }

    public boolean isInGameDownloadSupported() {
        return inGameDownloadSupported;
    }

    public @NotNull String getFileName() {
        return getName() + Constants.FileExtension.ZIP;
    }

    public @NotNull String getDatapackName() {
        return "file/" + getFileName();
    }

    public @NotNull String getReasonKey() {
        return "downloader.reason." + getName();
    }

    private @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExternalPack mapFromFileName(String fileName) {
        return Arrays.stream(ExternalPack.values())
            .filter(pack -> pack.getFileName().equals(fileName))
            .findFirst()
            .orElse(null);
    }
}
