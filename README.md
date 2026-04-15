# VLessGram

> An unofficial Telegram-based Android client with built-in VLESS proxy support.

VLessGram is a fork of [Telegram for Android](https://github.com/DrKLO/Telegram) that embeds [sing-box](https://github.com/SagerNet/sing-box) (via [hiddify-next-core](https://github.com/hiddify/hiddify-next-core)) so the app can speak the VLESS protocol natively — no external VPN required.

**This project is not affiliated with Telegram FZ-LLC, Telegram Messenger Inc., Hiddify, or SagerNet.** "Telegram" is a trademark of Telegram FZ-LLC.

## Features

- All standard Telegram features
- **VLESS proxy** as a first-class proxy type alongside SOCKS5 and MTProto
  - Pastes `vless://` links from clipboard, parses URI parameters
  - Supports TLS, Reality, uTLS fingerprinting, ALPN, transports (tcp/ws/grpc/http)
  - Local SOCKS5 with random port + random credentials per launch — other apps can't piggyback
  - DoH DNS to bypass mobile carrier filtering
- **Stories opt-out**: hide the stories bar, hide story rings around avatars, disable opening stories on avatar tap
- **Disable update checks** (recommended — official Telegram update checks point to the official APK)
- Built-in HTTP debug log viewer on `0.0.0.0:8765` (debug builds only)

All toggles live under **Settings → VLessGram Settings**.

## Building

### Prerequisites

- JDK 17
- Android SDK (API 35), build-tools 35.0.0
- Android NDK 21.4.7075529
- CMake 3.10.2

### Steps

1. Clone the repo:
   ```
   git clone https://github.com/melnoff/vlessgram.git
   cd vlessgram
   ```

2. Download the sing-box AAR (~100 MB, kept out of git):
   ```
   ./scripts/download-singbox.sh
   ```

3. Set up `local.properties` with your Telegram API credentials:
   ```
   cp local.properties.example local.properties
   $EDITOR local.properties
   ```
   Register your own API ID and hash at <https://my.telegram.org/apps>.

4. Build:
   ```
   ./gradlew :TMessagesProj_App:assembleAfatDebug
   ```
   APK lands in `TMessagesProj_App/build/outputs/apk/afat/debug/app.apk`.

   For a smaller production build use `assembleAfatRelease`.

## Project layout

```
TMessagesProj/                      # Library module (Telegram core)
  src/main/java/org/telegram/
    messenger/
      VlessProxyService.java        # sing-box lifecycle wrapper
      VlessPlatformInterface.java   # minimal sing-box PlatformInterface stub
      ForkConfig.java               # fork-specific feature flags
      DebugLogServer.java           # NanoHTTPD log viewer (debug builds only)
    ui/
      ForkSettingsActivity.java     # in-app settings UI for fork features
  libs/singbox/hiddify-core.aar     # downloaded, NOT in git
TMessagesProj_App/                  # Application module
scripts/
  download-singbox.sh               # fetches the hiddify-core AAR
```

## Credits & Licenses

- **Telegram for Android** — GNU GPL v2.0 — © Telegram FZ-LLC, Telegram Messenger Inc.
- **hiddify-next-core** — GNU GPL v3.0 — © Hiddify
- **sing-box** — GNU GPL v3.0 — © nekohasekai (SagerNet)
- **NanoHTTPD** — Modified BSD — used for the debug log viewer

This fork is distributed under the **GNU GPL v3.0** to be compatible with all upstream licenses. See [LICENSE](LICENSE).

## Contact

mail@melnoff.com
