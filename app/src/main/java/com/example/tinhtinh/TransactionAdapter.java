package com.example.tinhtinh;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    
    private final Context context;
    private final List<Map<String, String>> transactions;
    private final OnItemDeleteListener deleteListener;
    
    // Interface để xử lý xóa item
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }
    
    public TransactionAdapter(Context context, List<Map<String, String>> transactions, OnItemDeleteListener deleteListener) {
        this.context = context;
        this.transactions = transactions;
        this.deleteListener = deleteListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
        return new ViewHolder(view, deleteListener);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> transaction = transactions.get(position);
        
        holder.senderTextView.setText(transaction.get("sender"));
        holder.dateTextView.setText(transaction.get("date"));
        
        String amount = transaction.get("amount");
        if (amount != null && !amount.isEmpty()) {
            holder.amountTextView.setText(amount);
            holder.amountTextView.setVisibility(View.VISIBLE);
        } else {
            holder.amountTextView.setVisibility(View.GONE);
        }
        
        holder.messageTextView.setText(transaction.get("message"));
        
        // Thiết lập icon ngân hàng dựa vào tên ngân hàng
        String sender = transaction.get("sender");
        if (sender != null) {
            if (sender.contains("MB") || sender.contains("MB Bank")) {
                holder.bankIconImageView.setImageResource(android.R.drawable.ic_menu_send);
                holder.bankIconImageView.setBackgroundColor(context.getResources().getColor(R.color.primary));
            } else if (sender.contains("Vietcombank")) {
                holder.bankIconImageView.setImageResource(android.R.drawable.ic_menu_agenda);
                holder.bankIconImageView.setBackgroundColor(context.getResources().getColor(R.color.accent));
            } else if (sender.contains("Techcombank")) {
                holder.bankIconImageView.setImageResource(android.R.drawable.ic_menu_compass);
                holder.bankIconImageView.setBackgroundColor(context.getResources().getColor(R.color.money_green));
            } else {
                // Mặc định
                holder.bankIconImageView.setImageResource(android.R.drawable.ic_menu_info_details);
                holder.bankIconImageView.setBackgroundColor(context.getResources().getColor(R.color.primary_light));
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView senderTextView;
        public TextView dateTextView;
        public TextView amountTextView;
        public TextView messageTextView;
        public ImageView bankIconImageView;
        public ImageButton deleteButton;
        
        public ViewHolder(@NonNull View itemView, OnItemDeleteListener listener) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            bankIconImageView = itemView.findViewById(R.id.bankIconImageView);
            deleteButton = itemView.findViewById(R.id.deleteNotificationButton);
            
            // Thiết lập sự kiện click cho nút xóa
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemDelete(position);
                }
            });
        }
    }
} 