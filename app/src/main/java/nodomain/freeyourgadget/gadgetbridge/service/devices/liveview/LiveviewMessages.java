/*  Copyright (C) 2016-2026 Daniele Gobbetti

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
/**
 * Original implementation by Andrew de Quincey (BSD 3-Clause License)
 */
package nodomain.freeyourgadget.gadgetbridge.service.devices.liveview;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;

import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_CLEARDISPLAY_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_DEVICESTATUS;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_DEVICESTATUS_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_DISPLAYBITMAP_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_DISPLAYPANEL_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_DISPLAYTEXT_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_GETALERT;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_GETCAPS_RESP;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_GETMENUITEM;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_GETMENUITEMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_GETSCREENMODE_RESP;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_GETTIME;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_NAVIGATION;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_SETLED_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_SETSCREENMODE_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_SETSTATUSBAR_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_SETVIBRATE_ACK;

public class LiveviewMessages {
    private static final Logger LOG = LoggerFactory.getLogger(LiveviewMessages.class);

    public static byte[] encodeMessage(byte messageId, byte[] payload) {
        return new MessageWriter().u8(messageId).u8(4).u32(payload.length).bytes(payload).toArray();
    }

    public static abstract class LiveviewMessage {
        public final byte messageId;

        protected LiveviewMessage(byte messageId) {
            this.messageId = messageId;
        }

        public static LiveviewMessage parseMessage(byte messageId, byte[] payload) {
            try {
                return switch (messageId) {
                    case MSG_GETCAPS_RESP -> new DisplayCapabilities(messageId, payload);
                    case MSG_SETLED_ACK, MSG_SETVIBRATE_ACK, MSG_DEVICESTATUS_ACK,
                         MSG_SETSCREENMODE_ACK, MSG_CLEARDISPLAY_ACK, MSG_SETSTATUSBAR_ACK,
                         MSG_DISPLAYTEXT_ACK, MSG_DISPLAYBITMAP_ACK, MSG_DISPLAYPANEL_ACK ->
                            new Result(messageId, payload);
                    case MSG_GETMENUITEMS -> new GetMenuItems(messageId, payload);
                    case MSG_GETMENUITEM -> new GetMenuItem(messageId, payload);
                    case MSG_GETTIME -> new GetTime(messageId, payload);
                    case MSG_GETALERT -> new GetAlert(messageId, payload);
                    case MSG_DEVICESTATUS -> new DeviceStatus(messageId, payload);
                    case MSG_NAVIGATION -> new Navigation(messageId, payload);
                    case MSG_GETSCREENMODE_RESP -> new GetScreenMode(messageId, payload);
                    default -> {
                        LOG.warn("Unknown message id: {}", messageId);
                        yield null;
                    }
                };
            } catch (Exception e) {
                LOG.error("Error parsing message id {}", messageId, e);
                return null;
            }
        }

        public byte[] encodeAckMessage() {
            return encodeMessage(LiveviewConstants.MSG_ACK, new LiveviewMessages.MessageWriter().u8(messageId).toArray());
        }
    }

    public static class DisplayCapabilities extends LiveviewMessage {
        public final int width;
        public final int height;
        public final int statusBarWidth;
        public final int statusBarHeight;
        public final int viewWidth;
        public final int viewHeight;
        public final int announceWidth;
        public final int announceHeight;
        public final int textChunkSize;
        public final String softwareVersion;

        public DisplayCapabilities(byte messageId, byte[] payload) {
            super(messageId);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.order(LiveviewConstants.BYTE_ORDER);

            this.width = buffer.get() & 0xFF;
            this.height = buffer.get() & 0xFF;
            this.statusBarWidth = buffer.get() & 0xFF;
            this.statusBarHeight = buffer.get() & 0xFF;
            this.viewWidth = buffer.get() & 0xFF;
            this.viewHeight = buffer.get() & 0xFF;
            this.announceWidth = buffer.get() & 0xFF;
            this.announceHeight = buffer.get() & 0xFF;
            this.textChunkSize = buffer.get() & 0xFF;
            buffer.get(); // idleTimer - ignored

            byte[] versionBytes = new byte[buffer.remaining()];
            buffer.get(versionBytes);
            this.softwareVersion = new String(versionBytes, LiveviewConstants.ENCODING);
        }

        @Override
        public String toString() {
            return String.format("DisplayCapabilities{%dx%d, statusBar=%dx%d, view=%dx%d, announce=%dx%d, textChunk=%d, version=%s}", width, height, statusBarWidth, statusBarHeight, viewWidth, viewHeight, announceWidth, announceHeight, textChunkSize, softwareVersion);
        }
    }

    public static class Result extends LiveviewMessage {
        public final LiveviewConstants.RESULT code;

        public Result(byte messageId, byte[] payload) {
            super(messageId);
            this.code = LiveviewConstants.RESULT.values()[(payload[0] & 0xff)];
        }

        @Override
        public String toString() {
            return String.format("Result{messageId=%d, code=%s}", messageId, code);
        }
    }

