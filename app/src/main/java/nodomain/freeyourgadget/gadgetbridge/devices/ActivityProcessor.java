package nodomain.freeyourgadget.gadgetbridge.devices;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitAsyncProcessor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitImporter;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification;

public abstract class ActivityProcessor<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ActivityProcessor.class);

    private static final AtomicBoolean PARSING_FROM_STORAGE = new AtomicBoolean(false);

    protected final GBDevice mGBDevice;
    protected final Context mContext;
    private final GBProgressNotification mNotification;
    private final Handler mHandler;

    public ActivityProcessor(final GBDevice gbDevice,
                             final Context context) {
        mGBDevice = gbDevice;
        mContext = context;
        mNotification = new GBProgressNotification(context, GB.NOTIFICATION_CHANNEL_ID_TRANSFER);
        mHandler = new Handler(context.getMainLooper());
    }

    /**
     * List the entries to reprocess. These can be files, database entries, etc.
     */
    protected abstract List<T> getEntries();

    protected abstract boolean handle(T entry);

    protected List<File> safeList(final String subdir, final String suffix) {
        final List<File> files;
        try {
            final File externalFilesDir = mGBDevice.getDeviceCoordinator().getWritableExportDirectory(mGBDevice, true);
            final File exportDir = !subdir.isEmpty() ? new File(externalFilesDir, subdir) : externalFilesDir;

            if (!exportDir.exists() || !exportDir.isDirectory()) {
                LOG.error("export directory {} not found", exportDir);
                GB.toast(mContext, "export directory " + exportDir + " not found", Toast.LENGTH_LONG, GB.ERROR);
                return Collections.emptyList();
            }

            files = FileUtils.listRecursive(exportDir, (dir, name) -> name.endsWith(suffix));
            if (files.isEmpty()) {
                LOG.error("No {} files found in {}", suffix, exportDir);
                GB.toast(mContext, "No " + suffix + " files found in " + exportDir, Toast.LENGTH_LONG, GB.ERROR);
                return Collections.emptyList();
            }
        } catch (final Exception e) {
            LOG.error("Failed to parse from storage", e);
            GB.toast(mContext, "Failed to parse from storage", Toast.LENGTH_LONG, GB.ERROR, e);
            return Collections.emptyList();
        }

        return files;
    }

    public void start() {
        if (!PARSING_FROM_STORAGE.compareAndSet(false, true)) {
            GB.toast(mContext, "Already parsing!", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        LOG.info("Parsing all activities from storage");

        GB.toast(mContext, "Check notification for progress", Toast.LENGTH_LONG, GB.INFO);

        new Thread(() -> {
            final List<T> entries = getEntries();

            mNotification.start(R.string.busy_task_processing_files, 0, fitFiles.size());

            try {
                final int[] i = new int[]{0};

                for (final T file : entries) {
                    i[0]++;

                    LOG.debug("Parsing {}", file);

                    mHandler.post(() -> transferNotification.setTotalProgress(i));

                    try {
                        final FitImporter fitImporter = new FitImporter(context, gbDevice);
                        fitImporter.importFile(file);
                    } catch (final Exception ex) {
                        LOG.error("Exception while importing {}", file, ex);
                        continue; // do not remove from pending files
                    }

                    try (DBHandler handler = GBApplication.acquireDB()) {
                        final DaoSession session = handler.getDaoSession();

                        final PendingFileProvider pendingFileProvider = new PendingFileProvider(gbDevice, session);

                        pendingFileProvider.removePendingFile(file.getPath());
                    } catch (final Exception e) {
                        LOG.error("Exception while removing pending file {}", file, e);
                    }
                }
            } catch (final Exception e) {
                LOG.error("Failed to parse from storage", e);
            }

            FitAsyncProcessor.this.handler.post(() -> {
                PARSING_FROM_STORAGE.set(false);
                transferNotification.finish();
                GB.signalActivityDataFinish(device);
            });
        }, "FitAsyncProcessor_" + THREAD_COUNTER.getAndIncrement()).start();
    }
}
