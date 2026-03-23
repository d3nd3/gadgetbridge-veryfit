/*  Copyright (C) 2025 idowatch / TOOBUR device support

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.toobur;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Splits long VeryFit v3 frames when one ATT write cannot hold the full frame. Prefer
 * {@link #splitForAttMtu(byte[], int)} with payload from
 * {@code AbstractBTLEDeviceSupport.calcMaxWriteChunk(gattMtu)} so small frames (e.g. 26-byte HR
 * 0x09) use a single write after MTU exchange; default MTU 23 yields 20 payload bytes per write.
 * <p>
 * First segment is the start of the frame (byte {@code 0x33} is the v3 magic only there).
 * Continuation segments are the <em>next raw bytes</em> of the same frame — do not prefix
 * {@code 0x33} again on follow-up ATT writes (per TOOBUR / VeryFit wire behaviour).
 * </p>
 */
final class TooburV3BleChunkedWrite {
    /** ATT payload when MTU is not negotiated (23-byte ATT → 20-byte L2CAP payload is typical). */
    static final int DEFAULT_ATT_PAYLOAD = 20;
    /** Delay between chunk writes (matches HTML). */
    static final int CHUNK_GAP_MS = 25;

    private TooburV3BleChunkedWrite() {
    }

    static List<byte[]> splitForDefaultAttMtu(byte[] fullFrame) {
        return splitForAttMtu(fullFrame, DEFAULT_ATT_PAYLOAD);
    }

    /**
     * @param mtuPayload max bytes per ATT write payload for every segment (first and continuations).
     */
    static List<byte[]> splitForAttMtu(byte[] fullFrame, int mtuPayload) {
        if (fullFrame == null || fullFrame.length == 0) {
            return Collections.emptyList();
        }
        if (mtuPayload < 1) {
            throw new IllegalArgumentException("mtuPayload");
        }
        if (fullFrame.length <= mtuPayload) {
            return Collections.singletonList(fullFrame);
        }
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < fullFrame.length) {
            int n = Math.min(fullFrame.length - offset, mtuPayload);
            chunks.add(Arrays.copyOfRange(fullFrame, offset, offset + n));
            offset += n;
        }
        return chunks;
    }
}
