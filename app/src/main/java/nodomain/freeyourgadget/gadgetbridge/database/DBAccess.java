/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * This class uses a dedicated background thread to perform database operations
 * and delivers results back to the UI thread via a callback.
 *
 * @param <T> The type of the result expected from the background operation.
 */
public abstract class DBAccess<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DBAccess.class);

    // 1. A single-threaded executor to ensure all database operations are sequential.
    private static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // 2. A handler to post results back to the application's main thread.
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final Context mContext;
    private final String mTask;

    public DBAccess(String task, Context context) {
        // Use the application context to prevent memory leaks from Activities/Fragments
        this.mContext = context.getApplicationContext();
        this.mTask = task;
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * This method is executed on a background thread. Implement the database
     * logic here.
     *
     * @param handler The DBHandler to perform database operations.
     * @return The result of the background operation.
     * @throws Exception if an error occurs during the operation.
     */
    protected abstract T doInBackground(DBHandler handler) throws Exception;

    /**
     * This method is called on the UI thread before the background task starts.
     * Subclasses can override this to, for example, show a progress indicator.
     */
    protected void onPreExecute() {
    }

    /**
     * Executes the database task. The results are delivered via the provided callback.
     *
     * @param callback The callback to handle completion or errors on the UI thread.
     */
    public void execute(Callback<T> callback) {
        // Run pre-execution logic on the current thread (which should be the UI thread).
        onPreExecute();

        // Submit the background task to the executor.
        databaseExecutor.execute(() -> {
            try (DBHandler db = GBApplication.acquireDB()) {
                final T result = doInBackground(db);
                mainThreadHandler.post(() -> callback.onComplete(result));
            } catch (Exception e) {
                LOG.error("Error during DBAccess for {}", mTask, e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * A callback interface to receive results from the DBAccess task.
     *
     * @param <T> The type of the result.
     */
    public interface Callback<T> {
        /**
         * Called on the UI thread when the task completes successfully.
         * @param result The result from doInBackground.
         */
        void onComplete(T result);

        /**
         * Called on the UI thread when the task fails.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * A helper method to display a standardized error toast.
     * @param error The error to display.
     */
    public void displayError(Throwable error) {
        String message = getContext().getString(R.string.dbaccess_error_executing, error.getLocalizedMessage());
        GB.toast(getContext(), message, Toast.LENGTH_LONG, GB.ERROR, error);
    }
}

