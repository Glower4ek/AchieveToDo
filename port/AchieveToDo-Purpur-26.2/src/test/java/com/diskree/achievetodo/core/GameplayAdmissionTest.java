package com.diskree.achievetodo.core;

/** Release admission needs every part of the composition; the removed 0.2.x modes are unknown. */
public final class GameplayAdmissionTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }

    public static void main(String[] args) {
        check(GameplayAdmission.mode("release") == GameplayAdmission.Mode.RELEASE);
        check(GameplayAdmission.mode("certification") == GameplayAdmission.Mode.RELEASE);
        for (String bad : new String[] {"diagnostic", "product", "Release", "", "unknown"}) {
            try { GameplayAdmission.mode(bad); check(false); }
            catch (IllegalArgumentException expected) { check("ATD_UNKNOWN_GAMEPLAY_MODE".equals(expected.getMessage())); }
        }
        var release = new GameplayAdmission(GameplayAdmission.Mode.RELEASE);
        check(release.operational(true, true, true, true, true, true));
        for (int i = 0; i < 6; i++) {
            boolean[] live = {true, true, true, true, true, true};
            live[i] = false;
            check(!release.operational(live[0], live[1], live[2], live[3], live[4], live[5]));
        }
        System.out.println("ATD_GAMEPLAY_ADMISSION_PASS checks=" + checks);
    }
}
