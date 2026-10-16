# 2.0.1 — 2026-10-16

- Fix ping cooldown recovery; bound per-IP cache memory and avoid mutable timestamp races.
- Fix maintenance profile selection, static fake-player overrides, per-profile counts, explicit disabled overrides, integer overflow and full-range random counts.
- Fix truncated hex parsing, mixed MiniMessage/legacy rendering and named color tags on Paper/Velocity.
- Isolate Paper/Spigot listeners and Adventure use on BungeeCord; process Paper pings once, including displayed counts and playercount hover text.
- Fix individual command permissions granting admin access; allow independent permissions through platform command entry points.
- Retain working configuration after invalid YAML; use a safe constructor, reject duplicate keys and limit YAML size/aliases.
- Validate favicon paths, symlinks, file size, PNG format and dimensions before decoding; keep all image I/O outside ping events.
- Implement API reload, make maintenance toggles atomic, copy immutable configuration collections, avoid idle animation tasks and logger handler accumulation.
- Pin stable Velocity 4.2.0 and Paper 26.3 build 194 APIs, upgrade/relocate SnakeYAML 2.0 → 2.7; Gradle 9.1 with wrapper checksum, tests, CI and reproducible archives. Remove missing legacy subprojects from settings.
