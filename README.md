# walkNcamera
* smart glass는 착용형 디스플레이의 한 종류로 의료분야와 제조업 분야에서 유용하게 사용되고 있음.
* 하지만, 스마트 글래스의 입출력 인터페이스가 일반 사용자가 다루기 쉽지 않음. 예를들어 어떤 앱을 실행하려면 스마트폰에서 앱을 실행하는 것과 다르게 안경을 착용한 상태에서 안경 다리의 터치패드나 버튼을 이용하거나 음성인식 기능을 이용해야 한다. 동시에 작은 화면을 보면서 앱의 실행 여부를 확인해야 한다. 실제로 사용해보면 상당히 불편하고 실패할 가능성이 높으며, 시간이 오래 걸려 사용할 수 없는 수준이다.
* 따라서, 본 소스코드는 smart glass의 IMU센서를 이용하여 사용자의 동작을 인식하고, 그에 따라, 특정 앱을 실행하는 소스코드를 포함한다


# 대상 기기
* Vuzix M400 https://www.vuzix.com/

# 기능
* 안드로이드 플랫폼을 갖춘 스마트 글래스를 부팅 할 시, 자동으로 특정 앱을 실행히키기 위한 각종 코드 (안드로이드 11, API 30기준)
* 스마트 글래스의 물리버튼 사용 지원. 특정 앱을 실행히키기 위한 각종 코드 포함
* 스마트 글래스의 IMU센서를 이용하여 4x4 homogeneous matrix을 획득하여 스마트 글래스의 회전과 이동을 분석. 이를 이용하여 사용자의 동작을 파악하고 의도를 추론하여 특정 앱을 실행하는 코드 포함

# 문제점
* 반응속도가 느림(2023년 8월 26일)
* 안드로이드 버전 11. 따라서 intent를 통해 값을 넘기는 방법을 찾아야 함

# 코드설명
## 재부팅 후 앱 시작(안드로이드 버전 11, M400)

* AndroidManifest.xml 에 다음 코드를 추가한다.
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

* <application> </application> 사이에 서비스와 리시버를 삽입한다. 
~~~xml
<service
android:name=".BootService"
android:enabled="true"
android:exported="true" >
</service>

<receiver
android:name=".BootReceiver"
android:directBootAware="true"
android:enabled="true"
android:exported="true" >
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
~~~
* BootReceiver.java와 BootService.java 를 만든다.

~~~java
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, BootService.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startForegroundService(i);
        }


    }
}
~~~

* 다음은 서비스이다.
~~~java
public class BootService extends Service {
    public BootService() {
    }

    private String myChannelId = "mChannelId";
    private String channelName = "mChannelName";
    private String notificationTitle = "자동실행서비스";
    private String notificationText = "자동실행 서비스 실행 중";
    private int id = 1000;


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(id, createNotification());
        startApp();

    }

    private void createNotificationChannel(){
        NotificationChannel notificationChannel = new NotificationChannel(myChannelId, channelName, NotificationManager.IMPORTANCE_HIGH); // Android 8.0 이상 NotificationChannel 생성 필수
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel); // NotificationManager 를 통해서 Notification Channel 생성
    }

    private Notification createNotification(){ // Notification 만들기
        return new NotificationCompat.Builder(this, myChannelId)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .build();
    }

    private void startApp(){ // 앱 실행하기
        if(Settings.canDrawOverlays(this)){ // 권한이 있을 때
            Intent intent = new Intent(this, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}
~~~

## STEP DETECTOR 만들기
~~~xml
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-feature android:name="android.hardware.sensor.stepdetector" />
<uses-feature android:name="android.hardware.sensor.stepcounter" />
~~~
* 권한체크
~~~ java
if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){

            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_DENIED){

            requestPermissions(new String[]{Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS}, 0);
        }
~~~
~~~java
sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // 디바이스에 걸음 센서의 존재 여부 체크
        if (stepCountSensor == null) {
            Toast.makeText(this, "No Step Sensor", Toast.LENGTH_SHORT).show();
        }
~~~
* step을 감지하면 intent를 이용하여 다른 앱이나 activity를 실행할 수 있음
~~~java
public void onStart() {
        super.onStart();
        if(stepCountSensor !=null) {
            // 센서 속도 설정
            // * 옵션
            // - SENSOR_DELAY_NORMAL: 20,000 초 딜레이
            // - SENSOR_DELAY_UI: 6,000 초 딜레이
            // - SENSOR_DELAY_GAME: 20,000 초 딜레이
            // - SENSOR_DELAY_FASTEST: 딜레이 없음
            //
            sensorManager.registerListener(this,stepCountSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        // 걸음 센서 이벤트 발생시

        if(myEvent==true) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {

                if (event.values[0] == 1.0f) {
                    // 센서 이벤트가 발생할때 마다 걸음수 증가
                    currentSteps++;
                    stepCountView.setText(String.valueOf(currentSteps));
                    Log.d("walk", "step");

                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    startActivity(intent);
                    myEvent = false;

                }

            }
        }

    }
~~~
* Vuzix M400의 물리버튼 3개의 입력을 받아들이는 코드
~~~java
public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d("KeyUP Event", "RIGHT down");
                if(myEvent==true){
                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    startActivity(intent);
                    myEvent = false;
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d("KeyUP Event", "LEFT down");
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                Log.d("KeyUP Event", "CENTER down");
                return true;

        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }
~~~
# 참고자료
* 안드로이드 부팅 후 앱 시작 https://blog.naver.com/effortive_rich/223130649521

# 주의사항
* 앱을 설치 후 overlay를 허용해야 함
