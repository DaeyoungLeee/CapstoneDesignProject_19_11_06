package kr.co.aiotlab.capstonedesignproject.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import kr.co.aiotlab.capstonedesignproject.User_CardItem;
import kr.co.aiotlab.capstonedesignproject.R;

public class AddPersonList_Activity extends AppCompatActivity implements AddPerson_Recycler_Adapter.OnItemClickedListener {
    public static final String TAG = "AddPersonList_Activity";
    private ImageButton add_button;
    private FirebaseDatabase mDatabase;
    private RecyclerView recyclerView;
    AddPerson_Recycler_Adapter addPersonRecyclerAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    ArrayList<String> emailList = new ArrayList<>();
    // 중복 방지 리스트
    ArrayList<String> emailList1 = new ArrayList<>();
    ArrayList<String> emailList2 = new ArrayList<>();
    ArrayList<String> emailList_key = new ArrayList<>();

    private String nameExist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_added_person_list);

        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        add_button = findViewById(R.id.imgbtn_addperson);
        recyclerView = findViewById(R.id.recyclerView_person_list);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);

        addPersonRecyclerAdapter = new AddPerson_Recycler_Adapter(getApplicationContext());
        addPersonRecyclerAdapter.setOnItemClickedListener(this);

        //부드러운 스크롤링
        recyclerView.setNestedScrollingEnabled(false);

        // 저장되어있는 리스트 가져오기
        DatabaseReference listDataRef = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list");
        listDataRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                String email_list = data.get("email").toString();
                final User_CardItem userCardItem = new User_CardItem();

                userCardItem.setEmail(email_list);
                // 이름정보 가져오기
                mDatabase.getReference(email_list.replace(".", "_")).child("user_name").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String name = dataSnapshot.getValue(String.class);
                        Log.d(TAG, "name: " + name);

                        userCardItem.setName(name);
                        addPersonRecyclerAdapter.addItem(userCardItem);

                        recyclerView.setAdapter(addPersonRecyclerAdapter);
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
                finish();
                Intent intent = new Intent(getApplicationContext(), AddPersonList_Activity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                Toast.makeText(AddPersonList_Activity.this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 중복방지하는 emailList2 얻어내기
        DatabaseReference check2Ref = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list");
        check2Ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                String email = data.get("email").toString();
                emailList1.add(email);

                for (int i = 0; i < emailList1.size(); i++) {
                    if (!emailList2.contains(emailList1.get(i))) {
                        emailList2.add(emailList1.get(i));
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


        // 키값 받아오기(삭제하기 위해)
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshotKey: dataSnapshot.getChildren()) {
                    emailList_key.add(dataSnapshotKey.getKey());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 카드뷰 사람 추가 버튼
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show();
            }
        });
    }

    void show() {
        // 이메일 입력할 때 중복 확인, 있는 이메일인지 확인
        DatabaseReference checkRef = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list");
        checkRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                String email = data.get("email").toString();
                emailList.add(email);
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

        final EditText edittext = new EditText(this);
        // 0x00000021는 textEmailAddress
        edittext.setInputType(0x00000021);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("대상 추가");
        builder.setMessage("알림을 보낼 유저의 이메일을 입력해주세요");
        builder.setView(edittext);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int which) {
                        // 실제 존재하는 메일인지 유효성 확인해서 nameExist에 값 전달,
                        mDatabase.getReference(edittext.getText().toString().replace(".", "_")).child("user_name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                nameExist = dataSnapshot.getValue(String.class);

                                if (nameExist == null) {
                                    Toast.makeText(AddPersonList_Activity.this, "존재하는 이메일이 아닙니다.", Toast.LENGTH_SHORT).show();
                                }else if (emailList2.contains(edittext.getText().toString())) {
                                    Toast.makeText(AddPersonList_Activity.this, "이미 추가된 이메일입니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    // 데이터베이스에 이메일 추가!
                                    DatabaseReference mRef = mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list").push().child("email");
                                    mRef.setValue(edittext.getText().toString());

                                    Toast.makeText(AddPersonList_Activity.this, "추가되었습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }


    @Override
    public void onItemClick(AddPerson_Recycler_Adapter.ItemViewHolder holder, View view, int position) {

    }

    // 삭제!!
    @Override
    public void onDeleteButtonClick(int position) {
        mDatabase.getReference(mUser.getEmail().replace(".", "_")).child("listener_list").child(emailList_key.get(position)).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
