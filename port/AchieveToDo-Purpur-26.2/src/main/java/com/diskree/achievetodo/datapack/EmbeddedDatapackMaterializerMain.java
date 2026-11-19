package com.diskree.achievetodo.datapack;

import java.io.IOException;
import java.nio.file.Path;

public final class EmbeddedDatapackMaterializerMain {

    private EmbeddedDatapackMaterializerMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one argument: datapack output directory");
        }
        EmbeddedDatapack.writeToDirectory(Path.of(args[0]));
    }
}
