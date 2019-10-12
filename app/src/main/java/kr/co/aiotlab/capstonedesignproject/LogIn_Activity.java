package kr.co.aiotlab.capstonedesignproject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogIn_Activity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mUser;
    public static final String TAG = "LOGIN_ACTIVITY";
    private ProgressDialog dialog;
    private Button logIn_button;
    private EditText id, pw;
    private CheckBox check_saveId, check_auto_login;
    String email, password;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(this);

        id = findViewById(R.id.edt_login_id);
        pw = findViewById(R.id.edt_login_password);

        check_saveId = findViewById(R.id.check_saveId);
        check_auto_login = findViewById(R.id.check_autoLogin);

        // 로그인 변화 리스너
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Intent intent_signin = new Intent(LogIn_Activity.this, Main_Activity.class);
                    startActivity(intent_signin);
                    finish();
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        // 저장된 값 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("LOGIN_STATE", MODE_PRIVATE);
        String saved_email = sharedPreferences.getString("EMAIL", "");
        String saved_password = sharedPreferences.getString("PASSWORD", "");
        Boolean id_check = sharedPreferences.getBoolean("CHECKBOX_ID", false);
        Boolean auto_login_check = sharedPreferences.getBoolean("CHECKBOX_AUTO_LOGIN", false);

        id.setText(saved_email);
        pw.setText(saved_password);
        check_saveId.setChecked(id_check);
        check_auto_login.setChecked(auto_login_check);

        if(check_auto_login.isChecked()) {
            mAuth.addAuthStateListener(mAuthStateListener);
        }

        // 로그인 초기화
        init_signIn();

        //로그인버튼 클릭
        logIn_button = findViewById(R.id.btn_login);
        logIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 입력된 값 가져오기
                email = id.getText().toString();
                password = pw.getText().toString();

                // 입력 값이 null일 때 알려주기
                if (email.equals("")) {
                    Toast.makeText(LogIn_Activity.this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
                } else if (password.equals("")) {
                    Toast.makeText(LogIn_Activity.this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                } else if (email.equals("") && password.equals("")) {
                    Toast.makeText(LogIn_Activity.this, "입력을 확인해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    showProgressDialog();
                    dialog.show();

                    log_In();
                }

            }
        });

    }


    void init_signIn() {
        TextView signIn_button;

        signIn_button = findViewById(R.id.txt_signIn_click);
        signIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogIn_Activity.this, SignIn_Activity.class);
                startActivity(intent);
            }
        });
    }

    void log_In() {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent_goMain = new Intent(LogIn_Activity.this, Main_Activity.class);
                            startActivity(intent_goMain);
                            finish();

                            // 아이디 저장이 체크되어있으면 저장
                            SharedPreferences sharedPreferences = getSharedPreferences("LOGIN_STATE", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            // 아이디 저장에만 체크 되어있을 경우
                            if (check_saveId.isChecked() && !check_auto_login.isChecked()) {
                                editor.putString("EMAIL", id.getText().toString());
                                editor.putBoolean("CHECKBOX_ID", true);
                                editor.putBoolean("CHECKBOX_AUTO_LOGIN", false);
                            }
                            // 자동 로그인만 체크 되어있는 경우
                            else if(!check_saveId.isChecked() && check_auto_login.isChecked()) {
                                editor.putString("EMAIL", id.getText().toString());
                                editor.putString("PASSWORD", pw.getText().toString());
                                editor.putBoolean("CHECKBOX_ID", true);
                                editor.putBoolean("CHECKBOX_AUTO_LOGIN", true);
                            }
                            // 둘 다 체크되어있지 않은 경우
                            else if (!check_auto_login.isChecked() && !check_saveId.isChecked()) {
                                editor.putString("EMAIL", "");
                                editor.putString("PASSWORD", "");
                                editor.putBoolean("CHECKBOX_ID", false);
                                editor.putBoolean("CHECKBOX_AUTO_LOGIN", false);
                            }
                            // 둘 다 체크되어있는 경우
                            else {
                                editor.putString("EMAIL", id.getText().toString());
                                editor.putString("PASSWORD", pw.getText().toString());
                                editor.putBoolean("CHECKBOX_ID", true);
                                editor.putBoolean("CHECKBOX_AUTO_LOGIN", true);
                            }
                            editor.commit();

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(getApplicationContext(), "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }

                    }
                });
    }

    private void showProgressDialog() {

        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("잠시 기다려주세요.");

    }

    @Override
    protected void onStop() {
        super.onStop();
        // 로그인이 성공해서 메인으로 넘어가면 다이얼로그 없애기
        dialog.dismiss();
    }
}