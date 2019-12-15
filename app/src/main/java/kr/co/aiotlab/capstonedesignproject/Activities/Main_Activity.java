package kr.co.aiotlab.capstonedesignproject.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import kr.co.aiotlab.capstonedesignproject.User_CardItem;
import kr.co.aiotlab.capstonedesignproject.R;
import kr.co.aiotlab.capstonedesignproject.UserData;

public class Main_Activity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    public static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef_name;
    private DatabaseReference mRef_serverkey;
    private TextView my_latitude, my_longitude, my_altitude, my_address, txt_phoneNum, txt_messageContext;
    public static TextView txt_autoGPS_state;
    private double latitude, longitude, altitude = 0;
    private long backBtnTime = 0;
    private ListView listView;

    public static Context mContext;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    // 내 아이디
    private String myId;

    // 주소정보
    public static String area1, area2, area3, area4;

    // 중복되는 이메일 데이터
    ArrayList<String> array_email = new ArrayList<>();
    // 중복 제거된 이메일 데이터
    ArrayList<String> array_email2 = new ArrayList<>();
    // 보내는사람 이름 가져오기
    final ArrayList<String> arrayList_information = new ArrayList<>();

    // 네이버맵 내 위치 관련
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    // FCM 보내기 주소
    public static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";
    // 클라우드 메시지 서버 키
    public static String SERVER_KEY;
    private FusedLocationSource locationSource;

    public static TextView test;

    //hidden chart
    private ImageView img_showChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navigationView = findViewById(R.id.navigation_view);
        mAuth = FirebaseAuth.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        my_latitude = findViewById(R.id.txt_latitude);
        my_longitude = findViewById(R.id.txt_longitude);
        my_altitude = findViewById(R.id.txt_altitude);
        my_address = findViewById(R.id.txt_address);
        txt_autoGPS_state = findViewById(R.id.txt_autoGPS_state);
        txt_messageContext = findViewById(R.id.txt_messageContext);
        txt_phoneNum = findViewById(R.id.txt_phoneNum);
        listView = findViewById(R.id.list_name);
        test = findViewById(R.id.txt_test);
        img_showChart = findViewById(R.id.img_show_chart);

        img_showChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ChartViewPager_Activity.class);
                startActivity(intent);
            }
        });

        //카드뷰 내용 채우기
        SharedPreferences sharedPreferences = getSharedPreferences("MESSAGE", MODE_PRIVATE);
        String message = sharedPreferences.getString("LINE_MESSAGE", "긴급상황입니다.");
        txt_messageContext.setText(message);

        SharedPreferences phoneShared = getSharedPreferences("PHONE", MODE_PRIVATE);
        String text = phoneShared.getString("NUMBER", "");
        txt_phoneNum.setText(text);


        //
        myId = mUser.getEmail().replace(".", "_");
        Log.d(TAG, "getEmail: " + myId);
        // FCM token 업데이트
        updataFCMToken();

        // GPS가 꺼져있으면 키라고 설정
        chkGpsService();

        // 서버키 가져오기
        getCloudServerKey();

        // GPS 사용 권한 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Main_Activity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        // 네이버맵 내 위치 관련
        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // 드로어 네비게이션 토글기능
        drawerLayout = findViewById(R.id.drawerLayout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(this);
        // 네이게이션 드로어 아이콘 색상 추가
        navigationView.setItemIconTintList(null);

        // 네이버 지도 API 호출
        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient("2k84cbiifl"));

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 등록 유저 이름 가져와
        getFDBUserName();

    }
    // OnMapReadyCallback implement method

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.drawer_bluetooth:
                Intent intent_bluetooth = new Intent(this, BluetoothSelect_Activity.class);
                startActivity(intent_bluetooth);
                break;
            case R.id.drawer_add_person:
                addPersonList();
                break;
            case R.id.drawer_phone_number:
                inputPhoneNumber();
                break;
            case R.id.drawer_message:
                writeWhatYouWant();
                break;
            case R.id.drawer_send_message:
                sendMessageTest();
                break;
            case R.id.drawer_settings:
                Intent intent_settings = new Intent(this, Settings_Activity.class);
                startActivity(intent_settings);
                break;
            case R.id.drawer_logout:
                // 로그아웃 버튼을 클릭하면 Password 저장을 지우고 체크박스 체크 해지
                SharedPreferences sharedPreferences = getSharedPreferences("LOGIN_STATE", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("PASSWORD", "");
                editor.putBoolean("CHECKBOX_AUTO_LOGIN", false);
                editor.commit();
                // 로그아웃 진행
                mAuth.signOut();
                // 로그아웃 알림 토스트바
                Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                // 로그인화면 보여주기
                Intent intent = new Intent(this, LogIn_Activity.class);
                startActivity(intent);
                // 해당 엑티비티 완전 종료
                finishAffinity();
                System.runFinalization();
                System.exit(0);
                finish();
                break;
        }
        return true;
    }

    //토글버튼 클릭시 드로여 열리고 닫힘
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d(TAG, "onMapReady");
        naverMap.setLocationSource(locationSource);

        // 네이버지도 Ui 세팅
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);

        //위치 변경 이벤트
        naverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                altitude = location.getAltitude();

                my_altitude.setText(String.valueOf(location.getAltitude()));
                my_latitude.setText(String.valueOf(location.getLatitude()));
                my_longitude.setText(String.valueOf(location.getLongitude()));

                getAddress(longitude, latitude);
            }
        });

        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
