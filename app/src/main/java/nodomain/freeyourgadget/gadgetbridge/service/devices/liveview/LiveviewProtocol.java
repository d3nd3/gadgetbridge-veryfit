/*  Copyright (C) 2016-2024 Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.liveview;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

public class LiveviewProtocol {

    private static final int MENU_ID_OFFSET = 3; //TODO: why?

    public static byte[] encodeFindDevice(boolean start) {
        return encodeSetVibrate((short) 100, (short) 200);
    }

    public static byte[] encodeSetTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return encodeGetTimeResponse(now.toEpochSecond() + now.getOffset().getTotalSeconds(), true); //TODO: device prefs
    }

    public static byte[] encodeNotification(NotificationSpec notificationSpec) {
        String headerText;
        // for SMS and EMAIL that came in though SMS or K9 receiver
        if (notificationSpec.sender != null) {
            headerText = notificationSpec.sender;
        } else {
            headerText = notificationSpec.title;
        }

        String footerText = (null != notificationSpec.sourceName) ? notificationSpec.sourceName : "";
        String bodyText = (null != notificationSpec.body) ? notificationSpec.body : "";

        return encodeDisplayPanelMessage(headerText, footerText, bodyText, new byte[0], true);
    }

    public static byte[] encodeDeviceStatusAck() {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_DEVICESTATUS_ACK, new byte[]{(byte) LiveviewConstants.RESULT.OK.ordinal()});
    }

    public static byte[] encodeSetMenuSize(int menuSize) {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_SETMENUSIZE, new byte[]{(byte) menuSize});
    }

    public static byte[] encodeGetScreenMode() {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_GETSCREENMODE, new byte[0]);
    }

    public static byte[] encodeClearDisplay() {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_CLEARDISPLAY, new byte[0]);
    }

    public static byte[] encodeGetCaps() {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();
        w.str(LiveviewConstants.CLIENT_SOFTWARE_VERSION);
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_GETCAPS, w.toArray());
    }

    public static byte[] encodeSetVibrate(int delayTime, int onTime) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();
        w.u16(delayTime)
                .u16(onTime);
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_SETVIBRATE, w.toArray());
    }

    public static byte[] encodeSetLED(int r, int g, int b, int delayTime, int onTime) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();
        int color = ((r & 0x31) << 10) | ((g & 0x31) << 5) | (b & 0x31);
        w.u16(color)
                .u16(delayTime)
                .u16(onTime);

        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_SETLED, w.toArray());
    }

    public static byte[] encodeDisplayText(String text) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();
        w.u8(LiveviewConstants.FORMAT_PLAINTEXT).str(text);
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_DISPLAYTEXT, w.toArray());
    }

    public static byte[] encodeDisplayBitmap(int x, int y, byte[] bitmap) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();

        w.u8(x).u8(y).u8(LiveviewConstants.FORMAT_BITMAP)
                .bytes(bitmap);

        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_DISPLAYBITMAP, w.toArray());
    }

    public static byte[] encodeSetMenuSettings(int vibrationTime, int initialMenuItemId) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();

        w.u8(vibrationTime)
                .u8(12) //font size (but does not seem to change)
                .u8(initialMenuItemId);

        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_SETMENUSETTINGS, w.toArray());
    }

    public static byte[] encodeSetScreenMode(int brightness, boolean auto) {
        int value = (brightness << 1) | (auto ? 1 : 0);
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_SETSCREENMODE, new byte[]{(byte) value});
    }

    private static byte[] encodeScreen(boolean isAlertItem, int totalAlerts, int unreadAlerts, int currentAlert, int menuItemId, String header, String body, String footer, byte[] bitmap, boolean vibrate) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();

        if (!vibrate) menuItemId |= 1; //TODO: this does not mean vibrate!!!
        final boolean hideText = false;

        w.b(isAlertItem)
                .u16(totalAlerts)
                .u16(unreadAlerts)
                .u16(currentAlert)
                .u8(menuItemId)
                .b(hideText)
                .str(header)
                .str(body)
                .str(footer)
                .bytes(bitmap);
        return w.toArray();

    }

    public static byte[] encodeDisplayPanelMessage(String header, String footer, String body,
                                                   byte[] bitmap, boolean vibrate) {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_DISPLAYPANEL, encodeScreen(false, 0, 0, 0, 80, header, body, footer, bitmap, vibrate));
    }

    public static byte[] encodeGetTimeResponse(long timestamp, boolean is24HourDisplay) {
        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();
        w.u32(timestamp).u8(is24HourDisplay ? LiveviewConstants.CLOCK_24H : LiveviewConstants.CLOCK_12H);
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_GETTIME_RESP, w.toArray());
    }

    public static byte[] encodeNavigationResponse(LiveviewConstants.RESULT result) {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_NAVIGATION_RESP, new byte[]{(byte) result.ordinal()});
    }

    public static byte[] encodeGetMenuItemResponse(int menuItemId, boolean isAlertItem,
                                                   int unreadCount, String text, byte[] itemBitmap) {
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_GETMENUITEM_RESP, encodeScreen((!isAlertItem), 0, unreadCount, 0, menuItemId + MENU_ID_OFFSET, null, null, text, itemBitmap, true));
    }

    public static byte[] encodeGetAlertResponse(int totalCount, int unreadCount, int alertIndex,
                                                String timestampText, String headerText,
                                                String bodyTextChunk, byte[] bitmap) {

        LiveviewMessages.MessageWriter w = new LiveviewMessages.MessageWriter();

        w.u8(0)
                .u16(totalCount)
                .u16(unreadCount)
                .u16(alertIndex)
                .u8(0)
                .u8(0)
                .str(timestampText)
                .str(headerText)
                .str(bodyTextChunk)
                .u8(0)
                .u32(bitmap.length)
                .bytes(bitmap);

        //TODO: this looks suspiciously similar to encodeScreen, with extra bytes at the end (the last three elements)
//        encodeScreen(false, totalCount, unreadCount, alertIndex, 0, timestampText, headerText, bodyTextChunk, bitmap, true);
//        return LiveviewMessages.encodeMessage(MSG_GETALERT_RESP, );
        return LiveviewMessages.encodeMessage(LiveviewConstants.MSG_GETALERT_RESP, w.toArray());
    }


}

