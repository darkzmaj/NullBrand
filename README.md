# NullBrand

**NullBrand** is an ultra-lightweight plugin for all servers that removes default proxy branding and provides a clean, configurable MOTD with full HEX color support.

Designed to be minimal, fast, and invisible.

[![discord](https://cdn.modrinth.com/data/cached_images/c97c06e73051a5a1a196babde1652b0cac4b09f4.png)](https://discord.com/invite/4NF45wy2F2)

## Features

- **F3 Branding Override**
  - Replace default proxy branding
  - Fully configurable
  - Supports legacy (`&`) and HEX colors

- **Custom MOTD**
  - Single or multi-line MOTD
  - HEX (`#RRGGBB`) and legacy color support
  - Applied instantly on ping

- **Reload Command**
  - Reload configuration without restarting
  - Command: `/nullbrand reload`
  - Permission-based

- **Extremely Lightweight**
  - No schedulers
  - No background tasks
  - No reflection
  - Minimal memory usage

## Requirements

- **Velocity, BungeeCord, Folia, Paper, Spigot...** 1.16+
- **Java** 17+

## Installation

1. Download the latest `NullBrand.jar`
2. Place it in your server's `plugins/` folder
3. Start the server once to generate the config
4. Edit `config.yml` to your liking
5. Reload using `/nullbrand reload`

## Configuration (`config.yml`)

```yml
# ---------------------------------------------------
# NullBrand Configuration
# Lightweight MOTD system
# ---------------------------------------------------

# Enables debug logging
debug: false

# ---------------------------------------------------
# MOTD SETTINGS
# ---------------------------------------------------
motd:

  # Enable MOTD control
  enabled: true

  # Active profile
  profile: "default"

  # Favicon file inside /plugins/NullBrand/favicons/
  favicon: "test.png"



# ---------------------------------------------------
# MOTD PROFILES
# ---------------------------------------------------
# NOTE: Hover will only work on Proxies, and it does not support HEX Colors!

profiles:

  # Global fallback profile
  global:
    lines:
      - "&7Welcome to our server"
      - "&fHave fun!"

  # Default server MOTD
  default:
    lines:
      - "&5Null&dBrand"
      - "&7This is line 2"
    hover:
      enabled: false
      lines:
        - "&7Welcome to &5Null&dBrand"
        - "&7Running powerful infrastructure"

  # Example event profile
  event:
    lines:
      - "&6Special Event Live!"
      - "&eJoin today!"
    hover:
      enabled: true
      lines:
        - "&6Limited time event"
        - "&eDon't miss it!"

```
