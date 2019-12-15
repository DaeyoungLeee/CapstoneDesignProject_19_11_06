package kr.co.aiotlab.capstonedesignproject.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import kr.co.aiotlab.capstonedesignproject.R;

public class AlertFalling_Activity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView txt_alert_person, txt_alert_time, txt_alert_lat, txt_alert_long, txt_alert_altitude, txt_alert_address;
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private Button btn_call;
    final int RUNTIME_PERMISSION_REQUEST_CALL_PHONE = 1;
    private String userId;

    private double lat_gl, long_gl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_falling);

        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        userId = mUser.getEmail().replace(".", "_");

        txt_alert_lat = findViewById(R.id.txt_alert_latitude);
        txt_alert_long = findViewById(R.id.txt_alert_longitude);
        txt_alert_person = findViewById(R.id.txt_alert_personName);
        txt_alert_time = findViewById(R.id.txt_accident_time);
        txt_alert_altitude = findViewById(R.id.txt_alert_altitude);
        txt_alert_address = findViewById(R.id.txt_alert_address);



        // 신고하기 버튼 누르면 신고
        btn_call = findViewById(R.id.btn_alert_call);
        btn_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 전화 관련 permission
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AlertFalling_Activity.this, new String[]{Manifest.permission.CALL_PHONE}, RUNTIME_PERMISSION_REQUEST_CALL_PHONE);
                }else {
                    startCall();
                }
            }
        });

        // 네이버 지도 API 호출
        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient("2k84cbiifl"));

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.alert_map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 사고자 정보
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                txt_alert_person.setText(name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 위도
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("latitude").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String lat = dataSnapshot.getValue(String.class);
                lat_gl = Double.parseDouble(lat);
                txt_alert_lat.setText(lat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        // 경도
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("longitude").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String longi = dataSnapshot.getValue(String.class);
                long_gl = Double.parseDouble(longi);
                txt_alert_long.setText(longi);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 고도
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("altitude").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String alti = dataSnapshot.getValue(String.class);
                long_gl = Double.parseDouble(alti);
                txt_alert_altitude.setText(alti);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 주소
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("address").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String address = dataSnapshot.getValue(String.class);
                txt_alert_address.setText(address);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 시간 정보
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String timestamp = dataSnapshot.getValue(String.class);
                txt_alert_time.setText(timestamp);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {
        // 왜인지는 모르겠는데 이 안에 구현을 해야 지도가 제대로 표시됨...
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("accident_data").child("longitude").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String longi = dataSnapshot.getValue(String.class);
                                long_gl = Double.parseDouble(longi);

                                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(lat_gl, long_gl));
                                naverMap.moveCamera(cameraUpdate);

                                naverMap.setBuildingHeight(1f);

                                Marker marker = new Marker();
                                marker.setPosition(new LatLng(lat_gl, long_gl));
                                marker.setMap(naverMap);
                            }
                        }, 500);
                    }
                }).start();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // 전화 걸기
    private void startCall() {
        mDatabase.getReference(userId).child("accident_data").child("phone").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Intent call_intent = new Intent();
                call_intent.setAction(Intent.ACTION_CALL);
                call_intent.setData(Uri.parse("tel:" + dataSnapshot.getValue(String.class)));
                startActivity(call_intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RUNTIME_PERMISSION_REQUEST_CALL_PHONE:
                // 사용자가 권한을 수락헀다면
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 해당 권한을 실행하는 코드
                    startCall();
                }else {

                }
                return;
        }
    }
}
