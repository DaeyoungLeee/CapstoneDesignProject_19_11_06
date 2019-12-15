package kr.co.aiotlab.capstonedesignproject.Activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.co.aiotlab.capstonedesignproject.R;
import kr.co.aiotlab.capstonedesignproject.UserData;

public class SignIn_Activity extends AppCompatActivity {

    public static final String TAG = "SIGN_IN_ACTIVITY";

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private FirebaseUser mUser;
    private DatabaseReference mRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mUser = mAuth.getCurrentUser();

        signIn();
    }

    void signIn() {
        final EditText edt_email;
        final EditText edt_password, edt_password_check;
        Button signIn_button;

        edt_email = findViewById(R.id.edt_member_userEmail);
        edt_password= findViewById(R.id.edt_member_userPassword);
        edt_password_check= findViewById(R.id.edt_member_userPassword2);
        signIn_button = findViewById(R.id.btn_member_finish);

        signIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edt_email.getText().toString();
                String password = edt_password.getText().toString();
                String password_check = edt_password_check.getText().toString();

                Log.d(TAG, "password length = " + password.length());
                if (!password.equals("")) {
                    if (!password.equals(password_check)) {
                        Toast.makeText(SignIn_Activity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    } else if (password.equals(password_check) && password.length() >= 8) {
                        check_validation(email, password);
                    } else if (password.equals(password_check) && password.length() < 8) {
                        Toast.makeText(SignIn_Activity.this, "비밀번호는 8글자 이상 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(SignIn_Activity.this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void check_validation(String email, String password) {
        // 비밀번호 유효성 검사식1 : 숫자, 특수문자가 포함되어야 한다.
        String regExp_symbol = "([0-9].*[!,@,#,^,&,*,(,)])|([!,@,#,^,&,*,(,)].*[0-9])";
        // 비밀번호 유효성 검사식2 : 영문자 대소문자가 적어도 하나씩은 포함되어야 한다.
        //String regExp_alpha = "([a-z].*[A-Z])|([A-Z].*[a-z])";
        // 정규표현식 컴파일
        Pattern pattern_symbol = Pattern.compile(regExp_symbol);
        //Pattern pattern_alpha = Pattern.compile(regExp_alpha);

        Matcher matcher_symbol = pattern_symbol.matcher(password);
        //Matcher matcher_alpha = pattern_alpha.matcher(password);

        if (matcher_symbol.find()) {
            email_signIn(email, password);
        }else if (!matcher_symbol.find()) {
            Toast.makeText(this, "숫자와 특수문자를 반드시 넣어주세요", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "비밀번호로 부적절합니다. 다시 한 번 확인해주세요", Toast.LENGTH_SHORT).show();
        }

    }

    void email_signIn(final String email, String password) {
        final EditText edt_name;
        final String userName;

        edt_name = findViewById(R.id.edt_member_userName);
        userName = edt_name.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // token 저장
                            UserData userData = new UserData();
                            userData.fcmToken = FirebaseInstanceId.getInstance().getToken();
                            userData.userName = userName;

                            // RealTime Firebase database에 유저 정보 저장
                            mRef = mDatabase.getReference(email.replace(".", "_"));

                            mRef.child("token").setValue(userData.getFcmToken());
                            mRef.child("user_name").setValue(userData.getUserName());

                            Toast.makeText(SignIn_Activity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                            finish();
                        }else {
                            Toast.makeText(SignIn_Activity.this, "이미 가입된 아이디이거나 이메일 양식이 아닙니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
