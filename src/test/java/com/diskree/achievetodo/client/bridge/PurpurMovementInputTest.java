package com.diskree.achievetodo.client.bridge;

import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PurpurMovementInputTest {
    public static void main(String[] args) {
        var test = new PurpurMovementInputTest();
        test.relockChangesTheTransmittedSnapshotWhilePhysicalInputStaysHeld();
        test.airborneSneakAndLandingUseTheNativeChangeDetection();
        test.releaseIsNeverSuppressed();
        test.soloDedicatedAndNeutralKeepTheirOriginalInput();
        System.out.println("Purpur movement input: 4 regression cases PASS");
    }
    private final Input held = new Input(true, false, true, false, true, true, true);

    @Test void relockChangesTheTransmittedSnapshotWhilePhysicalInputStaysHeld() {
        Input lastSent = PurpurMovementInput.forServer(held, true, true, false);
        Input relocked = PurpurMovementInput.forServer(held, true, true, true);
        assertNotEquals(lastSent, relocked);
        assertEquals(new Input(true, false, true, false, true, false, true), relocked);
        assertTrue(held.shift());
        assertEquals(relocked, PurpurMovementInput.forServer(held, true, true, true));
        assertNotEquals(relocked, PurpurMovementInput.forServer(held, true, true, false));
    }

    @Test void airborneSneakAndLandingUseTheNativeChangeDetection() {
        Input airborne = PurpurMovementInput.forServer(held, true, false, true);
        assertSame(held, airborne);
        assertNotEquals(airborne, PurpurMovementInput.forServer(held, true, true, true));
    }

    @Test void releaseIsNeverSuppressed() {
        Input released = new Input(true, false, false, false, false, false, false);
        assertSame(released, PurpurMovementInput.forServer(released, true, true, true));
    }

    @Test void soloDedicatedAndNeutralKeepTheirOriginalInput() {
        assertSame(held, PurpurMovementInput.forServer(held, false, true, true));
        assertSame(held, PurpurMovementInput.forServer(held, true, true, false));
    }
}
