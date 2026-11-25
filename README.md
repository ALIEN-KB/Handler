```
 _  _              _ _
| || |__ _ _ _  __| | |___ _ _
| __ / _` | ' \/ _` | / -_) '_|
|_||_\__,_|_||_\__,_|_\___|_|
      accessibility autopilot
```

**Handler** is a small Android companion app for people who use an accessibility-based app
(like Curbox, for screen-time / scroll tracking) **and** banking apps that refuse to
run while any accessibility service is on.

## The problem

* Banking apps check the list of enabled accessibility services and block you until you turn them off.
* Turning Curbox's accessibility off by hand every time is slow, and forgetting to turn it back on
  silently breaks your tracking.
* Xiaomi / Redmi (MIUI / HyperOS) often kills the service in the background and Android then drops it
  from the enabled list, so it is "randomly" off after a day or two.

## What Handler does

| Feature | How |
|---|---|
| **Lock shortcuts** | A `🔒 Bank` icon on your home screen. Tapping it switches accessibility **off first**, then opens the bank. |
| **Auto-detect** | Open the bank normally and Handler notices (via Usage Access) and pauses accessibility immediately. |
| **Auto-resume** | Leave the protected app (after a configurable delay, default 8 s, so copying an OTP is fine) or lock the phone and it comes back on. |
| **Watchdog** | Listens to the system setting itself, so if HyperOS turns the service off Handler restores it **instantly**. Also checks on boot, unlock, every 30 s while running and every 15 min via WorkManager. Restarts a service that is "on" but stuck. |
| **Manual pause** | Turning the service off yourself in Settings counts as a manual pause; it comes back when you lock the phone. Also: notification button and a Quick Settings tile. |
| **Hide developer options** *(experimental)* | For banks that also check USB debugging. |
| **Privacy** | No `INTERNET` permission at all. |

It works with any accessibility app; Curbox itself does not need to be modified.

## Install

1. Download the latest APK from **Actions → Build APK → Handler-apk** (or from Releases).
2. Install it and walk through the intro.

## One-time setup (the important bit)

Handler needs `WRITE_SECURE_SETTINGS` to switch accessibility services. Android only lets you grant it over ADB.
It is granted **once** and survives reboots (only an uninstall removes it).

1. *Settings → About phone →* tap **OS version** 7×.
2. *Developer options →* enable **USB debugging** and, on Xiaomi, **USB debugging (Security settings)**
   (requires a Mi account and a SIM card).
3. Connect to a computer and run:

```sh
adb shell pm grant com.alienkb.handler android.permission.WRITE_SECURE_SETTINGS
# optional, grants Usage Access too:
adb shell appops set com.alienkb.handler GET_USAGE_STATS allow
```

No computer? Run the same command from **Shizuku** or **LADB** using *Wireless debugging*.
You can switch USB debugging off afterwards.

### Xiaomi / HyperOS survival kit

For **both Handler and Curbox**:

* Autostart → **on**
* Battery saver → **No restrictions**
* Recent apps → long-press → **lock**
* Other permissions → **Home screen shortcuts** (needed for the lock shortcuts)

## Building

CI builds a signed release APK on every push (`.github/workflows/build.yml`).
Locally: `./gradlew assembleRelease` (JDK 17, Android SDK 35).

The APK is signed with `keystore/handler.jks` so each build installs as an **update**
(uninstalling would lose the ADB permission). The key's password is public; set the
`HANDLER_KS_PASS` env var with your own keystore if you want a private signing key.
