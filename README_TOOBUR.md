# TOOBUR / VeryFit (IDO, Realtek 0x0AF0) — Gadgetbridge feature priority

This fork targets **TOOBUR A200** / **TOOBUR BAND 8** and compatible IDO/VeryFit bands. Implementation lives under `service/devices/toobur/` and `devices/toobur/`, extending **ID115** (`service/devices/id115/`) for the classic command pipe on **0x0AF6** / **0x0AF7**.

### Discovery (avoid “Xiaomi Smart Band 8” mistaken identity)

Some TOOBUR units advertise a **Band 8–style BLE name** (or similar wording to Xiaomi). Gadgetbridge matches **Xiaomi Smart Band 8** by name *before* generic ID115. **If the device still exposes the VeryFit GATT service `0x0AF0`**, it is **not** a real Xiaomi band: `MiBand8*Coordinator` now refuses those candidates, and `TooburCoordinator` matches names like **A200**, **Band 8**, **BAND8**, plus **Toobur** / **VeryFit** (see `TooburCoordinator.java`).

**Build & install on your phone:** see the repo root **[`gadgetbridge_setup.md`](../gadgetbridge_setup.md)** — *Quick start* (mainline vs **banglejs** flavor, `./gradlew :app:installMainlineDebug`, APK paths, `adb install`).

## Foremost features (user priority)

| Feature | Status | Implementation notes |
|--------|--------|-------------------------|
| **Bind** | **Implemented (opt-in)** | On connect, optional `04 01 F1 01 01 02 02 01 00` (same as `app_fresh_launch` / `confirmed-only.html`). Toggle: device settings → *Send bind on connect*. |
| **Health sync** | **Partial** | Legacy **CMD 0x08** activity fetch (`FetchActivityOperation`) uses **0x0AF1** / **0x0AF2**. Real TOOBUR A200 data often uses **v3** (`33 DA AD DA AD …`) on the bulk channel — **not fully implemented**; needs queue, chunking, CRC, and parsers. See repo root `TOOBUR.md` § v3 health. |
| **Health sync size / offsets** | **Planned** | VeryFit uses v3 **cmd 0x05** (sizes) then **cmd 0x04** per data type with stored offsets. Requires v3 stack + persistence (`packetdumps/logcat/get_sync_health_v3.txt`). |
| **Battery % + voltage** | **Implemented** | `TooburSupport.onCharacteristicChanged` parses GET **0x02 0x05** reply: level %, voltage (mV), charging state. |
| **Continuous HR: frequency + toggle** | **Partial** | **Legacy:** SET **0x03 0x25** (mode off/auto/manual) + **0x03 0x52** (realtime sensor). **VeryFit A200** often uses **v3 cmd 0x09** for continuous HR; not yet in Gadgetbridge. |
| **Raise to wake** | **Implemented (fixed)** | SET **0x03 0x28** with **9-byte** payload per captures: `03 28 AA/55 05 01 00 00 17 3B` (not 2-byte short form). |
| **Set time** | **Implemented** | Inherited `ID115Support.setTime()` on connect and via Gadgetbridge “Sync time”. |
| **Restart device** | **Implemented** | `ID115Support.onReset` → **F0 01**. |
| **Device info** | **Implemented** | GET **0x02 0x01**; firmware / device id shown on card. |

## Files

| Area | Path |
|------|------|
| Support + BLE | `app/src/main/java/.../service/devices/toobur/TooburSupport.java` |
| Coordinator | `app/src/main/java/.../devices/toobur/TooburCoordinator.java` |
| Settings UI | `app/src/main/res/xml/devicesettings_toobur.xml` |
| Protocol constants | `app/src/main/java/.../devices/id115/ID115Constants.java` |
| Legacy activity fetch | `app/src/main/java/.../service/devices/id115/FetchActivityOperation.java` |

## Next steps (v3 health)

1. Enable **0x0AF2** notifications (done in `TooburSupport.initializeDevice`).
2. Implement **v3 TX** builder (CRC, `nseq`, chunking) — align with `htmlapp/toobur-hr-csv.html` / `TOOBUR.md`.
3. Implement **cmd 0x05** → **cmd 0x04** state machine and **offset** storage (SpO2, pressure, activity, sleep, sport, HR).
4. **Optional:** expose **GET 0x02 0xB0** / **0xB1** (screen / wrist param readback) in UI or debug.

## References

- Repo root: `TOOBUR.md`, `packetdumps/logcat/reinstall_app_bind_full.txt`, `get_sync_health_v3.txt`.