    public static class GetMenuItem extends LiveviewMessage {
        public final int menuItemId;

        public GetMenuItem(byte messageId, byte[] payload) {
            super(messageId);
            this.menuItemId = payload[0] & 0xFF;
        }

        @Override
        public String toString() {
            return String.format("GetMenuItem{itemId=%d}", menuItemId);
        }
    }

    public static class GetMenuItems extends LiveviewMessage {
        public GetMenuItems(byte messageId, byte[] payload) {
            super(messageId);
        }

        @Override
        public String toString() {
            return "GetMenuItems{}";
        }
    }

    public static class GetTime extends LiveviewMessage {
        public GetTime(byte messageId, byte[] payload) {
            super(messageId);
        }

        @Override
        public String toString() {
            return "GetTime{}";
        }
    }

    public static class DeviceStatus extends LiveviewMessage {
        public final LiveviewConstants.DEVICESTATUS deviceStatus;

        public DeviceStatus(byte messageId, byte[] payload) {
            super(messageId);
            this.deviceStatus = LiveviewConstants.DEVICESTATUS.values()[payload[0] & 0xff];
        }

        @Override
        public String toString() {
            return String.format("DeviceStatus{%s}", deviceStatus);
        }
    }

    public static class GetAlert extends LiveviewMessage {
        public final int menuItemId;
        public final LiveviewConstants.ALERTACTION alertAction;
        public final int maxBodySize;

        public GetAlert(byte messageId, byte[] payload) {
            super(messageId);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            buffer.order(LiveviewConstants.BYTE_ORDER);

            this.menuItemId = buffer.get() & 0xFF;
            this.alertAction = LiveviewConstants.ALERTACTION.values()[buffer.get() & 0xff];
            this.maxBodySize = buffer.getShort() & 0xFFFF;
        }

        @Override
        public String toString() {
            return String.format("GetAlert{itemId=%d, action=%s, maxBody=%d}", menuItemId, alertAction, maxBodySize);
        }
    }

    public static class Navigation extends LiveviewMessage {
        public final LiveviewConstants.NAVACTION navAction;
        public final LiveviewConstants.NAVTYPE navType;
        public final int menuItemId;
        public final boolean wasInAlert;

        public Navigation(byte messageId, byte[] payload) {
            super(messageId);
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            buffer.get(); // byte0 - unused
            buffer.get(); // byte1 - unused
            int navigation = buffer.get() & 0xFF;
            this.menuItemId = buffer.get() & 0xFF;
            int menuId = buffer.get() & 0xFF;

            this.wasInAlert = (menuId == 20);

            if (navigation != 32) {
                this.navAction = LiveviewConstants.NAVACTION.values()[(byte) ((navigation - 1) % 3)];
                this.navType = LiveviewConstants.NAVTYPE.values()[(byte) ((navigation - 1) / 3)];
            } else {
                this.navAction = LiveviewConstants.NAVACTION.PRESS;
                this.navType = LiveviewConstants.NAVTYPE.MENUSELECT;
            }
        }


        @Override
        public String toString() {
            return String.format("Navigation{action=%s, type=%s, itemId=%d, wasInAlert=%b}", navAction, navType, menuItemId, wasInAlert);
        }
    }

    public static class GetScreenMode extends LiveviewMessage {
        public final boolean auto;
        public final int brightness;

        public GetScreenMode(byte messageId, byte[] payload) {
            super(messageId);
            int raw = payload[0] & 0xFF;
            this.auto = (raw & 1) != 0;
            this.brightness = raw >> 1;
        }

        @Override
        public String toString() {
            return String.format("GetScreenMode{auto=%b, brightness=%d}", auto, brightness);
        }
    }

    static class MessageWriter {
        private static final int DEFAULT_CAPACITY = 4096;
        private final ByteBuffer b;

        MessageWriter() {
            b = ByteBuffer.allocate(DEFAULT_CAPACITY).order(LiveviewConstants.BYTE_ORDER);
        }

        private static byte[] encodeString(String s) {
            return s.getBytes(LiveviewConstants.ENCODING);
        }

        MessageWriter b(boolean v) {
            b.put((byte) (v ? 1 : 0));
            return this;
        }

        MessageWriter u8(int v) {
            b.put((byte) v);
            return this;
        }

        MessageWriter u16(int v) {
            b.putShort((short) v);
            return this;
        }

        MessageWriter u32(long v) {
            b.putInt((int) v);
            return this;
        }

        MessageWriter str(String v) {
            if (StringUtils.isEmpty(v)) {
                u16(0);
            } else {
                u16(encodeString(v).length);
                bytes(encodeString(v));
            }
            return this;
        }

        MessageWriter bitmap(byte[] v) {
            if (v.length > 0) {
                u8(0);
                u32(v.length);
                bytes(v);
            }
            return this;
        }

        MessageWriter bytes(byte[] v) {
            b.put(v);
            return this;
        }

        byte[] toArray() {
            b.flip();
            byte[] result = new byte[b.remaining()];
            b.get(result);
            return result;
        }
    }
}
