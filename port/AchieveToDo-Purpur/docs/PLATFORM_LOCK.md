# Platform and dependency lock

Plugin: `0.4.0`; Minecraft/API descriptor: `26.3`. (First written for S2B on 26.2; updated for 0.4.0.)
Build JDK: Eclipse Adoptium Temurin **25.0.5+8**, Windows amd64.
Java toolchain/release: **25**; automatic toolchain downloads are disabled.
Both executable test runners explicitly use the Java 25 launcher.

Gradle wrapper: **9.5.0**. Official distribution SHA256:
`553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746`.
Source: https://services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256

Compile-only and test API:
`io.papermc.paper:paper-api:26.3.build.258-stable`.
JAR SHA256: `e9f3f025fde21188ad78d95101af64870263ac100c182f4438ee02f6c2c44ca8`.
The server API and its dependencies are not shaded into the plugin.
`gradle.lockfile` records resolved configurations and transitive versions;
`gradle/verification-metadata.xml` records strict SHA256 verification for
artifacts and metadata. No trusted-artifact wildcard is used. These are reviewed
resolution receipts, not a claim of independent signature certification.

Runtime: **Purpur 26.3 build 2660** (upstream Paper sync `a040e50c`), API
`26.3.build.2660-stable`. The plugin refuses any other build (`PinnedStartupContract`).

| Artifact | SHA256 |
|---|---|
| Paperclip wrapper | `e831a5dc0e22311b899dcf37d5a138b1ca7104d831bf65f0a5fbb966fef8daae` |
| Patched server | `f363f6047580739c1f31d7b948c95d108731bfe60924fb7b8a026e3f90d9fbd0` |
| Runtime Purpur API | `530ef84c348433d9a58d359f1e1fdc18c5cdeebab2a107acaca68a1787d4ce01` |

This exact pair passed compilation, executable core/foundation tests and an
actual no-player server bootstrap/enable/disable smoke. This proves the exercised
foundation surface, not all gameplay routes, public platform certification,
Linux hosting, plugin coexistence or Fabric/Paper message compatibility.
Those remain conditional gates. Never resolve a dynamic latest API silently.

The JAR contains the existing synthetic ATD datapack only. BACAP content and
redistribution rights are separate; this development build grants no new
third-party distribution permission.
