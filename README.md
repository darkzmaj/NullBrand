# NullBrand

**Controls how your server looks in the Minecraft server list: MOTD, icon, player count and hover text, on a proxy or a single server.**

NullBrand answers server list pings with your own MOTD profiles, icon and player count text. You can switch profiles with a command, rotate them on a timer or show a maintenance profile. It is one small jar that runs on Velocity, BungeeCord, Paper, Spigot and Folia, and it only does work when someone pings your server.

## Features

- **MOTD profiles.** Several named profiles; pick the active one with a command.
- **Colours.** `&` codes and hex colours everywhere, plus MiniMessage gradients on Velocity and Paper.
- **Server icon.** A 64 by 64 PNG, set globally or per profile.
- **Hover text and player count.** Hover lines, an optional fake online count and a custom maximum.
- **Animations and rotation.** Animated MOTD frames and profiles that switch on a timer.
- **Maintenance profile.** One command shows a maintenance MOTD. It only changes the server list; it does not block logins.
- **Ping cooldown.** Repeated pings from one IP within 500 ms are left alone.
- **Placeholders** such as `%online%`, `%time%` and `%profile%`.

## Supported platforms

- **Velocity and Paper:** every feature (tested on Velocity 4.2.0 and Paper 26.3).
- **BungeeCord:** every feature except MiniMessage (tested on build 2104).
- **Spigot and Folia:** 1.21 and newer. Folia from 2.0.2. Plain Spigot shows the MOTD, icon and maximum players only, and was not runtime-tested.
- **Java:** 17 or newer. Current Paper and Velocity need Java 25.
- **Not supported:** servers older than 1.21. NullBrand does not change the F3 debug screen.

## Installation

1. Download `NullBrand-2.0.2.jar` from one of the links below.
2. Put it in the `plugins` folder of your proxy or server and restart.
3. Write your MOTD lines in a profile in `config.yml`, then run `/nullbrand reload`.

Full setup: [Install guide](https://kori.my/docs/nullbrand/install/).

## Privacy

NullBrand opens no network connections of its own. It does not check for updates and sends no statistics.

## Links

- **Documentation:** https://kori.my/docs/nullbrand/
- **Download:** [GitHub](https://github.com/darkzmaj/NullBrand/releases/latest), [Modrinth](https://modrinth.com/plugin/nullbrand), [SpigotMC](https://www.spigotmc.org/resources/132353/)
- **Source code:** https://github.com/darkzmaj/NullBrand
- **Support:** [Discord](https://darkzmaj.eu/discord) and https://kori.my

[![Join our Discord: darkzmaj.eu/discord](https://kori.my/assets/img/brand/discord-banner.png)](https://darkzmaj.eu/discord)

Free and open source under the MIT licence. Made by DarkZmaj for KORI Network.
