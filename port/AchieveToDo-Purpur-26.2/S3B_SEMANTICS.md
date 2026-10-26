# S3B progression configuration and ownership

This supersedes the single-dimension configuration scope in S3A_PROGRESSION.md. It remains an experimental backend with **PRODUCT_GAMEPLAY_READY=false**. Diagnostic Bridge 1.2 still accepts only its explicit single world and cancels dimension travel. Connecting this verified backend to a group-aware client projection belongs to S3C.

One logical BACAP game uses the native **global main scoreboard**, vanilla advancement persistence, one group identity, and one configuration UUID. Independent progression groups are unsupported. World UUID, actual registry key, dimension role, and display name are separate identities. There is no first-world or matching-name fallback.

Before enabling progression, discover UUIDs and keys in a disposable discovery-only boot with no player admission. Explicitly create `plugins/AchieveToDo/progression/world-group.properties`:

```properties
formatVersion=1
groupId=my-game
configId=<stable nonzero configuration UUID>
overworld.uuid=<observed world UUID>
overworld.key=minecraft:overworld
nether.uuid=<observed nether UUID>
nether.key=minecraft:the_nether
end.uuid=<observed end UUID>
end.key=minecraft:the_end
```

Overworld is required; other roles may be omitted and then remain unavailable. All declared UUID/key/role bindings must match loaded worlds. Duplicate UUIDs, duplicate keys, partial bindings, unknown fields, missing files, and foreign configuration ownership fail validation. Keep the explicit S3A JVM main-world UUID/key assertions and BACAP installation properties: these must agree with the declared overworld. World names do not participate in ownership.

For an explicitly **new configuration selection**, start once with `-Dachievetodo.progression.freshConfig=true` and optionally `-Dachievetodo.progression.initialConfig=normal`. Remove the fresh flag for normal restarts. The resolved table is published before the bound `world-selection.properties` pointer. A saved selection must retain matching `groupId`, `configId`, `worldUid`, and `worldKey`. No implicit migration of unbound historical pointers is provided. Existing invalid files are never regenerated, including when fresh was explicitly requested.

EASY, NORMAL (MEDIUM alias), HARD and CHAOS select built-in files; other safe names select explicit CUSTOM files. CUSTOM never generates files or random values. Thresholds retain the signed-int canonical domain, clamp only above1152, default missing ordinary keys to0 and missing copper keys to−1. Malformed recognized values, duplicate keys/aliases, missing abilities tables, incompatible built-in versions and missing saved files fail closed. The scalar TOML reader supports comments, simple quoted/bare keys, strings, booleans and decimal integers with separators; complex/unsupported statements are rejected explicitly. Source files remain authoritative and are not normalized on read.

Fresh CHAOS uses all153 hard-preset values and canonical shuffle/shift/priority/special-flag stages. Initial map iteration is process-specific in canonical Java as well: a seed is **not** a portable resolved-table identity. `chaos_<world-seed>.toml` is the durable authority. Saved values are never rerolled. Only missing copper entries in valid built-in files may be inserted under the frozen canonical copper-backfill/v1 rule, preserving all pre-existing bytes. CUSTOM does not backfill. Ambiguous insertion syntax is rejected without a write.

Config files use UTF-8, contained names, a directory transaction lock, expected-content checks, forced temporary-file writes, readback verification, and same-directory atomic publication. Unsupported atomic replacement is an error with no truncation fallback. World-selection publication also forces the temporary file. Generic server/plugin `/reload`, crash/power-loss guarantees beyond supported filesystem atomic publication, hostile external editors ignoring the transaction lock, and independent groups are not certified.

Controlled native `minecraft:reload` validates the complete candidate binding/selection/table and loaded content before using it. The server-thread `reloadConfiguration(false)` API rejects invalid candidates and logs the retained last-known-good generation; initial boot with invalid input closes the backend. Explicit `true` is reserved for an operator-authorized new built-in selection. Config changes recompute numeric permission transitions even if the count is unchanged. Re-reading unchanged state does not repair externally removed synthetic criteria. Initial session reconstruction retains the accepted canonical synthetic synchronization semantics.

Team count resolution is unchanged from S3A: every tick reads the selected objective and sums **only actual current team entries**, including offline and fake entries that are members. Missing score/member team contributes0; nonmembers never contribute. There is no second persisted counter. Equal-count reconciliation, configuration reload and Bridge snapshots invoke no BACAP reward functions. Signed numeric thresholds below−1 now preserve canonical synthetic false→true and true→false transitions;0 and−1 keep their special criteria policy. Fabric's `Integer.MIN_VALUE` old-count sentinel is preserved: numeric permission can be true while that threshold has no representable false→true criterion crossing. Vanilla/BACAP retain all item/XP/trophy ownership; no reward suppression or persistent reward ledger is introduced.

Only the accepted canonical default content profile is admitted by the current exact pack-set validator. Canonical optional item/XP/trophy/cooperative source packs were inspected; enabled optional profiles and external world-generation combinations remain separate NOT_RUN gates. They must receive explicit content manifests and runtime proof before admission is widened. No upstream BACAP assets are included in Git/JAR.

Verification: `s3bTest` requires `ACHIEVETODO_CANONICAL_PRESETS` pointing to the read-only canonical `ability/Progressions.java`. It compiles temporary independent canonical preset/CHAOS oracles without writing to Fabric. Use `progressionTest` with `ACHIEVETODO_TEST_BACAP` pointing to external canonical-prepared packs. Retain dependency locks and strict verification. Actual runtime receipts distinguish diagnostic Bridge sessions from the isolated backend dimension fixture. Stop after S3B controller review.
