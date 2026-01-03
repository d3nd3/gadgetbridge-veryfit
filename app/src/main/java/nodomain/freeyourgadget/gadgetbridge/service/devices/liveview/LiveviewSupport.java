/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, Daniele
    Gobbetti, Sebastian Kranz

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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;

import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.MSG_DISPLAYPANEL_ACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants.RESULT;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.liveview.LiveviewMessages.LiveviewMessage.parseMessage;

public class LiveviewSupport extends AbstractBTBRDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(LiveviewSupport.class);
    private static final int BUFFERSIZE = 4096;
    private static final int MENU_VIBRATION_TIME = 5;
    private static final int MENU_SIZE = 4;
    private final ByteBuffer packetBuffer = ByteBuffer.allocate(BUFFERSIZE).order(LiveviewConstants.BYTE_ORDER);
    private LiveviewMessages.DisplayCapabilities deviceCapabilities;

    public LiveviewSupport() {
        super(LOG, BUFFERSIZE);
        addSupportedService(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    }

    private static byte[] drawableToByteArray(android.content.Context context, int drawableRes,
                                              int width, int height) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableRes);

            if (drawable == null) {
                return new byte[0];
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(android.graphics.Color.WHITE);

            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] result = stream.toByteArray();
            stream.close();
            bitmap.recycle();

            return result;
        } catch (Exception e) {
            LOG.error("Error converting drawable to byte array", e);
        }
        return new byte[0];
    }

    private void sendCommand(final TransactionBuilder builder, final byte[] payload) {
        builder.write(payload);
    }

    private void sendCommand(final String taskName, final byte[]... payloads) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        for (byte[] payload : payloads) {
            sendCommand(builder, payload);
        }
        builder.queue();
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {

        packetBuffer.clear();
        builder.write(LiveviewProtocol.encodeGetCaps());
        builder.write(LiveviewProtocol.encodeSetTime());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public void onSocketRead(final byte[] data) {
        packetBuffer.put(data);
        packetBuffer.flip();

        while (packetBuffer.hasRemaining()) {
            packetBuffer.mark();

            if (packetBuffer.remaining() < 2) {
                // not enough bytes for min packet
                packetBuffer.reset();
                break;
            }

            final byte messageId = packetBuffer.get();

            final int headerLength = packetBuffer.get() & 0xff;
            if (packetBuffer.remaining() < headerLength) {
                // not enough bytes
                packetBuffer.reset();
                break;
            }

            final byte[] header = new byte[headerLength];
            packetBuffer.get(header);

            final int payloadSize = getLastInt(header);
            if (packetBuffer.remaining() < payloadSize) {
                // not enough bytes
                packetBuffer.reset();
                break;
            }

            final byte[] packet = new byte[payloadSize];
            packetBuffer.get(packet);

            handleLiveviewMessage(parseMessage(messageId, packet));
        }

        packetBuffer.compact();
    }

    private void handleLiveviewMessage(LiveviewMessages.LiveviewMessage message) {
        LOG.debug("Handling message: {}", message);

        if (message instanceof LiveviewMessages.Result result) {
            if (result.code != RESULT.OK) {
                LOG.warn("Received non-OK result: {}", result);
            }
            // Don't ACK result messages, but check for special cases
            if (result.messageId == MSG_DISPLAYPANEL_ACK) { //DISPLAYPANEL USED FOR NOTIFICATIONS
                sendCommand("vibrate", LiveviewProtocol.encodeSetVibrate(100, 200));
            }

        } else if (message instanceof LiveviewMessages.DisplayCapabilities) {
            deviceCapabilities = (LiveviewMessages.DisplayCapabilities) message;
            LOG.info("Device capabilities: {}", deviceCapabilities);

            sendCommand("responding to device capabilities"
                    , message.encodeAckMessage()
                    , LiveviewProtocol.encodeSetMenuSize(MENU_SIZE)
                    , LiveviewProtocol.encodeSetMenuSettings(MENU_VIBRATION_TIME, 0));

        } else if (message instanceof LiveviewMessages.GetMenuItems) {
            LOG.info("GetMenuItems received - sending menu items");

            sendCommand("responding to getmenuitems"
                    , message.encodeAckMessage()
                    , LiveviewProtocol.encodeGetMenuItemResponse(0, true, 1, "Notifications", new byte[]{})
                    , LiveviewProtocol.encodeGetMenuItemResponse(1, false, 0, "Messages", loadIcon())
                    , LiveviewProtocol.encodeGetMenuItemResponse(2, false, 3, "Calls", new byte[]{})
                    , LiveviewProtocol.encodeGetMenuItemResponse(3, false, 0, "Settings", new byte[]{})
                    , LiveviewProtocol.encodeSetMenuSettings(MENU_VIBRATION_TIME, 0));

        } else if (message instanceof LiveviewMessages.GetMenuItem getItem) {
            LOG.info("GetMenuItem request for item {}", getItem.menuItemId);
            sendCommand("responding to GetMenuItem"
                    , message.encodeAckMessage());
        } else if (message instanceof LiveviewMessages.GetTime) {
            long timestamp = Instant.now().getEpochSecond();
            sendCommand("responding to gettime"
                    , message.encodeAckMessage()
                    , LiveviewProtocol.encodeGetTimeResponse(timestamp, true));

        } else if (message instanceof LiveviewMessages.DeviceStatus status) {
            LOG.debug("Device status: {}", status);
            sendCommand("responding to DeviceStatus"
                    , message.encodeAckMessage()
                    , LiveviewProtocol.encodeDeviceStatusAck());
        } else if (message instanceof LiveviewMessages.GetAlert alert) {
            LOG.debug("GetAlert: {}", alert);

            sendCommand("responding to GetAlert"
                    , message.encodeAckMessage()
                    , LiveviewProtocol.encodeGetAlertResponse(
                            20, 4, 15,
                            formatTimestamp(),
                            "HEADER",
                            "Test alert body text",
                            loadIcon()
                    ));

        } else if (message instanceof LiveviewMessages.Navigation nav) {
            LOG.info("Navigation: {} {} (item: {}, wasInAlert: {})",
                    nav.navAction, nav.navType, nav.menuItemId, nav.wasInAlert);

            sendCommand("responding to Navigation"
                    , message.encodeAckMessage()
                    , LiveviewProtocol.encodeNavigationResponse(RESULT.EXIT));
        } else if (message instanceof LiveviewMessages.GetScreenMode screenMode) {
            LOG.debug("Screen mode: brightness={}, auto={}", screenMode.brightness, screenMode.auto);

            sendCommand("responding to GetScreenMode", message.encodeAckMessage());
        } else {
            LOG.debug("Unhandled message type: {}", message.getClass().getSimpleName());

            sendCommand("sending ack for unhandled message", message.encodeAckMessage());
        }
    }
    private byte[] loadIcon() {
        byte[] icon = drawableToByteArray(getContext(), R.drawable.ic_activity_biking, 36, 36);
        return icon;
    }

    private void sendToDevice(final byte[] bytes) {
        if (bytes != null) {
            final TransactionBuilder builder = createTransactionBuilder("send bytes");
            builder.write(bytes);
            builder.queue();
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        sendCommand("find device", LiveviewProtocol.encodeFindDevice(start));
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        sendCommand("notification", LiveviewProtocol.encodeNotification(notificationSpec));
    }

    private int getLastInt(final byte[] array) {
        final ByteBuffer buffer = ByteBuffer.wrap(array, array.length - 4, 4);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        return buffer.getInt();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private String formatTimestamp() {
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.systemDefault());
        return String.format("%02d:%02d", zdt.getHour(), zdt.getMinute());
    }

}
