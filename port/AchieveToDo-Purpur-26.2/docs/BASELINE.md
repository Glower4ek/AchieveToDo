# Purpur foundation baseline

The immutable legacy baseline is commit
`4edf983e876280fa2761366174dfb6bf53732d4c`, tree
`0de317e54b4eb7d9d8ee5504ad87f8abd2805473` on `s2-purpur-foundation`.
It contains 531 approved files. The 532-file forensic source inventory also
includes `.manual-sources.txt`, which intentionally remains untracked.

S2B retains the existing landmark, progression, restriction and inventory UI
services in their previous startup order. `ServerRuntimeReadiness` owns cleanup
before each service starts, supports partial-enable teardown, and releases
services in reverse order. The plugin unregisters its listeners and cancels its
tasks before releasing their state. Cleanup failures are reported without
skipping the remaining owners; repeated stop does not repeat resource cleanup.

`runtimeStatus()` describes the main-thread lifecycle only. RUNNING does not mean
BACAP progression, world bindings, player admission or client compatibility are
ready. Bridge v1 and handshake admission are not implemented. Existing gameplay
listeners remain active. Do not deploy this increment as a complete dual-runtime
release or admit players expecting the future handshake policy.

No ability registry, reward, progression/team/CHAOS/CUSTOM, world-selection,
landmark-detection or UI semantics are migrated here. In particular, explicit
world/group selection and the remaining canonical parity work still require
their assigned later stages. Fabric and the sibling `original` remain protected.

See `PLATFORM_LOCK.md` and `VALIDATION.md` for the exact build and proof boundary.
