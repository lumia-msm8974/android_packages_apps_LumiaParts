# LumiaParts

Device settings for the LineageOS 18.1 (Android 11) ports to the MSM8974
Lumias: Lumia 1520 (`bandit`) and Lumia 930 (`martini`). A system app that
adds:

- a **Lumia settings** launcher icon and entry in **Settings > System**, with
  the current profile and the double tap state as its summary;
- **Performance profile**: the five LineageOS profiles of the power HAL, each
  described with what it does on these phones and with the frequencies of
  the phone's own CPU table;
- **Double tap to wake**;
- **Capacitive keys**: on-screen navigation bar (turns the Back, Start and
  Search keys off through the key disabler), key backlight level (or on/off),
  timeout and "only when touched";
- **Quick Settings tiles**: *Performance* (a tap cycles the profiles, a long
  press opens the page) and *Double tap*.

The app writes no sysfs file and needs no SELinux rule of its own. Every
setting is the one that LineageOS or AOSP already owns, so LineageParts,
AOSP Settings and this app always show the same state:

| Setting | Stored in | Applied by |
|---|---|---|
| Performance profile | `LineageSettings.Secure.PERFORMANCE_PROFILE` (lineage-sdk `PerformanceManager`) | `PerformanceManagerService` → `android.hardware.power-service-qti` (`power-8974.c`, `vendor.lineage.power` `SET_PROFILE`) → the device perf lock library (`lib<device>-perflock`, `ro.vendor.extension_library`) |
| Double tap to wake | `Settings.Secure.DOUBLE_TAP_TO_WAKE` | `PowerManagerService` → power HAL `Mode::DOUBLE_TAP_TO_WAKE` → `TARGET_TAP_TO_WAKE_NODE` (`/sys/devices/f9924000.i2c/i2c-2/2-004b/wake_gesture`, Synaptics driver) |
| Navigation bar / key disabler | `LineageSettings.System.FORCE_SHOW_NAVBAR` | `PhoneWindowManager` → `vendor.lineage.touch@1.0` `IKeyDisabler` of the device |
| Key backlight | `LineageSettings.Secure.BUTTON_BRIGHTNESS`, `BUTTON_BACKLIGHT_TIMEOUT`, `LineageSettings.System.BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED` | `PowerManagerService` → lights HAL, buttons light (PM8941 KPDBL rows) |

Entries and tiles that the build does not support are hidden at run time:
profiles when the power HAL reports none, double tap without
`config_supportDoubleTapWake`, the navigation bar switch without a key
disabler HAL, the key backlight without
`config_deviceSupportsButtonBrightnessControl`.

## Profiles on these phones

`power-8974.c` asks for CPU limits; the device's perf lock library applies
them to cpufreq (requests of *n* × 100 MHz rounded up to the next available
frequency). CPU hotplug and power collapse requests are ignored: the Windows
Phone TrustZone of these phones forbids both, so the four cores are always
online.

| Profile | Result |
|---|---|
| Power save | all cores capped at about 1.0 GHz (first frequency ≥ 1.0 GHz), no touch or launch boosts |
| Efficiency | same cap, boosts kept within it |
| Balanced (default) | full range, touch and launch boosts |
| Quick | minimum about 1.1 GHz (first frequency ≥ 1.1 GHz), boosts |
| Performance | minimum = maximum frequency on all cores; thermal-engine still throttles |

Battery Saver makes LineageOS use Power save; the page says so, and the tile
does nothing while it is on (choosing another profile on the page turns
Battery Saver off, as in LineageParts).

## Not included

- **USB OTG tile.** The Lumia 950 XL needs a manual switch because no driver
  reports the USB-C role. These phones have a micro-USB port whose host mode
  follows the ID pin (`qcom,hsusb-otg-otg-control = <2>`, PMIC ID), and the
  kernel has no switch outside debugfs, so there is nothing to toggle.
- Anything of Android 12 and later (SettingsLib main switch, collapsing
  toolbar) or of the MSM8994.

## Build

Checked out at `packages/apps/LumiaParts` and added to `PRODUCT_PACKAGES` by
the device tree. It is signed with the platform key and runs with the
system uid (`system_app`), which already has the permissions and SELinux
access these services need; it is installed in `system_ext`.

## Credits

- Ported from **CitymanParts**, the device settings of the Lumia 950 XL
  LineageOS 23 port
  ([`giannoccarol/android_packages_apps_CitymanParts`](https://github.com/giannoccarol/android_packages_apps_CitymanParts),
  branch `lineage-23-cityman`): performance profile page, Quick Settings
  tile, Settings entry with a dynamic summary, launcher icon.
- **The LineageOS Project** and AOSP: lineage-sdk (`PerformanceManager`,
  `LineageHardwareManager`, `LineageSettings`), LineageParts (the button
  settings and their defaults, which this app follows), the QTI power HAL
  (`power-8974.c`), SettingsLib (radio button and footer preferences) and
  `org.lineageos.settings.resources` (Settings theme).
- **Android4Lumia950** (djokcho, EpicLPer, j0sh1x, sonic011gamer and the
  community), whose Lumia work the 1520 and 930 ports build on.
- Google, Material icons "speed", "tune" and "touch_app" (Apache 2.0).

## License

Apache License 2.0.
