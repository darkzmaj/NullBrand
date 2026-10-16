# NullBrand

Lightweight server-list branding: MOTD profiles, legacy/hex colors, hover samples, cached favicons, animations, profile rotation, maintenance branding and configurable displayed player counts. Existing `config.yml` keys are preserved.

## Compatibility

Verified on Minecraft Java **26.3** (protocol 777):

| Platform | Tested runtime | Java |
| --- | --- | --- |
| Paper | 26.3 build 194, stable | 25 |
| Velocity | 4.2.0 build 30, stable | 25 |
| BungeeCord | build 2104 | 21 / 25 |

Waterfall is discontinued and remains a legacy, untested target. Spigot and older platform versions retain compatibility adapters but were not runtime tested. Plugin classes retain Java 17 bytecode; use the Java runtime required by the host. Folia is not declared supported because it has not been validated.

MiniMessage (including gradients) is supported on Paper/Velocity using their Adventure libraries. BungeeCord/Spigot use legacy `&` codes and hex syntax (`&#RRGGBB`, `<#RRGGBB>`). Do not use MiniMessage gradients on those platforms.

## Install and configure

Copy `NullBrand-2.0.1.jar` into `plugins/`, start the host and edit `plugins/NullBrand/config.yml` (Velocity uses `plugins/nullbrand/`). Place PNG favicons in the `favicons/` subdirectory. Icons must be 64×64, at most 1 MiB, and stay inside that directory. Images are validated and cached during startup/reload; ping handlers never read files.

Use `/nb reload`, `/nb profile <name>`, `/nb maintenance on|off`, `/nb fakeplayers <amount>`, or `/nb status`. Individual permissions are `nullbrand.reload`, `nullbrand.profile.switch`, `nullbrand.maintenance`, and `nullbrand.fakeplayers`. `nullbrand.admin` permits all management operations. Individual permissions no longer imply administrator access.

Malformed YAML, duplicate keys and unsupported object tags reject reload and retain the previous state. Missing optional values receive defaults. Explicitly disabled per-profile count settings override the global setting. Runtime profile, maintenance and fake-player overrides reset on a successful reload, as before. Maintenance changes the ping branding only, not player login access.

Ping protection bounds the cooldown cache to 16,384 IPs and skips branding for rapid repeat pings; it does not disconnect clients or replace the host's network rate limiting. `max-per-second` remains reserved for future use.

## Build and validation

Build with JDK 25: `bash gradlew clean build`. Distribute `build/libs/NullBrand-2.0.1.jar`; the `-plain.jar` is not a standalone distributable. Only SnakeYAML 2.7 is bundled and relocated; server APIs/Adventure are supplied by the host. SnakeYAML is Apache-2.0 licensed and its notices remain in the archive.

See [CHANGELOG.md](CHANGELOG.md) and [AUDIT.md](AUDIT.md) for validation and limitations. No telemetry or external service was added.

## Release downloads and publishing

Install the compiled **NullBrand-<version>.jar** from [GitHub Releases](https://github.com/darkzmaj/NullBrand/releases); the generated source archives are not installable plugins. Release assets include a SHA-256 file.

The **Publish release JAR** workflow runs for new `vMAJOR.MINOR.PATCH` tags. To attach assets to an existing release, run it manually from `main` with the exact version tag (for example `v2.0.1`). It checks out the resolved tag commit, uses Java 25, runs `clean build` and existing tests, validates plugin metadata and bytecode, and preserves the JAR as an Actions artifact before publishing. The build has read-only repository access; only the separate publishing job has `contents: write`. Existing assets are compared and never overwritten. The audited release tag and original JAR checksum are pinned for v2.0.1; a mismatch stops publication.

Repository Actions must be enabled and permit the workflow's `GITHUB_TOKEN` to request `contents: write` (organization policy must permit it). No personal access token is required. No tag or release needs to be deleted or recreated. See [release notes](docs/releases/v2.0.1.md) for verified platforms and limitations.
