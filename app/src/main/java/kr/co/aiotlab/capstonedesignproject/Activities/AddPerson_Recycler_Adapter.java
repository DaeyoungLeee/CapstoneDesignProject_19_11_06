package kr.co.aiotlab.capstonedesignproject.Activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import kr.co.aiotlab.capstonedesignproject.User_CardItem;
import kr.co.aiotlab.capstonedesignproject.R;

public class AddPerson_Recycler_Adapter extends RecyclerView.Adapter<AddPerson_Recycler_Adapter.ItemViewHolder> {
    Context context;

    ArrayList<User_CardItem> items = new ArrayList<>();

    OnItemClickedListener listener;

    public static interface OnItemClickedListener {
        public void onItemClick(ItemViewHolder holder, View view, int position);
        void onDeleteButtonClick(int position);
    }

    public AddPerson_Recycler_Adapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_person_list, viewGroup, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder itemViewHolder, int i) {
        final int position = i;

        itemViewHolder.setSingerItem(items.get(i));

        itemViewHolder.setOnItemClickedListener(listener);

        itemViewHolder.btn_delete_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDeleteButtonClick(position);
                    Log.d("tag", "onClick: " + listener.toString());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(User_CardItem item) {
        items.add(item);
    }
    public User_CardItem getItem(int position) {
        return items.get(position);
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        this.listener = listener;
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView txt_email, txt_name;
        OnItemClickedListener listener;
        Button btn_delete_list;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            txt_email = itemView.findViewById(R.id.txt_item_email);
            txt_name = itemView.findViewById(R.id.txt_item_name);
            btn_delete_list = itemView.findViewById(R.id.btn_delete_list);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null) {
                        listener.onItemClick(ItemViewHolder.this, v, position);
                    }
                }
            });
        }
        public void setOnItemClickedListener(OnItemClickedListener listener) {
            this.listener = listener;
        }

        public void setSingerItem(User_CardItem item) {
            txt_email.setText(item.getEmail());
            txt_name.setText(item.getName());
        }
    }
}
