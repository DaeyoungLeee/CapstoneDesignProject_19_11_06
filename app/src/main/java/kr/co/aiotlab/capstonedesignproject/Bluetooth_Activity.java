package kr.co.aiotlab.capstonedesignproject;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class Bluetooth_Activity extends AppCompatActivity {

    Button scan_bluetooth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        scan_bluetooth = findViewById(R.id.btn_bt_connect);

        // 스캔 버튼 클릭
        scan_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {

        }
    }
}
