package wangdaye.com.geometricweather.background.polling.services.basic;

import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import wangdaye.com.geometricweather.GeometricWeather;
import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.common.basic.models.Location;
import wangdaye.com.geometricweather.common.basic.models.weather.Weather;
import wangdaye.com.geometricweather.db.DatabaseHelper;

/**
 * Foreground update service.
 * */

public abstract class ForegroundUpdateService extends UpdateService {

    private int mFinishedCount;

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    GeometricWeather.NOTIFICATION_CHANNEL_ID_BACKGROUND,
                    GeometricWeather.getNotificationChannelName(
                            this, GeometricWeather.NOTIFICATION_CHANNEL_ID_BACKGROUND),
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setShowBadge(false);
            channel.setLightColor(ContextCompat.getColor(this, R.color.colorPrimary));
            NotificationManagerCompat.from(this).createNotificationChannel(channel);
        }

        mFinishedCount = 0;
        startForeground(
                getForegroundNotificationId(),
                getForegroundNotification(
                        DatabaseHelper.getInstance(this).countLocation()
                ).build()
        );

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // version O.
        stopForeground(true);
        NotificationManagerCompat.from(this).cancel(getForegroundNotificationId());
    }

    @Override
    public void stopService(boolean updateFailed) {
        stopForeground(true);
        NotificationManagerCompat.from(this).cancel(getForegroundNotificationId());
        super.stopService(updateFailed);
    }

    public NotificationCompat.Builder getForegroundNotification(int total) {
        return new NotificationCompat.Builder(this, GeometricWeather.NOTIFICATION_CHANNEL_ID_BACKGROUND)
                .setSmallIcon(R.drawable.ic_running_in_background)
                .setContentTitle(getString(R.string.geometric_weather))
                .setContentText(getString(R.string.feedback_updating_weather_data) + " (" + (mFinishedCount + 1) + "/" + total + ")")
                .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setProgress(0, 0, true)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setAutoCancel(false)
                .setOngoing(false);
    }

    public abstract int getForegroundNotificationId();

    @Override
    public void responseSingleRequest(@NonNull Location location, @Nullable Weather old,
                                      boolean succeed, int index, int total) {
        super.responseSingleRequest(location, old, succeed, index, total);

        mFinishedCount ++;
        if (mFinishedCount != total) {
            NotificationManagerCompat.from(this).notify(
                    getForegroundNotificationId(),
                    getForegroundNotification(total).build()
            );
        }
    }
}
