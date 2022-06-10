//in menifest
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>

        <service
            android:name=".LocalService"
            android:foregroundServiceType="location"
            android:stopWithTask="false" />



//in main ac
    private void startTraking() {
        if (isMyServiceRunning(LocalService.class)) {
            stopService(new Intent(MainAc.this, LocalService.class));
        }
        ContextCompat.startForegroundService(this, new Intent(MainAc.this, LocalService.class));
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


//----------------------------------------
public class EndlessService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate");
        if (handler != null) handler.removeCallbacks(runnable);
        update();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand");

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainAc.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "CHANNEL_ID",
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    Handler handler = new Handler();
    Runnable runnable = () -> {
        if (checkPermissions()) {
            doTask();
        }
        update();
    };

    private void update() {
        handler.postDelayed(runnable, 3000L);
    }

    private void doTask() {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        log("onTaskRemoved");
    }

    @Override
    public IBinder onBind(Intent intent) {return null;}

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
