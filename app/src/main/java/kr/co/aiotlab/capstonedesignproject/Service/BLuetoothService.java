package kr.co.aiotlab.capstonedesignproject.Service;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import kr.co.aiotlab.capstonedesignproject.Activities.BluetoothSelect_Activity;
import kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity;
import kr.co.aiotlab.capstonedesignproject.R;

import static kr.co.aiotlab.capstonedesignproject.Activities.BluetoothSelect_Activity.dialog;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.FCM_MESSAGE_URL;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.SERVER_KEY;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.area1;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.area2;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.area3;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.area4;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.test;
import static kr.co.aiotlab.capstonedesignproject.Activities.Main_Activity.txt_autoGPS_state;

public class BLuetoothService extends Service {

    // TAG 서정
    private static final String TAG = "BluetoothService";
    // 플래그로 디바이스 구분 (Falling1 = 낙상 디바이스, Falling2 = 추락 디바이스 Arduino nano 33 ble sense)
    private static final String FALLING_DEVICE_1 = "FALLING_1";
    private static final String FALLING_DEVICE_2 = "FALLING_2";

    // 기기별 UUID 설정
    // 낙상 UUID
    private static final UUID ServiceUUID_Falling_1 = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    private static final UUID CharacteristicUUID_Noti_Falling_1 = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB");
    private static final UUID CharacteristicUUID_Write_Falling_1 = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB");
    //추락 UUID
    private static final UUID ServiceUUID_Falling_2 = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    private static final UUID CharacteristicUUID_Noti_Falling_2 = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");

    // 현재 날짜 받아오기
    private SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    // 배터리 잔량 요청코드
    byte[] batteryRequest_bytes = {0x5A, 0x01, (byte) 0xB1, 0x71, (byte) 0xA5};

    // 초기화
    private DeviceMirror deviceMirrorCustom;
    private BluetoothGattChannel bluetoothGattChannel_Write;

    private String device_flag;
    private String accelerometerData;
    private double latitude, longitude, altitude;

    // 블루투스
    ViseBle viseBleInstance = ViseBle.getInstance();
    Thread thread, thread_request;

    // Firebase 세팅
    private FirebaseDatabase mDatabase;
    private FirebaseUser mUser;

