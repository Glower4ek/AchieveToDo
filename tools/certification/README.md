# Current S5 certification inputs

This route uses owner-supplied S5-R5 evidence, SHA-256
`0f4f75aa35f0aa3d47ad5b20a0c1477c55c5d24069e7c8be7b2c1e718b0ad804`,
144474242 bytes. Obtain this sealed archive from the project owner. It is an
internal verification prerequisite, not a public downloadable dependency.
No automatic acquisition, current/latest selection or historical fallback exists.
The pinned provisioning contract retains archive identities and acquisition,
licensing and alternative metadata provenance. Third-party archives stay external.

Use Python 3.14, Java 25 and the repository's Gradle 9.8.0 wrapper. From the checkout:

```powershell
python -B tools/certification/r6_inputs.py provision --source $PWD --archive <R5.zip> --assets <external-assets> --authority S5_R3_ALT_METADATA_V1
$env:JAVA_HOME = '<jdk-25>'
$env:ACHIEVETODO_PYTHON = '<python.exe>'
$env:ACHIEVETODO_R6_ASSETS = '<external-assets>'
$env:ACHIEVETODO_R6_OUTPUT = '<separate-external-output>'
$env:ACHIEVETODO_TEST_INPUTS_DIR = "$env:ACHIEVETODO_R6_ASSETS/inputs/alternative"
$env:ACHIEVETODO_INPUT_AUTHORITY = 'S5_R3_ALT_METADATA_V1'
$env:GRADLE_USER_HOME = '<isolated-verified-seeded-gradle-home>'
$env:TEMP = '<short-writable-temp>'
$env:TMP = $env:TEMP
./gradlew.bat --no-daemon --no-watch-fs --project-cache-dir <isolated-project-cache> --dependency-verification strict --init-script tools/certification/r6.init.gradle compileJava compileTestJava compileGametestJava r6AlternativeJUnit --rerun-tasks
./gradlew.bat --no-daemon --no-watch-fs --project-cache-dir <same-project-cache> --dependency-verification strict --init-script tools/certification/r6.init.gradle r6ExportJUnitBinary
```

Expected baseline: 1048 unique test executions, zero skips/errors/failures.
Inspect XML and binary identities, not just Gradle's exit status. Preserve source
commit/tree, dependency inventories, command/environment and original logs.
Seeded builds require retained verified transformed Minecraft bytes; cold-cache
reproducibility has a separate unresolved authority gate. Strict verification stays on.
Original `test`, `check`, historical verifiers and replay refusals retain their
historical semantics and failures. Current authority does not replace them.

Native qualification uses the sealed R5 117 R3 + 19 mapped + 2 location + 1 TNT V2
receipts. Read the sealed native launch, fixture, world and classpath records before
choosing an isolated run. The R4 registration profile is explicit, temporary and
must be restored after execution. B10 deep/companion preparation requires the
sealed B10 inputs and hash-bound R5 wrapper; its original absolute paths describe
provenance only. Do not replay a historical case as fresh qualification.

Product authority remains the accepted S5 Fabric/Purpur binaries. Use an external
JAR destination with the existing same-version guard preserved; do not overwrite
accepted artifacts. Current technical certification is distinct from delivery
approval. PRODUCT_GAMEPLAY_READY=false. No publication is authorized.
