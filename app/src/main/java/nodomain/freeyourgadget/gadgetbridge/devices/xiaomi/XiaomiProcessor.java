package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.ActivityProcessor;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class XiaomiProcessor extends ActivityProcessor<File> {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiProcessor.class);

    public XiaomiProcessor(final GBDevice gbDevice, final Context context) {
        super(gbDevice, context);
    }

    @Override
    protected List<File> getEntries() {
        return safeList("rawFetchOperations", ".bin");
    }

    @Override
    protected boolean handle(final File activityFile) {
        // The logic below just replicates XiaomiActivityFileFetcher

        final byte[] data;
        try (InputStream in = new FileInputStream(activityFile)) {
            data = FileUtils.readAll(in, 999999);
        } catch (final IOException ioe) {
            LOG.error("Failed to read {}", activityFile, ioe);
            return false;
        }

        final byte[] fileIdBytes = Arrays.copyOfRange(data, 0, 7);
        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(fileIdBytes);

        final XiaomiActivityParser activityParser = XiaomiActivityParser.create(fileId);
        if (activityParser == null) {
            LOG.warn("Failed to find parser for {}", fileId);
            return false;
        }

        try {
            if (activityParser.parse(mContext, mGBDevice, fileId, data)) {
                LOG.info("Successfully parsed {}", fileId);
            } else {
                LOG.warn("Failed to parse {}", fileId);
                return false;
            }
        } catch (final Exception ex) {
            LOG.error("Exception while parsing {}", fileId, ex);
            return false;
        }

        return true;
    }
}
