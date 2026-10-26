package com.diskree.achievetodo.client.bridge;

import net.minecraft.world.entity.player.Input;

/** Presentation-only input routing; native server setters retain permission authority. */
public final class PurpurMovementInput {
    public static Input forServer(Input original, boolean purpur, boolean grounded, boolean sneakLocked) {
        if (!purpur || !grounded || !sneakLocked || !original.shift()) return original;
        return new Input(original.forward(), original.backward(), original.left(), original.right(),
            original.jump(), false, original.sprint());
    }

    private PurpurMovementInput() {}
}
