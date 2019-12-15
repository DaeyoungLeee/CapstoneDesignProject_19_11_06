package kr.co.aiotlab.capstonedesignproject.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.vise.baseble.ViseBle;

import kr.co.aiotlab.capstonedesignproject.R;
import kr.co.aiotlab.capstonedesignproject.Service.BLuetoothService;

public class BluetoothSelect_Activity extends AppCompatActivity implements View.OnClickListener {

    CardView card_falling_1, card_falling_2;
    Button btn_close_bluetooth;

    public static ImageView img_ble_state_slippery, img_ble_state_falling;

    private static final String TAG = "BluetoothActivity";
    public static ProgressDialog dialog;

    public static Context mContext;

    Intent intentBluetoothService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mContext = this;

        intentBluetoothService = new Intent(this, BLuetoothService.class);

        card_falling_1 = findViewById(R.id.card_falling1);
        card_falling_2 = findViewById(R.id.card_falling2);
        btn_close_bluetooth = findViewById(R.id.btn_close_bluetooth);
        img_ble_state_slippery = findViewById(R.id.img_bluetoothState_slippery);
        img_ble_state_falling = findViewById(R.id.img_bluetoothState_falling);


        card_falling_1.setOnClickListener(this);
        card_falling_2.setOnClickListener(this);
        btn_close_bluetooth.setOnClickListener(this);


        SharedPreferences sharedPreferences_state = getSharedPreferences("STATE", MODE_PRIVATE);
        if (!sharedPreferences_state.getBoolean("BLUETOOTH_SLIPPERY", false)) {
            img_ble_state_slippery.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
        } else {
            img_ble_state_slippery.setImageResource(R.drawable.ic_bluetooth_black_24dp);
        }
        if (!sharedPreferences_state.getBoolean("BLUETOOTH_FALLING", false)) {
            img_ble_state_falling.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
        } else {
            img_ble_state_falling.setImageResource(R.drawable.ic_bluetooth_black_24dp);
        }

        // 블루투스 초기화 작업
        ViseBle.config()
                .setScanTimeout(-1)
                .setConnectTimeout(10 * 1000) // 연결 시간 초과 시간설정
                .setOperateTimeout(5 * 1000)  // 데이터 작업 시간 초과 설정
                .setConnectRetryCount(3)      // 연결 실패 재시도 횟수 설정
                .setConnectRetryInterval(1000)// 재시도 시간 간격 설정
                .setOperateRetryCount(3)      // 데이터 조작 실패한 재시도 설정
                .setOperateRetryInterval(1000)// 데이터 조작 실패에 대한 재시도 시간간격
                .setMaxConnectCount(3);       // 연결된 최대 장치 수 설정
        ViseBle.getInstance().init(this);


        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

    }

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onClick(View view) {
        // 블루투스 서비스 인텐트
        switch (view.getId()) {
            // 낙상 제품 스캔
            case R.id.card_falling1:
                // 추락 제품 스캔
            case R.id.card_falling2:
                // 블루투스 서비스 시작
                // 서비스 시작
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopService(intentBluetoothService);
                    ViseBle.getInstance().disconnect();
                    startForegroundService(intentBluetoothService);
                } else {
                    stopService(intentBluetoothService);
                    ViseBle.getInstance().disconnect();
                    startService(intentBluetoothService);
                }

                break;
            // 블루투스 끄기
            case R.id.btn_close_bluetooth:
                stopService(intentBluetoothService);
                ViseBle.getInstance().disconnect();
                img_ble_state_slippery.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
                img_ble_state_falling.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
                SharedPreferences sharedPreferences_state = getContext().getSharedPreferences("STATE", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences_state.edit();
                editor.putBoolean("BLUETOOTH_SLIPPERY", false);
                editor.putBoolean("BLUETOOTH_FALLING", false);
                editor.commit();
                /*// write 설정
                BluetoothGattChannel bluetoothGattChannel_Write = new BluetoothGattChannel.Builder()
                        .setServiceUUID(ServiceUUID_Falling_1)
                        .setCharacteristicUUID(CharacteristicUUID_Write_Falling_1)
                        .setDescriptorUUID(null)
                        .setPropertyType(PropertyType.PROPERTY_WRITE)
                        .setBluetoothGatt(deviceMirrorCustom.getBluetoothGatt())
                        .builder();
                deviceMirrorCustom.bindChannel(iBleCallBack_Falling_1_Write, bluetoothGattChannel_Write);
                deviceMirrorCustom.writeData(batteryRequest_bytes);
                break;*/
        }


    }

    public static void threadHandlerBLEState(final String sensor, final String state) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (sensor.equals("Slippery") && state.equals("ON")) {
                            img_ble_state_slippery.setImageResource(R.drawable.ic_bluetooth_black_24dp);
                        } else if (sensor.equals("Slippery") && state.equals("OFF")) {
                            img_ble_state_slippery.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
                        } else if (sensor.equals("Falling") && state.equals("ON")) {
                            img_ble_state_falling.setImageResource(R.drawable.ic_bluetooth_black_24dp);
                        } else if (sensor.equals("Falling") && state.equals("OFF")) {
                            img_ble_state_falling.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
                        }
                    }
                });
            }
        }).start();
    }

    // 진행중 다이얼로그
    public static void showProgressDialog(String message) {
        dialog = new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(message);
        dialog.show();
    }

   /* // 블루투스 연결 확인 창
    public static void  (final String address) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("장치를 발견했습니다.");
        builder.setMessage("장치와 연결하시겠습니까?");
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        showProgressDialog("MAC address : " + address + " 장치에 연결중입니다.");
                        viseBleInstance.connectByMac(address, iConnectCallback);
                    }
                });
        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.show();
    }*/
}
