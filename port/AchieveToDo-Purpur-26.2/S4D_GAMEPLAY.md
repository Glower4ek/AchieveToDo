# S4D native food gameplay

Status: S4D_GAMEPLAY_PASS_WITH_OPEN_ADMISSION_GATE. PRODUCT_GAMEPLAY_READY=false.

20 additional exact food IDs;62/153 implemented,91 remaining. NativeFoodAbilities matches canonical Foods references by identity through the pinned26.2 native ItemStack component contract. BukkitFoodGameplayListener denies native item use without changing the block-use decision and rechecks current S3C authority before consumption completion, including an earlier plugin's replacement food. Native code owns all unlocked effects/depletion/remainders; no client authority, new counter, reward ledger or unlock cache.

Added IDs: eat_apple, eat_beetroot, eat_bread, eat_chicken, eat_chorus_fruit, eat_cod, eat_cooked_beef, eat_cooked_cod, eat_dried_kelp, eat_enchanted_golden_apple, eat_golden_apple, eat_honey, eat_mushroom_stew, eat_poisonous_potato, eat_rabbit_stew, eat_rotten_flesh, eat_salmon, eat_spider_eye, eat_suspicious_stew, eat_tropical_fish.

Validation: full offline check PASS;8799 gameplay checks (6228 prior +2571 new) and unchanged shared Bridge/progression suites. 208 distinct accepted native receipts on Java25/Minecraft26.2/Purpur2620/actual BACAP. All20 additions have native locked/unlocked, API relock/unlocked and cancellation receipts; all42 prior IDs have native regression coverage. Controlled scoreboard boundaries are not natural-acquisition certification. Earlier failed fixture runs are retained separately.

Admission remains OPEN. Actual packet/receiver/queue/cache/FINISH/JOIN traces from2 cold+2 reconnect sessions did not reproduce the intermittent failure; no speculative Bridge or Fabric fix. Fabric product source/JARs remain unchanged. Optional rewards, two-active-client reload, arbitrary multiworld/proxy, remaining91 abilities and production integration are not certified.

Detailed per-ID contracts, raw native state receipts, input/artifact hashes, failure provenance and preservation are in D:\Vibecode\AchieveToDo_DualRuntime_S4D_Evidence. No0.2.1, push, tag or release. S4D STOP.
