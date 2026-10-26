package com.diskree.achievetodo.client;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** External R7 conversion/consumption probe; no production serializer override. */
public final class R7ArchiveProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("action input output");
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path input = Path.of(args[1]);
        if (args[0].equals("convert")) {
            Path output = Path.of(args[2]);
            if (Files.exists(output)) throw new IllegalArgumentException("New output required");
            Files.createDirectories(output.toAbsolutePath().getParent());
            ExternalPackCompatibility.copyForWorld(input, output, ExternalPack.BACAP);
            if (!ExternalPackCompatibility.isCompatibleWorldCopy(output, ExternalPack.BACAP)
                    || !ExternalPackCompatibility.isCurrentWorldPack(output, ExternalPack.BACAP)) {
                throw new IllegalStateException("Production conversion admission failed");
            }
        } else if (args[0].equals("consume")) {
            if (!ExternalPackCompatibility.isCompatibleWorldCopy(input, ExternalPack.BACAP)
                    || !ExternalPackCompatibility.isCurrentWorldPack(input, ExternalPack.BACAP)) {
                throw new IllegalStateException("Production marker consumption failed");
            }
        } else throw new IllegalArgumentException("Unknown probe action");
        System.out.println("R7_PRODUCTION_ARCHIVE_PROBE_PASS " + args[0]);
    }
}
