package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.ActivityProcessor;
import nodomain.freeyourgadget.gadgetbridge.devices.PendingFileProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitImporter;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class FitAsyncProcessor extends ActivityProcessor<File> {
    private static final Logger LOG = LoggerFactory.getLogger(FitAsyncProcessor.class);

    private final List<Uri> localUris;
    public FitAsyncProcessor(final GBDevice gbDevice,
                             final Context context,
                             final List<Uri> localUris) {
        super(gbDevice, context);

        this.localUris = localUris;
    }

    public FitAsyncProcessor(final GBDevice gbDevice,
                             final Context context) {
        this(gbDevice, context, Collections.emptyList());
    }

    @Override
    protected List<File> getEntries() {
        if (!localUris.isEmpty()) {
            final List<File> filesToProcess = new ArrayList<>(localUris.size());

            for (final Uri uri : localUris) {
                final File file;
                try {
                    file = File.createTempFile("activity-files-import", ".bin", mContext.getCacheDir());
                    file.deleteOnExit();
                    FileUtils.copyURItoFile(mContext, uri, file);
                    filesToProcess.add(file);
                } catch (final IOException e) {
                    LOG.error("Failed to create temp file for activity file", e);
                }
            }

            return filesToProcess;
        }

        // Everything from storage
        return safeList("", ".fit");
    }

    @Override
    protected boolean handle(final File file) {
        // The logic below just replicates FitAsyncProcessor

        try {
            final FitImporter fitImporter = new FitImporter(mContext, mGBDevice);
            fitImporter.importFile(file);
        } catch (final Exception ex) {
            LOG.error("Exception while importing {}", file, ex);
            return false; // do not remove from pending files
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final PendingFileProvider pendingFileProvider = new PendingFileProvider(mGBDevice, session);

            pendingFileProvider.removePendingFile(file.getPath());
        } catch (final Exception e) {
            LOG.error("Exception while removing pending file {}", file, e);
            return false;
        }

        return true;
    }
}
