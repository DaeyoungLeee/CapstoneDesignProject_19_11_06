package kr.co.aiotlab.capstonedesignproject.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import kr.co.aiotlab.capstonedesignproject.Adapters.ViewPager_adapter;
import kr.co.aiotlab.capstonedesignproject.Fragments.Chart_Fragment;
import kr.co.aiotlab.capstonedesignproject.Fragments.SecondView_Fragment;
import kr.co.aiotlab.capstonedesignproject.R;

public class ChartViewPager_Activity extends AppCompatActivity {

    ViewPager viewPager;
    PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager_main);

        List<Fragment> list = new ArrayList<>();

        list.add(new Chart_Fragment());
        list.add(new SecondView_Fragment());

        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new ViewPager_adapter(getSupportFragmentManager(), list);
        viewPager.setAdapter(pagerAdapter);
    }
}
