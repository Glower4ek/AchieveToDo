# S4L FINAL — movement gameplay certified

`S4L_FINAL_MOVEMENT_GAMEPLAY_CERTIFIED_WITH_OPEN_INTEGRATION_GATES`.

Exact IDs **jump, sneak, sprint, swim: CERTIFIED 4/4**. Overall **128/153** (previous 124). **PRODUCT_GAMEPLAY_READY=false** until independent full integration certification.

Jump enforcement now validates on-foot ascent before the packet-controlled native ground branch and before position acceptance. Native collision/step resolution and a bounded per-player native velocity envelope authorize permitted ascent. Packet onGround/Input.jump never grant permission. Pinned velocity/teleport hooks record external motion and retire flight state at native landing, world/context change, teleport, reconnect and owner retirement. The old callback-dependent packet guard is removed; canonical ground-jump setters still query current S3C authority. There is no repeated teleport rollback or second gravity engine.

FINAL forged Pos/PosRot true/false-ground, +0.42/+0.21, StatusOnly primer, off-support sequences, landing/forged ascent, held relock and teleport/reconnect regressions pass. Unlocked acceptance remains vanilla-class. Piston, knockback, water, teleport/falls, step-up, levitation, ladder, bubble and slime rebound preserve native motion. No enforcement correction loop appears. Levitation comparison aligns identical native frames because asynchronous effect expiration differed by one frame between runs; the exact difference is retained in evidence.

Sneak constrained pose, passenger/dismount, hold/toggle, held relock, airborne/landing and reconnect pass without forcing an invalid pose. Sprint/swim retain the canonical `isUnderWater ? SWIM : SPRINT` true-setter decision and allow false/stop. Four independent compositions, all eight crossing/relock input permutations, stale/gap/invalid projection, active land/water BACAP loss/recovery, signed CUSTOM/CHAOS, standard three-world resync and group-owner rejection pass. Current server progression remains the only permission authority.

FINAL Purpur `check movementNativeTest portalNativeTest jar`: **15,783 gameplay**, **48 movement bytecode**, **84 portal** checks plus retained core/progression/Bridge/prior-ability suites PASS. Strict available Fabric build and targeted movement tests PASS; Bridge **4,129** PASS. The rebuilt universal client is byte-identical to R2 FINAL v2; no client product source changed. Solo/Fabric Dedicated genuine R2 regressions on that exact client remain valid, with their original fixture failure and correction distinguished. No 0.2.1, Purpur-only client, push, tag or release.

79 selected FINAL native case receipts pass across the corrected proof matrix; repeated variants count as cases, not abilities. Original candidate product failures and harness/setup failures remain immutable. Full Fabric Gradle check remains OPEN for valid missing external fixtures. General admission, busy-transition stress, two-client reload/watchdog and other independent integration gates remain OPEN. Three non-landmark IDs and 22 landmarks remain outside scope.

Evidence: `D:/Vibecode/AchieveToDo_DualRuntime_S4L_FINAL_Evidence`, especially `summary.md`, `native_negative_and_positive_results.md`, `remaining_gates.md` and the immutable final-certification receipt.
