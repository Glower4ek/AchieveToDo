# S2B validation

Use an installed Java25 JDK as the process JAVA_HOME. The successful Windows
validation used Temurin25.0.4+7 and the checked wrapper9.5.0:

```text
gradlew.bat build --offline --dependency-verification strict --rerun-tasks --no-daemon --max-workers=2
```

An online dependency-resolution pass is needed to provision a fresh cache. Do
not regenerate lock/verification files merely to hide a verification failure.
Review new artifacts and metadata, then perform a strict build again. S2B first
generated locks/checksums, encountered incomplete offline/cache and strict
metadata checks, performed additional online resolution, and added two missing
metadata entries after independently matching their bytes from Maven Central.
Existing checksum entries were unchanged; verification was never disabled.
The final offline strict rerun passed.

`check` runs `coreTest` (the existing executable legacy suite) and
`foundationTest`. The ordinary `test` task remains disabled and is not reported
as a test PASS. Foundation coverage: reverse dependency cleanup, immutable
snapshots, partial start at every one of four service positions, cleanup failure
continuation, linkage failure, duplicate ownership, restart after clean stop,
reentrant/idempotent stop, and refusal to publish readiness after failure.

Plugin and sources JARs from the first build and strict offline rerun were
byte-identical. This is repeat-build evidence on the same Windows installation
and dependency cache; clean-machine and Linux reproducibility are not certified.
The source-writing `generateEmbeddedDatapack` task was not executed.

An actual disposable Purpur2620 server ran with only this plugin, existing local
runtime libraries, a new world, loopback binding and no player connections.
Synthetic datapack discovery, lifecycle RUNNING and graceful exit0 were
observed. Shutdown reported retainedServices0, ownedPendingTasks0 and
registeredListeners0. BACAP was absent; the legacy missing-objective diagnostic
was expected. This is not a progression-ready, gameplay, client or protocol PASS.
Generic plugin reload, live partial-enable fault injection and in-flight async
event quiescence remain additional integration proofs.

Pre-existing `SMITHING_NEW` deprecation/removal and Gradle10 deprecation warnings
remain; unrelated legacy mechanics were not changed to silence them.

Detailed commands, logs, artifact hashes and preservation receipts are held in
the external S2B evidence package. S2P requires independent review and separate
authorization; it was not started.
