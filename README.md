# Discofi

Discofi-Android is a new Discord client for legacy Android devices targeting versions under 5.0, currently in development. Its goal is to be an evolution of [Discord J2ME](https://github.com/gtrxAC/discord-j2me) and [Droidcord](https://github.com/leap0x7b/Droidcord) with a focus on proxyless connectivity, user-friendly UI, and maintainability.

> [!IMPORTANT]
> To avoid account restrictions, **use the "Direct" connection option and login with email and password whenever possible.** If you get a CAPTCHA when logging in, you may need to enable 2FA on your account and try the login again **while connected to a Wi-Fi network that you have previously used Discord on**.
> 
> Do **not** follow old Discord J2ME setup guides that ask you to get your token from browser dev tools.

### <div style="text-align: center;">[Download (Codeberg)](https://codeberg.org/gtrxAC/Discofi-Android/releases) - [Download (GitHub)](https://github.com/gtrxAC/Discofi-Android/releases/latest)</div>
### <div style="text-align: center;">[Support Server](https://discord.gg/2GKuJjQagp) <small>(go to #discord-j2me-wap)</small></div>

![](/img/screenshots.png)

This repo is for the Android version. There is a separate repo for the [J2ME version](https://github.com/gtrxAC/Discofi), but the plan is to eventually merge these into one project and potentially add support for other Java-based platforms like desktop.

## Status
### Working
* Password/token login
* Multiple accounts
* Main menu with servers, DMs, and inbox
* Channel list with categories
* Chat screen with pagination, avatars, replies, timestamps
* Sending messages and replies
* Viewing attachments and embeds
* Text formatting with [nnmarkdown](https://github.com/shinovon/nnmarkdown)
* Emojis
* Message editing and deleting
* Gateway
* Settings for theme, message count, etc.
* Unread indicator
* Ping indicator

### Planned for later
* Threads
* Forums
* Reactions
* Notifications
* Sending attachments
* QR login
* Pinned messages
* User profiles
* Member lists on servers and group DMs
* Custom status
* Voice chat
* Native Holo UI on Android 4.x or higher

### Not planned
* Friend requests (unsafe)
* Joining servers (unsafe)

<!--
## Build variants
Official builds are not yet available, this is preliminary information.

| Build | Description |
|-|-|
| **discofi** | Generic build for most devices
-->

## For developers
The build system used is Gradle 4.9 on Java 8. Android Studio can be used for development though using an older version such as 3.0.1 (171.4443003) is recommended.
