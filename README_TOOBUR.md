# TOOBUR / VeryFit (IDO, Realtek 0x0AF0) — Gadgetbridge feature priority

This fork targets **TOOBUR A200** / **TOOBUR BAND 8** and compatible IDO/VeryFit bands. Implementation lives under `service/devices/toobur/` and `devices/toobur/`, extending **ID115** (`service/devices/id115/`): **normal** commands on **`0x0AF6`** / **`0x0AF7`**, **v3 health / HR** (`0x33…`) on **`0x0AF1`** (write) / **`0x0AF2`** (notify). See repo root **`LATEST_SYNC_PARSING.md`** §11 for the exact route (avoid duplicating GATT details here).

### Discovery (avoid “Xiaomi Smart Band 8” mistaken identity)

Some TOOBUR units advertise a **Band 8–style BLE name** (or similar wording to Xiaomi). Gadgetbridge matches **Xiaomi Smart Band 8** by name *before* generic ID115. **If the device still exposes the VeryFit GATT service `0x0AF0`**, it is **not** a real Xiaomi band: `MiBand8*Coordinator` now refuses those candidates, and `TooburCoordinator` matches names like **A200**, **Band 8**, **BAND8**, plus **Toobur** / **VeryFit** (see `TooburCoordinator.java`).

**Build & install on your phone:** see the repo root **[`gadgetbridge_setup.md`](../gadgetbridge_setup.md)** — *Quick start* (mainline vs **banglejs** flavor, `./gradlew :app:installMainlineDebug`, APK paths, `adb install`).

## Foremost features (user priority)

| Feature | Status | Implementation notes |
|--------|--------|-------------------------|
| **Bind / unbind** | **Manual only** | **No** automatic bind on connect. Device settings → *Send bind* (`04 01 F1…` tail per firmware) and *Send unbind* (`04 02…` — adjust if your capture differs). Implemented in `TooburSupport.onSendConfiguration`. |
| **Health sync (v3)** | **Implemented** | `onFetchRecordedData` runs **`TooburV3FetchHealthOperation`**: **cmd `0x05`** → per-type **cmd `0x04`** (SpO₂, pressure, HR, activity, swim, sleep, sport). Chunked writes on **`0x0AF1`**, replies on **`0x0AF2`**. **Sport summary** → `ID115ActivitySample` when parsed. Reference: **`packetdumps/logcat/sync_example.txt`**, **`LATEST_SYNC_PARSING.md`** §11. |
| **Health sync size / offsets** | **Implemented (baseline)** | Stored offsets + optional sport probe prefs on `TooburSupport`; skip full sync when total unchanged where applicable. |
| **Battery % + voltage** | **Implemented** | `TooburSupport` parses GET **0x02 0x05** reply: level %, voltage (mV), charging state. |
| **Continuous HR** | **Implemented (v3)** | **v3 cmd `0x09`** on **`0x0AF1`** (chunked via `TooburV3BleChunkedWrite`), plus legacy SET **0x03 0x25** for compatibility. Interval from **`toobur_hr_interval_seconds`**; see **`htmlapp/toobur-hr-csv.html`**. |
| **Raise to wake** | **Implemented** | SET **0x03 0x28** with **9-byte** payload per captures: `03 28 AA/55 05 01 00 00 17 3B`. |
| **Set time** | **Implemented** | Inherited `ID115Support.setTime()` on connect and via Gadgetbridge “Sync time”. |
| **Restart device** | **Implemented** | `ID115Support.onReset` → **F0 01**. |
| **Device info** | **Implemented** | GET **0x02 0x01**; firmware / device id shown on card. |

## Files

| Area | Path |
|------|------|
| Support + BLE | `app/src/main/java/.../service/devices/toobur/TooburSupport.java` |
| v3 health fetch op | `app/src/main/java/.../service/devices/toobur/TooburV3FetchHealthOperation.java` |
| v3 reassembly / parsers | `app/src/main/java/.../service/devices/toobur/TooburV3HealthSync.java` |
| v3 HR packets | `app/src/main/java/.../service/devices/toobur/TooburV3HrPackets.java` |
| Coordinator | `app/src/main/java/.../devices/toobur/TooburCoordinator.java` |
| Settings UI | `app/src/main/res/xml/devicesettings_toobur.xml` |
| Protocol constants | `app/src/main/java/.../devices/id115/ID115Constants.java` |
| Legacy activity fetch (other ID115) | `app/src/main/java/.../service/devices/id115/FetchActivityOperation.java` |

## v3 sync sequence (matches VeryFit / `sync_example.txt`)

1. **MANUAL SYNC** in the official app corresponds to **`VBUS_EVT_FUNC_START_SYNC_V3_HEALTH`** — we trigger the same **cmd `0x05`** → **`0x04`** loop from Gadgetbridge fetch.
2. First **v3 cmd `0x05`** uses a monotonic **`nseq`** (internal counter; first slot after connect aligns with captures like **`0x2F`**).
3. Reply to **`0x05`** yields total bytes / package counts; then for each health data type, send **`0x04`** START with offset, consume notify fragments until complete, then **`0x04`** STOP before the next type.

## References

- Repo root: **`TOOBUR.md`**, **`README.md`**, **`LATEST_SYNC_PARSING.md`** (canonical v3 sync route + parsing — do not fork duplicate GATT tables into this file).
- Captures: **`packetdumps/logcat/sync_example.txt`**, **`packetdumps/logcat/reinstall_app_bind_full.txt`**, **`packetdumps/logcat/get_sync_health_v3.txt`**.
- HR layout reference: **`htmlapp/toobur-hr-csv.html`**.
