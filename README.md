# Godot AdMob Plugin (FruitMergeAdMob)

A **consolidated** Godot 4 v2 Android plugin for Google Mobile Ads (AdMob). All ad types — banner, rewarded, interstitial, rewarded interstitial, and native — are handled by a **single plugin singleton** with prefixed methods.

Built on the [Google Mobile Ads SDK](https://developers.google.com/admob/android/quick-start) v25.2.0.

---

## Features

- **Single plugin** — one AAR, one gdap, one engine singleton (`FruitMergeAdMob`)
- **All ad types**: Banner, Rewarded, Interstitial, Rewarded Interstitial, Native
- **Impression-level revenue tracking** via `OnPaidEventListener` — every ad emits `on_paid_event(ad_type, uid, value_micros, currency_code, precision)`
- **UID-based instance tracking** — create multiple ad instances per type
- **Auto-initialization** — `MobileAds.initialize()` runs on plugin load
- **GDScript wrappers** compatible with the PoingGodotAdMob API (just change plugin name + method prefix)

---

## Quick Start

### 1. Build the AAR

```bash
cd godot-admob-android
./gradlew :plugin:assembleRelease
```

Output: `plugin/build/outputs/aar/plugin-release.aar`

### 2. Install in your Godot project

Copy these files into your project:

```
your_project/
  android/
    plugins/
      FruitMergeAdMob.aar        ← renamed from plugin-release.aar
      FruitMergeAdMob.gdap       ← from export_scripts_template/
    build/
      google-services.json       ← from Firebase Console (required for AdMob+Firebase integration)
```

**FruitMergeAdMob.gdap**:
```ini
[config]
name="FruitMergeAdMob"
binary_type="local"
binary="FruitMergeAdMob.aar"

[dependencies]
local=[]
remote=["com.google.android.gms:play-services-ads:25.2.0"]
```

### 3. Enable in Godot Editor

**Project → Export → Android → Plugins tab → check ☑ FruitMergeAdMob**

### 4. Update your GDScript wrappers

All ad wrappers must point to `"FruitMergeAdMob"` and use prefixed methods:

| Wrapper | Plugin Name | Method Prefix |
|---|---|---|
| `MobileAds.gd` | `"FruitMergeAdMob"` | *(none)* |
| `AdView.gd` | `"FruitMergeAdMob"` | `banner_` |
| `RewardedAd.gd` / `RewardedAdLoader.gd` | `"FruitMergeAdMob"` | `rewarded_` |
| `InterstitialAd.gd` / `InterstitialAdLoader.gd` | `"FruitMergeAdMob"` | `interstitial_` |
| `RewardedInterstitialAd.gd` / `Loader` | `"FruitMergeAdMob"` | `rewarded_interstitial_` |
| *(future) NativeAd.gd* | `"FruitMergeAdMob"` | `native_` |

Example:
```gdscript
# Old (separate plugins)
static var _plugin = _get_plugin("FruitMergeAdMobAdView")
_uid = _plugin.create(ad_view_dictionary)

# New (consolidated)
static var _plugin = _get_plugin("FruitMergeAdMob")
_uid = _plugin.banner_create(ad_view_dictionary)
```

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                         GDScript                              │
│  MobileAds.gd   AdView.gd   RewardedAd.gd   InterstitialAd.gd│
│       │             │             │                │          │
│  "FruitMergeAdMob"  │             │                │          │
└───────┬─────────────┴─────────────┴────────────────┘          │
        │  Engine.get_singleton("FruitMergeAdMob")              │
        ▼                                                       │
┌──────────────────────────────────────────────────────────────┐
│                  FruitMergeAdMobPlugin.kt                     │
│                  (single GodotPlugin class)                   │
│                                                              │
│  initialize()    banner_*()    rewarded_*()    interstitial_*()│
│  set_request_    rewarded_     native_*()                     │
│  configuration() interstitial_*()                             │
│                                                              │
│  Signals: on_initialization_complete                         │
│           on_ad_loaded / on_ad_clicked / ...                 │
│           on_rewarded_ad_loaded / on_rewarded_ad_* / ...     │
│           on_interstitial_ad_loaded / ...                     │
│           on_paid_event(ad_type, uid, value, currency, prec) │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
   Google Mobile Ads SDK 25.2.0
```

---

## Kotlin API Reference

### Initialization (no prefix)

| Method | Description |
|--------|-------------|
| `initialize()` | Initialize MobileAds SDK (auto-called on plugin load) |
| `set_request_configuration(dict, test_device_ids)` | Set ad request configuration |
| `get_initialization_status()` | Get current initialization status as Dictionary |
| `set_app_volume(volume: Float)` | Set app volume (0.0–1.0) |
| `set_app_muted(muted: Boolean)` | Mute/unmute app audio for ads |

### Banner (`banner_*`)

| Method | Description |
|--------|-------------|
| `banner_create(ad_view_dict) → Int` | Create a banner ad view, returns UID |
| `banner_load_ad(uid, request_dict, keywords)` | Load an ad into the banner |
| `banner_destroy(uid)` | Destroy the banner |
| `banner_hide(uid)` | Hide the banner |
| `banner_show(uid)` | Show the banner |
| `banner_get_width(uid) → Int` | Get banner width in dp |
| `banner_get_height(uid) → Int` | Get banner height in dp |
| `banner_get_width_in_pixels(uid) → Int` | Get banner width in pixels |
| `banner_get_height_in_pixels(uid) → Int` | Get banner height in pixels |

### Rewarded (`rewarded_*`)

| Method | Description |
|--------|-------------|
| `rewarded_create() → Int` | Create a rewarded ad slot, returns UID |
| `rewarded_load(ad_unit_id, request_dict, keywords, uid)` | Load a rewarded ad |
| `rewarded_show(uid)` | Show the rewarded ad |
| `rewarded_destroy(uid)` | Destroy the rewarded ad |
| `rewarded_set_server_side_verification_options(uid, options_dict)` | Set SSV options |

### Interstitial (`interstitial_*`)

| Method | Description |
|--------|-------------|
| `interstitial_create() → Int` | Create an interstitial ad slot, returns UID |
| `interstitial_load(ad_unit_id, request_dict, keywords, uid)` | Load an interstitial ad |
| `interstitial_show(uid)` | Show the interstitial ad |
| `interstitial_destroy(uid)` | Destroy the interstitial ad |

### Rewarded Interstitial (`rewarded_interstitial_*`)

| Method | Description |
|--------|-------------|
| `rewarded_interstitial_create() → Int` | Create a rewarded interstitial slot |
| `rewarded_interstitial_load(ad_unit_id, request_dict, keywords, uid)` | Load a rewarded interstitial |
| `rewarded_interstitial_show(uid)` | Show the rewarded interstitial |
| `rewarded_interstitial_destroy(uid)` | Destroy the rewarded interstitial |
| `rewarded_interstitial_set_server_side_verification_options(uid, options_dict)` | Set SSV options |

### Native (`native_*`)

| Method | Description |
|--------|-------------|
| `native_create() → Int` | Create a native ad slot |
| `native_load(ad_unit_id, request_dict, keywords, uid)` | Load a native ad |
| `native_get_ad_data(uid) → Dictionary` | Get native ad content (headline, body, CTA, etc.) |
| `native_destroy(uid)` | Destroy the native ad |

---

## Signals

| Signal | Parameters | Description |
|--------|-----------|-------------|
| `on_initialization_complete` | `(status_dict)` | MobileAds SDK initialized |
| `on_ad_loaded` | `(uid)` | Banner ad loaded |
| `on_ad_clicked` | `(uid)` | Banner ad clicked |
| `on_ad_closed` | `(uid)` | Banner ad closed |
| `on_ad_failed_to_load` | `(uid, error_dict)` | Banner failed to load |
| `on_ad_impression` | `(uid)` | Banner impression |
| `on_ad_opened` | `(uid)` | Banner ad opened |
| `on_rewarded_ad_loaded` | `(uid)` | Rewarded ad loaded |
| `on_rewarded_ad_failed_to_load` | `(uid, error_dict)` | Rewarded failed to load |
| `on_rewarded_ad_user_earned_reward` | `(uid, reward_dict)` | User earned reward |
| `on_rewarded_ad_clicked` | `(uid)` | Rewarded ad clicked |
| `on_rewarded_ad_dismissed_full_screen_content` | `(uid)` | Rewarded ad dismissed |
| `on_rewarded_ad_failed_to_show_full_screen_content` | `(uid, error_dict)` | Rewarded failed to show |
| `on_rewarded_ad_impression` | `(uid)` | Rewarded impression |
| `on_rewarded_ad_showed_full_screen_content` | `(uid)` | Rewarded ad showed |
| `on_interstitial_ad_loaded` | `(uid)` | Interstitial loaded |
| `on_interstitial_ad_failed_to_load` | `(uid, error_dict)` | Interstitial failed to load |
| `on_interstitial_ad_clicked` | `(uid)` | Interstitial clicked |
| `on_interstitial_ad_dismissed_full_screen_content` | `(uid)` | Interstitial dismissed |
| `on_interstitial_ad_failed_to_show_full_screen_content` | `(uid, error_dict)` | Interstitial failed to show |
| `on_interstitial_ad_impression` | `(uid)` | Interstitial impression |
| `on_interstitial_ad_showed_full_screen_content` | `(uid)` | Interstitial showed |
| `on_rewarded_interstitial_ad_*` | Same as rewarded | Rewarded interstitial equivalents |
| `on_native_ad_loaded` | `(uid)` | Native ad loaded |
| `on_native_ad_failed_to_load` | `(uid, error_dict)` | Native failed to load |
| `on_native_ad_clicked` | `(uid)` | Native ad clicked |
| `on_native_ad_impression` | `(uid)` | Native impression |
| `on_paid_event` | `(ad_type, uid, value_micros, currency_code, precision)` | Impression-level revenue |

---

## Revenue Tracking

The plugin emits `on_paid_event` for every ad impression with actual revenue data from Google's `OnPaidEventListener`:

```gdscript
# In your admob_manager.gd:
func _connect_paid_event_signals():
    var plugin = Engine.get_singleton("FruitMergeAdMob")
    plugin.connect("on_paid_event", _on_paid_event)

func _on_paid_event(ad_type: String, _uid: int, value_micros: int, currency_code: String, precision: int):
    var revenue = value_micros / 1000000.0
    print("Revenue: %s %f %s (precision=%d)" % [ad_type, revenue, currency_code, precision])
    # Forward to AppsFlyer, Firebase Analytics, etc.
```

---

## Project Structure

```
godot-admob-android/
  plugin/
    src/main/
      AndroidManifest.xml              ← Single plugin registration
      java/com/fruitmerge/admob/
        FruitMergeAdMobPlugin.kt       ← Consolidated plugin (all ad types)
        AdRevenueHelper.kt             ← Revenue logging helper
    build.gradle.kts                   ← Build config (SDK 35, play-services-ads:25.2.0)
    libs/
      godot-lib.aar                    ← Godot Engine library (compileOnly)
  export_scripts_template/
    FruitMergeAdMob.gdap               ← Godot plugin descriptor template
  build.gradle.kts                     ← Root build config
  settings.gradle.kts                  ← Gradle settings
```

---

## Building from Source

### Prerequisites

- Android SDK (API 35)
- Kotlin 2.0+
- Gradle 8.x

### Build

```bash
cd godot-admob-android
./gradlew :plugin:assembleRelease
```

Output: `plugin/build/outputs/aar/plugin-release.aar`

---

## Migration from 5-Plugin Version

Previously this plugin was split into 5 separate `GodotPlugin` classes. To migrate:

1. **Replace 5 gdap files** with single `FruitMergeAdMob.gdap`
2. **Update all GDScript wrappers**: `_get_plugin("FruitMergeAdMobAdView")` → `_get_plugin("FruitMergeAdMob")`
3. **Prefix all method calls**: `create()` → `banner_create()`, `load()` → `rewarded_load()`, etc.
4. **Update revenue tracking**: Connect to single `on_paid_event(ad_type, uid, ...)` signal
5. **Update export_presets.cfg**: Remove 4 extra plugin entries, keep only `FruitMergeAdMob=true`

---

## License

MIT
