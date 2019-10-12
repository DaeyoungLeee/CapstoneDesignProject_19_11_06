package kr.co.aiotlab.capstonedesignproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class Intro_Activity extends AppCompatActivity {
    private ImageView img_falling;
    Animation fromBottom, fromLeft, fromRight;
    private TextView choo, rak, al, rim, seo, bi, s;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        img_falling = findViewById(R.id.img_intro_fall);
        choo = findViewById(R.id.txt_choo);
        rak = findViewById(R.id.txt_rak);
        al = findViewById(R.id.txt_al);
        rim = findViewById(R.id.txt_rim);
        seo = findViewById(R.id.txt_seo);
        bi = findViewById(R.id.txt_bi);
        s = findViewById(R.id.txt_s);

        fromBottom = AnimationUtils.loadAnimation(this, R.anim.fromtop);
        fromLeft = AnimationUtils.loadAnimation(this, R.anim.fromleft);
        fromRight = AnimationUtils.loadAnimation(this, R.anim.fromright);

        img_falling.setAnimation(fromBottom);

        choo.setAnimation(fromRight);
        rak.setAnimation(fromRight);
        al.setAnimation(fromRight);
        rim.setAnimation(fromRight);

        seo.setAnimation(fromLeft);
        bi.setAnimation(fromLeft);
        s.setAnimation(fromLeft);

        Handler handler = new Handler(); // 객체생성
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intro_Activity.this, LogIn_Activity.class);
                startActivity(intent);
                finish();
            }
        }, 1500);           // 몇 초간 띄우고 다음 액티비티로 넘어갈지 결정

    }
}