    // 중복되는 이메일 데이터
    ArrayList<String> array_email = new ArrayList<>();
    // 중복 제거된 이메일 데이터
    ArrayList<String> array_email2 = new ArrayList<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /** 서비스 노티피케이션 **/
        Intent notificationIntent = new Intent(this, Main_Activity.class);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(this, 0, notificationIntent, 0);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.activity_bluetooth);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 29) {
            String CHANNEL_ID = "falling_service_channel_Id";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "SnowDeer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);

            builder.setSmallIcon(R.drawable.safety_jacket_icon)
                    .setContentIntent(pendingIntent);
//
        } else if (Build.VERSION.SDK_INT >= 29) {
            String CHANNEL_ID = "falling_service_channel_Id";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "SnowDeer Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.drawable.safety_jacket_icon)
                    .setContent(remoteViews)
                    .setContentIntent(pendingIntent);
        } else {
            builder = new NotificationCompat.Builder(this);
            builder.setContentIntent(pendingIntent);
        }
        startForeground(1, builder.build());

        /** Firebase database **/
        mDatabase = FirebaseDatabase.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();

        /** 스레드 구현 **/
        if (thread == null) {
            thread = new Thread(new BluetoothThreading());
            thread.start();

        }
        return START_STICKY;
    }

    /** 블루투스 관련 스레드 클레스 **/
    class BluetoothThreading implements Runnable {

        @Override
        public void run() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BluetoothSelect_Activity.showProgressDialog("장치 검색중...");
                    viseBleInstance.startScan(falling_ScanCallBack);

                    /**  내 위치 리스너 **/
                    // GPS 연동을 위한 권한 체크
                    if (Build.VERSION.SDK_INT >= 23 &&
                            ContextCompat.checkSelfPermission(getApplicationContext(),
                                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) BluetoothSelect_Activity.getContext(),
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                0);
                    } else {
                        // 내위치 검색
                        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        String provider = location.getProvider();
                        double now_longitude = location.getLongitude();
                        double now_latitude = location.getLatitude();
                        double now_altitude = location.getAltitude();

                        latitude = now_latitude;
                        altitude = now_altitude;
                        longitude = now_longitude;
                        Log.d(TAG, "1 lat: " + latitude + "long: " + longitude + "alti: " + altitude);

                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                3000,
                                1,
                                gpsLocationListener);
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                3000,
                                1,
                                gpsLocationListener);
                    }
                }
            });

        }
    }

    /** 배터리 용량 요청 스레드 **/
    class RequestBattery implements Runnable {

        @Override
        public void run() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Write 설정
                    bluetoothGattChannel_Write = new BluetoothGattChannel.Builder()
                            .setServiceUUID(ServiceUUID_Falling_1)
                            .setCharacteristicUUID(CharacteristicUUID_Write_Falling_1)
                            .setDescriptorUUID(null)
                            .setPropertyType(PropertyType.PROPERTY_WRITE)
                            .setBluetoothGatt(deviceMirrorCustom.getBluetoothGatt())
                            .builder();
                    deviceMirrorCustom.bindChannel(iBleCallBack_Falling_1_Write, bluetoothGattChannel_Write);
                    deviceMirrorCustom.writeData(batteryRequest_bytes);

                }
            });
        }
    }

    /** 낙상(Falling1 device)의 Write에 대한 콜백 (내가 무엇을 썼는지 되돌아옴) **/
    IBleCallback iBleCallBack_Falling_1_Write = new IBleCallback() {
        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {

        }

        @Override
        public void onFailure(BleException exception) {

        }
    };
    /** 블루투스 연결 되었을 때 CallBack **/
    IConnectCallback iConnectCallback = new IConnectCallback() {
        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            deviceMirrorCustom = deviceMirror;  // 할당
            threadHandlerToast("연결되었습니다.");    // 연결됨 토스트 핸들러

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            txt_autoGPS_state.setText("서비스가 실행중입니다.");
                            txt_autoGPS_state.setTextColor(Color.BLUE);
                            dialog.cancel();    // 다이얼로그 종료

                            // 블루투스 이름이 AirBag 일 경우 (낙상 센서)
                            if (deviceMirror.getBluetoothGatt().getDevice().getName().equals("AirBag")) {
                                BluetoothSelect_Activity.threadHandlerBLEState("Slippery", "ON");   // 블루투스 실행중 이미지 표시
                                SharedPreferences sharedPreferences_state = getApplicationContext().getSharedPreferences("STATE", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences_state.edit();
                                editor.putBoolean("BLUETOOTH_SLIPPERY", true);  // 블루투스 상태 저장
                                editor.apply();
                            }
                            // 블루투스 이름 AccelerometerMonitor 일 경우 (추락 센서)
                            else if (deviceMirror.getBluetoothGatt().getDevice().getName().equals("AccelerometerMonitor")) {
                                BluetoothSelect_Activity.threadHandlerBLEState("Falling", "ON");    // 블루투스 실행중 이미지 표시
                                SharedPreferences sharedPreferences_state = getApplicationContext().getSharedPreferences("STATE", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences_state.edit();
                                editor.putBoolean("BLUETOOTH_FALLING", true);   // 블루투스 상태 저장
                                editor.apply();
                            }

                        }
                    });
                }
            }).start();

            /** 낙상센서 UUID 연결 **/
            if (device_flag.equals(FALLING_DEVICE_1)) {
                // Notification 설정
                BluetoothGattChannel bluetoothGattChannel_Noti = new BluetoothGattChannel.Builder()
                        .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                        .setServiceUUID(ServiceUUID_Falling_1)
                        .setCharacteristicUUID(CharacteristicUUID_Noti_Falling_1)
                        .setDescriptorUUID(null)
                        .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                        .builder();
                deviceMirror.bindChannel(iBleCallBack_Falling_1_Noti, bluetoothGattChannel_Noti);
                deviceMirror.registerNotify(false);
                deviceMirror.setNotifyListener(bluetoothGattChannel_Noti.getGattInfoKey(), iBleCallBack_Falling_1_Noti);
            }
            /** 추락센서 UUID 연결 **/
            else if (device_flag.equals(FALLING_DEVICE_2)) {
                // Notification 설정
                BluetoothGattChannel bluetoothGattChannel_Noti = new BluetoothGattChannel.Builder()
                        .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                        .setServiceUUID(ServiceUUID_Falling_2)
                        .setCharacteristicUUID(CharacteristicUUID_Noti_Falling_2)
                        .setDescriptorUUID(null)
                        .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                        .builder();
                deviceMirror.bindChannel(iBleCallBack_Falling_2_Noti, bluetoothGattChannel_Noti);
                deviceMirror.registerNotify(false);
                deviceMirror.setNotifyListener(bluetoothGattChannel_Noti.getGattInfoKey(), iBleCallBack_Falling_2_Noti);

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                int year = date.getYear() + 2000 - 100;
                int month = date.getMonth() + 1;
                int day = date.getDate();
                int hour = date.getHours();
                int minute = date.getMinutes();
                int second = date.getSeconds();

                String timestamp = year + "_" + month + "_" + day + "_" + hour + "_" + minute;
                // 날짜 저장
                mDatabase.getReference(mUser.getEmail().replace(".", "_"))
                        .child("date_list")
                        .push()
                        .child("timestamp")
                        .setValue(timestamp);
            }
        }

        @Override
        public void onConnectFailure(BleException exception) {
            stopAll();
        }

        @Override
        public void onDisconnect(boolean isActive) {
            stopAll();
        }
    };

    /** 낙상 Notification 콜백 */
    IBleCallback iBleCallBack_Falling_1_Noti = new IBleCallback() {
        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            String receivedData = Arrays.toString(data);
            try {
                // 낙상이 발생했을때 콜백
                if (receivedData.equals("[90, 1, -95, 113, -91]")) {
                    threadHandlerToast("낙상 발생");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // 낙상이 발생했으므로 FCM 푸시 준비
                                    sendMessageService();
                                }
                            });
                        }
                    }).start();
                }
                if (receivedData.equals("[90, 1, -94, 113, -91]")) {
                    threadHandlerToast("에어백 펑");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // 일단 데이터가 들어오는 데에 시간이 좀 걸려서 1초 딜레이를 두었지만 더 좋은 방법이 있으면 수정해야함
                                    triggerMessage();
                                }
                            }, 1000);
                        }
                    }).start();
                }
                // 배터리 잔량이 돌아오는 바이트 코드 공식 : { 5A, 01, B1, 잔량(0~100), A5 }
                if ((data[0] == 90) && (data[1] == 1) && (data[2] == -79) && (data[4] == -91)) {
                    threadHandlerToast("배터리 잔량 = " + Arrays.toString(new byte[]{data[3]}) + "%");
                } else if (receivedData.equals("[1, 0]")) {
                    // 쓰레기값
                }

            } catch (Exception e) {

            }
        }

        @Override
        public void onFailure(BleException exception) {
            stopAll();
        }
    };

    /** 낙상 Notification CallBack */
    IBleCallback iBleCallBack_Falling_2_Noti = new IBleCallback() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        int year = date.getYear() + 2000 - 100;
        int month = date.getMonth() + 1;
        int day = date.getDate();
        int hour = date.getHours();
        int minute = date.getMinutes();
        int second = date.getSeconds();
        String timestamp = year + "_" + month + "_" + day + "_" + hour + "_" + minute;
        String timestamp_detail = timestamp + "_" + second;

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            // 콜백해서 들어온 데이터 String 값
            accelerometerData = Arrays.toString(data);
            // 바이트 코드로 들어오는 데이터를 비교할 수 있도록 Integer형으로 변경
            int int_accelData = Integer.parseInt(accelerometerData.replace("[", "").replace("]", ""));
            // 테스트 ...
            test.setText(accelerometerData);
            // 데이터 저장
            mDatabase.getReference(mUser.getEmail().replace(".", "_"))
                    .child("chart_data")
                    .child(timestamp)
                    .push()
                    .child("data")
                    .setValue(timestamp_detail + ":" + int_accelData);

            // RMS Threshold (일정량 이상의 가속도값이 검지되면 FCM알림)
            if (int_accelData > 65) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                sendMessageService();
                            }
                        });
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 일단 데이터가 들어오는 데에 시간이 좀 걸려서 1초 딜레이를 두었지만 더 좋은 방법이 있으면 수정해야함
                                triggerMessage();
                            }
                        }, 1000);
                    }
                }).start();
            }
        }

        @Override
        public void onFailure(BleException exception) {
            stopAll();
        }
    };
    /** 낙상 스캔 콜백 2번째 */
    IScanCallback falling_IScanCallBack = new IScanCallback() {
        @Override
        public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Falling1(낙상센서) 발견하면
                            // 연결되었을 때 계속 검색
                            try {
                                // AirBag 이름의 디바이스가 스캔되면,
                                if (bluetoothLeDevice.getName().equals("AirBag")) {
                                    // MAC address로 연결
                                    viseBleInstance.connectByMac(bluetoothLeDevice.getAddress(), iConnectCallback);
                                    // Flag 올려!
                                    device_flag = FALLING_DEVICE_1;
                                    // 스캔 연결시도되기 때문에 스캔 중단
                                    viseBleInstance.stopScan(falling_ScanCallBack);
                                }
                                // AccelerometerMonitor 이름의 디바이스가 스캔되면,
                                else if (bluetoothLeDevice.getName().equals("AccelerometerMonitor")) {
                                    viseBleInstance.connectByMac(bluetoothLeDevice.getAddress(), iConnectCallback);
                                    // Flag 올려!
                                    device_flag = FALLING_DEVICE_2;
                                    viseBleInstance.stopScan(falling_ScanCallBack);
                                }
                            } catch (Exception e) {

                            }
                        }
                    });
                }
            }).start();
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

        }

        @Override
        public void onScanTimeout() {
            threadHandlerToast("스캔 시간을 초과하였습니다. 다시 시도해주세요");
        }
    };
    /** 낙상 스캔 콜백 */
    ScanCallback falling_ScanCallBack = new ScanCallback(falling_IScanCallBack);

    /** 토스트 메시지 핸들러에 담아서 보내기 */
    public void threadHandlerToast(final String string) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** 위급알람 보내기  */
    void sendMessageService() {
        // 데이터베이서에서 유저가 등록한 이메일 주소 불러오기
        DatabaseReference mGetPersonData = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list");
        mGetPersonData.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Map<String, Object> email_data = (Map<String, Object>) dataSnapshot.getValue();
                String name = null;
                if (email_data != null) {
                    name = email_data.get("email").toString();
                }
                // 이메일 arrayList에 담기
                array_email.add(name);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    /** FCM 메시지 전송 메소드 */
    private void sendPostToFCM(final String email, final String message, final double latitude, final double longitude, final double altitude, String phoneNum) {
        // 사용자 이름 정보 받아오기
        final String[] userName = new String[1];
        // DB에 접근해서 유저가 보내기로 설정한 이메일 정보 가져오기
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("user_name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userName[0] = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        // Token 찾아서 메시지 보내기
        mDatabase.getReference(email).child("token").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final String userData_fcm = dataSnapshot.getValue(String.class);
                //Toast.makeText(MainActivity.this, userData, Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject root = new JSONObject();
                            JSONObject notification = new JSONObject();
                            notification.put("body", message + "\n위도 :" + latitude + "경도 :" + longitude);   // body에 내용 담기
                            notification.put("title", userName[0] + "님으로부터 온 메시지");                     // Notification 알림 타이틀 설정
                            notification.put("sound", "emergencysound");                                       // 소리 설정
                            notification.put("android_channel_id", "1008");                                    // Android O에서 새로 추가된 알림의 채널 ID
                            notification.put("vibrate", "true");                                               // 진동 설정
                            notification.put("icon", "myicon");                                                // 아이콘 설정
                            notification.put("click_action", "OPEN_ACTIVITY");                                 // FCM Noti 클릭시 이동할 액티비티

                            root.put("notification", notification);
                            root.put("to", userData_fcm);

                            URL url = new URL(FCM_MESSAGE_URL);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setDoOutput(true);
                            conn.setDoInput(true);
                            conn.addRequestProperty("Authorization", "key=" + SERVER_KEY);
                            conn.setRequestProperty("Accept", "application/json");
                            conn.setRequestProperty("Content-type", "application/json");
                            OutputStream os = conn.getOutputStream();
                            os.write(root.toString().getBytes("utf-8"));
                            os.flush();
                            conn.getResponseCode();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // database에 시간정보, 위도, 경도 저장
        // 시간
        mDatabase.getReference(email).child("accident_data").child("timestamp").setValue(getTime());
        // 위도
        mDatabase.getReference(email).child("accident_data").child("latitude").setValue(String.valueOf(latitude));
        // 경도
        mDatabase.getReference(email).child("accident_data").child("longitude").setValue(String.valueOf(longitude));
        // 고도
        mDatabase.getReference(email).child("accident_data").child("altitude").setValue(String.valueOf(altitude));
        // 번호
        mDatabase.getReference(email).child("accident_data").child("phone").setValue(phoneNum);
        // 주소
        mDatabase.getReference(email).child("accident_data").child("address").setValue(area1 + " " + area2 + " " + area3 + " " + area4);
        // 사고자 정보
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("user_name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mDatabase.getReference(email).child("accident_data").child("name").setValue(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    // 유저가 설정한 값들을 바탕으로 데이터 전송
    void triggerMessage() {
        // 저장된 메시지 가져오기
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("MESSAGE", MODE_PRIVATE);
        final String message = sharedPreferences.getString("LINE_MESSAGE", "위급상황입니다.");
        // 저장된 번호 가져오기
        SharedPreferences phoneNum_shared = getApplicationContext().getSharedPreferences("PHONE", MODE_PRIVATE);
        final String phoneNum = phoneNum_shared.getString("NUMBER", "");

        for (int i = 0; i < array_email.size(); i++) {
            if (!array_email2.contains(array_email.get(i)))
                array_email2.add(array_email.get(i));
        }
        for (int i = 0; i < array_email2.size(); i++) {
            sendPostToFCM(array_email2.get(i).replace(".", "_"), message, latitude, longitude, altitude, phoneNum);
        }
    }

    /**------------------------   stop , destroy  --------------------------**/
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Service Stop 호출시
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (thread_request != null) {
            thread_request.interrupt();
            thread_request = null;
        }
    }

    public void stopAll() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        threadHandlerToast("연결이 끊겼습니다.");
                        ViseBle.getInstance().disconnect();
                        BluetoothSelect_Activity.threadHandlerBLEState("Falling", "OFF");
                        BluetoothSelect_Activity.threadHandlerBLEState("Slippery", "OFF");
                        SharedPreferences sharedPreferences_state = getApplicationContext().getSharedPreferences("STATE", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences_state.edit();
                        editor.putBoolean("BLUETOOTH_SLIPPERY", false);
                        editor.putBoolean("BLUETOOTH_FALLING", false);
                        editor.commit();
                        txt_autoGPS_state.setText("서비스가가 중지됨");
                        txt_autoGPS_state.setTextColor(Color.RED);
                        stopForeground(true);
                    }
                });
            }
        }).start();
    }

    /** 위치 정보 리스너 **/
    final LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            String provider = location.getProvider();
            double now_longitude = location.getLongitude();
            double now_latitude = location.getLatitude();
            double now_altitude = location.getAltitude();

            latitude = now_latitude;
            longitude = now_longitude;
            altitude = now_altitude;
            Log.d(TAG, "2lat: " + latitude + "long: " + longitude + "alti: " + altitude);

            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    3000,
                    1,
                    gpsLocationListener);

            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    3000,
                    1,
                    gpsLocationListener);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    public String getTime() {
        long mNow = System.currentTimeMillis();
        Date mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

}
