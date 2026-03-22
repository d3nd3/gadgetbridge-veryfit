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
 * Splits long VeryFit v3 frames for BLE characteristics when ATT MTU is still the default
 * (typically 23 → <strong>20 payload bytes</strong> per write). Otherwise writes are truncated
 * ({@code TransactionBuilder} warns: {@code payload … longer than current MTU: 26 > 20}) and the
 * watch never sees a valid CRC.
 * <p>
 * Same layout as {@code htmlapp/toobur-hr-csv.html} {@code sendV3PacketChunked}: first segment is
 * the start of the frame; each continuation segment is {@code 0x33} + next bytes.
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
     * @param mtuPayload max bytes per <em>first</em> segment; continuation segments allow
     *                   {@code mtuPayload - 1} data bytes (one byte for {@code 0x33} prefix).
     */
    static List<byte[]> splitForAttMtu(byte[] fullFrame, int mtuPayload) {
        if (fullFrame == null || fullFrame.length == 0) {
            return Collections.emptyList();
        }
        if (mtuPayload < 2) {
            throw new IllegalArgumentException("mtuPayload");
        }
        if (fullFrame.length <= mtuPayload) {
            return Collections.singletonList(fullFrame);
        }
        List<byte[]> chunks = new ArrayList<>();
        final int maxContData = mtuPayload - 1;
        int firstLen = Math.min(fullFrame.length, mtuPayload);
        chunks.add(Arrays.copyOfRange(fullFrame, 0, firstLen));
        int offset = firstLen;
        while (offset < fullFrame.length) {
            int n = Math.min(fullFrame.length - offset, maxContData);
            byte[] c = new byte[1 + n];
            c[0] = 0x33;
            System.arraycopy(fullFrame, offset, c, 1, n);
            chunks.add(c);
            offset += n;
        }
        return chunks;
    }
}
