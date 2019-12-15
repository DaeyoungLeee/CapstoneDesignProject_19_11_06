package kr.co.aiotlab.capstonedesignproject.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import kr.co.aiotlab.capstonedesignproject.DataList_CardItem;
import kr.co.aiotlab.capstonedesignproject.R;
import kr.co.aiotlab.capstonedesignproject.Recycler_Adapter;

public class Chart_Fragment extends Fragment {
    static LineChart mChart;
    static FirebaseDatabase mDatabase;
    static FirebaseAuth mAuth;
    static DatabaseReference ref;
    static ChildEventListener childEventListener;
    private RecyclerView recyclerView;
    private Recycler_Adapter adapter;

    private long now = System.currentTimeMillis();
    Date date = new Date(now);
    public static String timeFromDB;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_chart, container, false);

        mChart = viewGroup.findViewById(R.id.lineChart);
        mChart.setNoDataText("데이터를 불러오는 중입니다.");
        mChart.setNoDataTextColor(Color.BLUE);
        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        recyclerView = viewGroup.findViewById(R.id.recyclerView_dbList);

        LimitLine limit_high_risk = new LimitLine(65, "추락검지");

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.enableGridDashedLine(10f,10f,0f);
        leftAxis.addLimitLine(limit_high_risk); //상한선 추가(왼쪽 축)
        leftAxis.setDrawLimitLinesBehindData(true);

        findDataList();
        if (timeFromDB != null) {
            try {
                showLineChartData(timeFromDB);
            }catch (Exception e) {

            }
        }
        return viewGroup;
    }

    public static void showLineChartData(String timestamp) {

        final ArrayList<String> xDataList = new ArrayList<>();
        final ArrayList<Float> yDataList = new ArrayList<>();


        // 데이터 받아오기, 갱신
        // Log.d(TAG, "MQTT_Temperature_minute_" + year + month + day);
        ref = mDatabase.getReference(mAuth.getCurrentUser().getEmail().replace(".", "_"))
                .child("chart_data").child(timestamp);
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();

                String allData = data.get("data").toString();

                String[] xyData = allData.split(":");

                String xData = xyData[0];
                float yData = Float.parseFloat(xyData[1]);

                xDataList.add(xData);
                yDataList.add(yData);

                //Log.d(TAG, "xaxisSize: " + xEntrys.size());
                //Log.d(TAG, "yaxisSize: " + watt_data.size());

                final String[] xaxes = new String[xDataList.size()];

                final ArrayList<Entry> yDataEntry = new ArrayList<>();

                ArrayList<ILineDataSet> dataSets = new ArrayList<>();

                //a의 data로 a는 value값을 int형으로 변환한 것. value는 데이터가 바뀔 때의 값으로 업데이트 될 때마다 값이 나온다.
                for (int j = 0; j < xDataList.size(); j++) {
                    yDataEntry.add(new Entry(j, yDataList.get(j)));
                    xaxes[j] = xDataList.get(j);
                }

                //Line data setting
                LineDataSet set1 = new LineDataSet(yDataEntry,"단위 : 0.1㎨");
                set1.setFillAlpha(110);
                set1.setColor(Color.BLACK);
                set1.setLineWidth(3f);

                dataSets.add(set1);

                LineData data2 = new LineData(dataSets);

                mChart.invalidate();
                mChart.setData(data2);

                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // 오른쪽 와이축 없앰
                mChart.getAxisRight().setEnabled(false);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setGranularity(1f);

                xAxis.setValueFormatter(new IAxisValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        String val = null;
                        try {
                            val = xDataList.get((int) value);
                        } catch (IndexOutOfBoundsException e) {

                        }
                        return val;                    }
                });


                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                /*xAxis.setValueFormatter(new IAxisValueFormatter() {
                   @Override
                   public String getFormattedValue(float value, AxisBase axis) {
                       return xaxes[(int) value];
                   }
               }); */

                YAxis leftAxis = mChart.getAxisLeft();

                float y_max = Collections.max(yDataList);
                float y_min = Collections.min(yDataList);

                leftAxis.setAxisMaximum(y_max + 3);               //보여지는 최대
                leftAxis.setAxisMinimum(y_min - 3);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @android.support.annotation.Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        ref.addChildEventListener(childEventListener);

    }

    private void findDataList() {
        final ArrayList<String> dataDateList = new ArrayList<>();

        mDatabase.getReference(mAuth.getCurrentUser().getEmail().replace(".", "_"))
                .child("date_list")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Map<String, Object> data = (Map<String, Object>)dataSnapshot.getValue();
                        Log.d("TAG", "onChildAdded: " + data.get("timestamp"));
                        dataDateList.add(data.get("timestamp").toString());

                        /** 데이터가, 바뀔때마다 리사이클러뷰 업데이트! */
                        ArrayList<DataList_CardItem> cardItemsList = new ArrayList<>();
                        adapter = new Recycler_Adapter(cardItemsList);
//                        adapter.setOnItemClickedListener(DataList_CardItem.this);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                        recyclerView.setLayoutManager(linearLayoutManager);
                        adapter.notifyDataSetChanged();

                        for (int j = 0; j < dataDateList.size(); j++) {
                            try {
                                final DataList_CardItem cardItem = new DataList_CardItem(dataDateList.get(j));
                                cardItemsList.add(cardItem);
                                recyclerView.removeAllViewsInLayout();
                                recyclerView.setAdapter(adapter);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

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
}
