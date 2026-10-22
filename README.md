# Microcord-Android
Microcord-Android is a new Discord client for legacy Android devices, currently in early development. Its goal is to be an evolution of [Discord J2ME](https://github.com/gtrxAC/discord-j2me) and [Droidcord](https://github.com/leap0x7b/Droidcord) with a focus on proxyless connectivity, user-friendly UI, and maintainability.

This repo is for the Android version. There is a separate repo for the [J2ME version](https://github.com/gtrxAC/Microcord), but the plan is to eventually merge these into one project and potentially add support for other Java-based platforms like desktop.

## Status
### Working
* Main menu with servers, DMs, and inbox
* Channel list
* Chat screen with pagination
* Password login
* Token login
* Profile pictures
* Sending messages
* Message timestamps
* Viewing replies
* Viewing attachments
* Embeds
* Sending replies
* Text formatting (using [nnmarkdown](https://github.com/shinovon/nnmarkdown))
* Emojis
* Gateway

### Planned for first release
* Message editing
* Message deleting
* Token file import
* HTTP connection over BlueWAP
* Theme settings (light/dark)

### Planned for later
* Sending attachments
* Threads
* Forums
* Reactions
* Native Holo UI on Android 4.x or higher
* etc.

## Build variants
Official builds are not yet available, this is preliminary information.

| Build | Description |
|-|-|
| **microcord** | Generic build for most devices
