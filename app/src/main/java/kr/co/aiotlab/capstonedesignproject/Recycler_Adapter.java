package kr.co.aiotlab.capstonedesignproject;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import static kr.co.aiotlab.capstonedesignproject.Fragments.Chart_Fragment.showLineChartData;
import static kr.co.aiotlab.capstonedesignproject.Fragments.Chart_Fragment.timeFromDB;

public class Recycler_Adapter extends RecyclerView.Adapter<Recycler_Adapter.CustomViewHolder> {

    private ArrayList<DataList_CardItem> cardItems;
    OnItemClickedListener listener;
    private static final String TAG = "RecyclerView";

    public static interface OnItemClickedListener {
        public void onItemClick(CustomViewHolder holder, View view, int position);

        void onDeleteButtonClick(int position);
    }

    public Recycler_Adapter(ArrayList<DataList_CardItem> cardItems) {
        this.cardItems = cardItems;
    }

    @NonNull
    @Override
    public Recycler_Adapter.CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_database_list, parent, false);
        CustomViewHolder customViewHolder = new CustomViewHolder(view);

        return customViewHolder;
    }

    @Override
    public void onBindViewHolder(final Recycler_Adapter.CustomViewHolder holder, final int position) {
        holder.txt_data_date.setText(cardItems.get(position).getData_date());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 아이템 하나씩 클릭했을 때
                timeFromDB = cardItems.get(position).getData_date();
                showLineChartData(timeFromDB);
            }
        });


        holder.setOnItemClickedListener(listener);

        holder.img_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("tag", "onClick: num" + holder.getAdapterPosition());
                if (listener != null) {
                    listener.onDeleteButtonClick(holder.getAdapterPosition());
                }
            }
        });
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        this.listener = listener;

    }

    @Override
    public int getItemCount() {
        return (null != cardItems ? cardItems.size() : 0);
    }

    public void remove(int pos) {
        try {
            cardItems.remove(pos);
            notifyItemRemoved(pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        private TextView txt_data_date;
        private ImageButton img_delete;
        OnItemClickedListener listener;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            this.txt_data_date = itemView.findViewById(R.id.txt_date);
            this.img_delete = itemView.findViewById(R.id.btn_delete);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null) {
                        listener.onItemClick(CustomViewHolder.this, v, position);
                    }
                }
            });
        }

        public void setOnItemClickedListener(OnItemClickedListener listener) {
            this.listener = listener;
        }
    }


}