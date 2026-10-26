# S2B platform and dependency lock

Development plugin: `0.2.0-dev.1`; Minecraft/API descriptor: `26.2`.
Build JDK: Eclipse Adoptium Temurin **25.0.4+7**, Windows amd64.
Java toolchain/release: **25**; automatic toolchain downloads are disabled.
Both executable test runners explicitly use the Java 25 launcher.

Gradle wrapper: **9.5.0**. Official distribution SHA256:
`553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746`.
Source: https://services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256

Compile-only and test API:
`io.papermc.paper:paper-api:26.2.build.112-stable`.
JAR SHA256: `71c508348ca95f0e33a159d5da5ad351caea5fc866067c06aca0d679bc506763`.
The server API and its dependencies are not shaded into the plugin.
`gradle.lockfile` records resolved configurations and transitive versions;
`gradle/verification-metadata.xml` records strict SHA256 verification for
artifacts and metadata. No trusted-artifact wildcard is used. These are reviewed
resolution receipts, not a claim of independent signature certification.

Runtime smoke: **Purpur 26.2 build2620**, commit `8762b2f`, API
`26.2.build.2620-stable`.

| Artifact | SHA256 |
|---|---|
| Paperclip wrapper | `21003a8bf180855bf8b3cdd7212d6b6ff76f06de5dcba84740c3ed433ba6c34d` |
| Patched server | `340cc119f08eec7397494aa8be83a66b28bbb77d38c39c09721ae79995b03b6d` |
| Runtime Purpur API | `17303f1e64eb2448dffd4645e7c6821c164f68236e2bd855f732bfba371e589f` |

This exact pair passed compilation, executable core/foundation tests and an
actual no-player server bootstrap/enable/disable smoke. This proves the exercised
foundation surface, not all gameplay routes, public platform certification,
Linux hosting, plugin coexistence or Fabric/Paper message compatibility.
Those remain conditional gates. Never resolve a dynamic latest API silently.

The JAR contains the existing synthetic ATD datapack only. BACAP content and
redistribution rights are separate; this development build grants no new
third-party distribution permission.
