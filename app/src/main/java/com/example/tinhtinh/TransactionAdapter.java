package com.example.tinhtinh;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    
    private final Context context;
    private final List<Map<String, String>> transactions;
    
    public TransactionAdapter(Context context, List<Map<String, String>> transactions) {
        this.context = context;
        this.transactions = transactions;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false);
        return new ViewHolder(view);
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
        
        // Thiết lập icon ngân hàng (mặc định là icon thông tin)
        holder.bankIconImageView.setImageResource(android.R.drawable.ic_menu_info_details);
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
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            bankIconImageView = itemView.findViewById(R.id.bankIconImageView);
        }
    }
} 