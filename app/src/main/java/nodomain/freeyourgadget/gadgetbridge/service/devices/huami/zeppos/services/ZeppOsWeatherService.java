/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.weather.Weather;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * @noinspection unused
 */
public class ZeppOsWeatherService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsWeatherService.class);

    private static final short ENDPOINT = 0x000e;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_LIST_SET = 0x03;
    private static final byte CMD_LIST_SET_ACK = 0x04;
    private static final byte CMD_LIST_GET = 0x05;
    private static final byte CMD_LIST_RET = 0x06;
    private static final byte CMD_SINGLE_SET = 0x07;
    private static final byte CMD_SINGLE_SET_ACK = 0x08;
    private static final byte CMD_CURRENT_DEFAULT_SET = 0x09;
    private static final byte CMD_CURRENT_DEFAULT_SET_ACK = 0x0a;
    private static final byte CMD_CURRENT_DEFAULT_GET = 0x0b;
    private static final byte CMD_CURRENT_DEFAULT_RET = 0x0c;

    private static final byte FLAG_ALARMS = 1;
    private static final byte FLAG_CURRENT_LOCATION = 2;
    private static final byte FLAG_DEFAULT_LOCATION = 4;

    private int version = -1;

    public ZeppOsWeatherService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                version = payload[1] & 0xff;
                if (version != 2) {
                    LOG.warn("Unsupported weather service version {}", version);
                    return;
                }
                LOG.info("Weather service version={}", version);
                onSendWeather();
                break;
            case CMD_LIST_SET_ACK:
                LOG.info("Weather list set ACK, status = {}", payload[1]);
                return;
            case CMD_SINGLE_SET_ACK:
                LOG.info("Weather single set ACK, status = {}", payload[1]);
                return;
            case CMD_CURRENT_DEFAULT_SET_ACK:
                LOG.info("Weather current/default set ACK, status = {}", payload[1]);
                return;
            case CMD_LIST_RET:
                parseWeatherLocationList(payload);
                return;
            case CMD_CURRENT_DEFAULT_RET:
                parseCurrentDefault(payload);
                return;
            default:
                LOG.warn("Unexpected weather byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void onSendWeather() {
        if (version != 2) {
            LOG.warn("Ignoring weather update - unsupported weather service version {}", version);
            return;
        }

        final List<WeatherSpec> weatherSpecs = Weather.getWeatherSpecs()
                .stream().filter(weatherSpec -> weatherSpec.getLocation() != null)
                .limit(5)
                .collect(Collectors.toList());
        if (weatherSpecs.isEmpty()) {
            LOG.warn("No weather in instance");
            return;
        }

        LOG.debug("Will send {} weather locations", weatherSpecs.size());

        WeatherSpec currentLocation = null;
        WeatherSpec defaultLocation = weatherSpecs.get(0);

        for (final WeatherSpec weatherSpec : weatherSpecs) {
            if (weatherSpec.isCurrentLocation() == 1) {
                currentLocation = weatherSpec;
                LOG.debug("Will send ('{}', '{}') as current weather", weatherKey(currentLocation), currentLocation.getLocation());
                break;
            }
        }

        LOG.debug("Will send ('{}', '{}') as default weather", weatherKey(defaultLocation), defaultLocation.getLocation());

        // Weather is not sent directly to the bands, they send HTTP requests for each location.
        // When we have a weather update, update the full list on the band.
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(CMD_LIST_SET);
            baos.write(weatherSpecs.size());

            for (final WeatherSpec weatherSpec : weatherSpecs) {
                baos.write(weatherKey(weatherSpec).getBytes(StandardCharsets.UTF_8));
                baos.write((byte) 0x00);
                baos.write(Objects.requireNonNull(weatherSpec.getLocation()).getBytes(StandardCharsets.UTF_8));
                baos.write((byte) 0x00);
            }

            write("set weather list", baos.toByteArray());
        } catch (final Exception e) {
            LOG.error("Failed to set weather location", e);
        }

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(CMD_CURRENT_DEFAULT_SET);

            byte flags = FLAG_ALARMS | FLAG_DEFAULT_LOCATION;

            if (currentLocation != null) {
                flags |= FLAG_CURRENT_LOCATION;
            }

            baos.write(flags);
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x00); // ?
            baos.write((byte) 0x01); // alarm ?

            // Current location
            if (currentLocation != null) {
                baos.write(weatherKey(currentLocation).getBytes(StandardCharsets.UTF_8));
                baos.write((byte) 0x00);
                baos.write(Objects.requireNonNull(currentLocation.getLocation()).getBytes(StandardCharsets.UTF_8));
                baos.write((byte) 0x00);
            }

            // Default location
            baos.write(weatherKey(defaultLocation).getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00);
            baos.write(Objects.requireNonNull(defaultLocation.getLocation()).getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00);

            write("set weather current and default", baos.toByteArray());
        } catch (final Exception e) {
            LOG.error("Failed to set weather location", e);
        }
    }

    private void createSingleLocation(final WeatherSpec weatherSpec) {
        LOG.debug("Sending ('{}', '{}') as secondary weather", weatherKey(weatherSpec), weatherSpec.getLocation());
        setSingleLocation(weatherKey(weatherSpec), weatherSpec.getLocation(), (byte) 0x01);
    }

    private void deleteSingleLocation(final String key, final String name) {
        LOG.debug("Deleting ('{}', '{}') as secondary weather", key, name);
        setSingleLocation(key, name, (byte) 0x00);
    }

    private void setSingleLocation(final String key, final String name, final byte op) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(CMD_SINGLE_SET);

            // Current location
            baos.write(key.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00);
            baos.write(name.getBytes(StandardCharsets.UTF_8));
            baos.write((byte) 0x00);
            baos.write(op);

            write("set single weather weather location", baos.toByteArray());
        } catch (final Exception e) {
            LOG.error("Failed to set single weather weather location", e);
        }
    }

    private void parseWeatherLocationList(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard first byte

        final int status = buf.get() & 0xff;
        if (status != 1) {
            LOG.warn("Unexpected weather list status {}", status);
            return;
        }

        final int numLocations = buf.get() & 0xff;
        LOG.info("Got weather list from device, {} locations", numLocations);

        for (int i = 0; i < numLocations; i++) {
            final String key = StringUtils.untilNullTerminator(buf);
            final String name = StringUtils.untilNullTerminator(buf);

            LOG.debug("Weather[{}]: key={}, name={}", i, key, name);
        }

        if (buf.position() < buf.limit()) {
            LOG.warn("There are {} data bytes still in the buffer", (buf.limit() - buf.position()));
        }
    }

    private void parseCurrentDefault(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard first byte

        final int status = buf.get() & 0xff;
        if (status != 1) {
            LOG.warn("Unexpected weather list status {}", status);
            return;
        }

        final byte flags = buf.get();

        buf.get(new byte[3]); // 0 0 0

        if ((flags & FLAG_ALARMS) != 0) {
            buf.get(); // 1?
        }

        if ((flags & FLAG_CURRENT_LOCATION) != 0) {
            final String key = StringUtils.untilNullTerminator(buf);
            final String name = StringUtils.untilNullTerminator(buf);

            LOG.debug("Current location: key={}, name={}", key, name);
        }

        if ((flags & FLAG_DEFAULT_LOCATION) != 0) {
            final String key = StringUtils.untilNullTerminator(buf);
            final String name = StringUtils.untilNullTerminator(buf);

            LOG.debug("Default weather: key={}, name={}", key, name);
        }
    }

    private static String weatherKey(final WeatherSpec weatherSpec) {
        if (weatherSpec.getLatitude() != 0 && weatherSpec.getLongitude() != 0) {
            return String.format(Locale.ROOT, "%.3f,%.3f,xiaomi-accu:1234567890", weatherSpec.getLatitude(), weatherSpec.getLongitude());
        } else {
            return "1.234,-5.678,xiaomi_accu:1234567890";
        }
    }
}