//        Marker marker = new Marker();
//        marker.setPosition(new LatLng(latitude, longitude));
//        marker.setMap(naverMap);
    }

    void sendMessageTest() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.send_message_test, null);
        builder.setView(view);
        final Button submit = view.findViewById(R.id.buttonSubmit);

        final AlertDialog dialog = builder.create();

        // 데이터베이서에서 유저가 등록한 이메일 주소 불러오기
        DatabaseReference mGetPersonData = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list");

        mGetPersonData.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Map<String, Object> email_data = (Map<String, Object>) dataSnapshot.getValue();
                String name = email_data.get("email").toString();

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
        // 저장된 메시지 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("MESSAGE", MODE_PRIVATE);
        final String message = sharedPreferences.getString("LINE_MESSAGE", "위급상황입니다.");
        // 저장된 번호 가져오기
        SharedPreferences phoneNum_shared = getSharedPreferences("PHONE", MODE_PRIVATE);
        final String phoneNum = phoneNum_shared.getString("NUMBER", "");
        // 보내기 버튼 클릭하면..
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < array_email.size(); i++) {
                    if (!array_email2.contains(array_email.get(i)))
                        array_email2.add(array_email.get(i));
                }
                for (int i = 0; i < array_email2.size(); i++) {
                    sendPostToFCM(array_email2.get(i).replace(".", "_"), message, latitude, longitude, altitude, phoneNum);
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void sendPostToFCM(final String email, final String message, final double latitude, final double longitude, final double altitude, String phoneNum) {
        // 사용자 이름 정보 받아오기
        final String[] userName = new String[1];

        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("user_name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userName[0] = dataSnapshot.getValue(String.class);
                Log.d(TAG, "onDataChange:" + userName[0]);
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
                            notification.put("body", message + "\n위도 :" + latitude + "경도 :" + longitude);
                            notification.put("title", userName[0] + "님으로부터 온 메시지");
                            notification.put("sound", "emergencysound");
                            notification.put("android_channel_id", "1008");
                            notification.put("vibrate", "true");
                            notification.put("icon", "myicon");
                            notification.put("click_action", "OPEN_ACTIVITY");

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
                Toast.makeText(Main_Activity.this, "취소되었습니다", Toast.LENGTH_SHORT).show();
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

    public String getTime() {
        long mNow = System.currentTimeMillis();
        Date mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    // 번호 입력하는 다이얼로그
    void inputPhoneNumber() {
        // 저장된 폰 정보 가져오가
        SharedPreferences phoneShared = getSharedPreferences("PHONE", MODE_PRIVATE);
        String text = phoneShared.getString("NUMBER", "");

        final EditText edittext = new EditText(this);
        edittext.setInputType(InputType.TYPE_CLASS_PHONE);
        edittext.setText(text);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("번호 입력");
        builder.setMessage("알려주고싶은 번호를 입력해주세요");
        builder.setView(edittext);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences phoneShared = getSharedPreferences("PHONE", MODE_PRIVATE);
                        SharedPreferences.Editor editor = phoneShared.edit();
                        editor.putString("NUMBER", edittext.getText().toString());
                        txt_phoneNum.setText(edittext.getText().toString());
                        editor.commit();
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }


    // 원하는 메시지 내요 입력 다이얼로그
    void writeWhatYouWant() {
        SharedPreferences sharedPreferences = getSharedPreferences("MESSAGE", MODE_PRIVATE);
        String message = sharedPreferences.getString("LINE_MESSAGE", "긴급상황입니다.");
        final EditText edittext = new EditText(this);
        edittext.setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("보낼 메시지");
        builder.setMessage("보내고싶은 내용 입력");
        builder.setView(edittext);
        builder.setPositiveButton("저장",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences sharedPreferences = getSharedPreferences("MESSAGE", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("LINE_MESSAGE", edittext.getText().toString());
                        editor.commit();
                        txt_messageContext.setText(edittext.getText().toString());
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    private void getCloudServerKey() {
        mRef_serverkey = mDatabase.getReference("cloudmessageKey");
        mRef_serverkey.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SERVER_KEY = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addPersonList() {
        Intent intent = new Intent(this, AddPersonList_Activity.class);
        startActivity(intent);
    }

    private void updataFCMToken() {
        UserData userData = new UserData();
        userData.fcmToken = FirebaseInstanceId.getInstance().getToken();

        DatabaseReference mRef_token = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("token");
        mRef_token.setValue(userData.getFcmToken());
    }


    @Override
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        long gapTime = curTime - backBtnTime;
        // 드로어가 열려있으면 닫고
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
        }
        // 드로어가 닫혀있으면
        else {
            // 뒤로가기 버튼 한 번 클릭하고 2초 내로 한 번 더 클릭하면 종료
            if (gapTime >= 0 && gapTime <= 2000) {
                super.onBackPressed();
            } else {
                backBtnTime = curTime;
                Toast.makeText(this, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //GPS 설정 체크
    private boolean chkGpsService() {

        String gps = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        Log.d(gps, "aaaa");

        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {

            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    }).create().show();
            return false;

        } else {
            return true;
        }
    }

    private void getAddress(final double longitude, final double latitude) {

        new Thread(new Runnable() {
            String clientId = "2k84cbiifl";// 애플리케이션 클라이언트 아이디값";
            String clientSecret = "X9pd8MAgAclQiGdQewRUgVJYqqKyKT1gx488UpXR";// 애플리케이션 클라이언트 시크릿값";
            String json = null;

            @Override
            public void run() {
                try {
                    Log.d(TAG, "getPointFromNaver: 진행중");

                    String apiURL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?coords=" + longitude + "," + latitude + "&sourcecrs=epsg:4326&orders=legalcode,admcode,addr,roadaddr&output=json"; // json
                    URL url = new URL(apiURL);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    // 헤더부분 입력
                    con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
                    con.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);

                    // request 코드가 200이면 정상적으로 호출된다고 나와있다.
                    int responseCode = con.getResponseCode();
                    Log.d(TAG, "response code:" + responseCode);

                    BufferedReader br = null;

                    if (responseCode == 200) { // 정상 호출
                        Log.d(TAG, "getPointFromNaver: 정상호출");
                        //정상적으로 호출이 되면 읽어온다.
                        br = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    } else { // 에러 발생
                        Log.d(TAG, "getPointFromNaver: 비정상호출");
                    }

                    // json으로 받아내는 코드!
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    //한 줄 한 줄 읽어들임
                    while ((inputLine = br.readLine()) != null) {
                        response.append(inputLine);
                    }
                    br.close();
                    json = response.toString();

                    // json값이 만약 null값이면 return시킴
                    if (json == null) {
                        return;
                    }

                    //이제 그 결과값 json이 잘 변환되어 들어왔는지 로그를 찍어서 확인해본다.
                    Log.d("TEST2", "json => " + json);

                    // json형식의 데이터를 String으로 변환하는 과정
                    JSONObject jsonObject = new JSONObject(json);

                    // results는 대괄호 []로 감싸져있다. -> Array변환
                    JSONArray resultsArray = jsonObject.getJSONArray("results");

                    JSONObject jsonObject1 = resultsArray.getJSONObject(0);

                    //이제 배열중 region에 area값들이 들어있기 때문에 중괄호 {}로 감싸진 region값을 가져온다.
                    JSONObject dataObject = (JSONObject) jsonObject1.get("region");

                    // region에서 area1, area2, area3, area4를 각각 또 불러와야한다.
                    JSONObject area1Object = (JSONObject) dataObject.get("area1");
                    JSONObject area2Object = (JSONObject) dataObject.get("area2");
                    JSONObject area3Object = (JSONObject) dataObject.get("area3");
                    JSONObject area4Object = (JSONObject) dataObject.get("area4");

                    Log.d(TAG, "area1 name : " + area1Object.getString("name") + area2Object.getString("name") + area3Object.getString("name") + area4Object.getString("name"));

                    // 각각 불러온 객체에서 원하는 name 값을 가져오면 끝( area1, area2, area3, area4 는 final 전역변수로 지정
                    area1 = area1Object.getString("name");
                    area2 = area2Object.getString("name");
                    area3 = area3Object.getString("name");
                    area4 = area4Object.getString("name");

                    // 이제 추출한 데이터를 가지고 Ui 변경하기 위해 handler 사용
                    Message msg = handler.obtainMessage();
                    handler.sendMessage(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    // 주소 받아오면 처리 메시지
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            my_address.setText(area1 + " " + area2 + " " + area3 + " " + area4);
        }
    };

    public static Context getContext() {
        return mContext;
    }

    // 파이어베이스 디비 접근해서 유저 이름 정보 가져오기
    private void getFDBUserName() {
        DatabaseReference listDataRef = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list");
        listDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                String email_list = data.get("email").toString();
                final User_CardItem cardItem = new User_CardItem();

                cardItem.setEmail(email_list);
                // 이름정보 가져오기
                mDatabase.getReference(email_list.replace(".", "_")).child("user_name").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String name = dataSnapshot.getValue(String.class);
                        arrayList_information.add(name);
                        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(),
                                R.layout.support_simple_spinner_dropdown_item,
                                arrayList_information);

                        listView.setAdapter(arrayAdapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

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


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //ViseBle.getInstance().disconnect();
    }
}
