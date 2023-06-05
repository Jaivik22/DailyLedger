package com.example.dailyledger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockEntryAdapter extends RecyclerView.Adapter<StockEntryAdapter.ViewHolder> {


   private List<StockEntryModel> stockEntries;
   private Context context;
   private String stockFilter;
   private String stockPrice;
   private String stockItemID;

    public StockEntryAdapter( Context context,List<StockEntryModel> stockEntries,String stockFilter,String stockPrice,String stockItemID) {
        this.stockEntries = stockEntries;
        this.context = context;
        this.stockFilter = stockFilter;
        this.stockPrice = stockPrice;
        this.stockItemID = stockItemID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_details, parent, false);
        return new StockEntryAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockEntryModel stockEntry = stockEntries.get(position);
        holder.bind(stockEntry);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context,showStockEntry.class);
                i.putExtra("stockEntry",stockEntry);
                i.putExtra("stockFilter",stockFilter);
                i.putExtra("stockItemPrice",stockPrice);
                i.putExtra("stockItemID",stockItemID);
                context.startActivity(i);
                ((Activity) context).finish();

            }
        });
    }


    @Override
    public int getItemCount() {
        return stockEntries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate,tvOrderStatus,tvItem,tvQty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        private String formatDate(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(date);
        }
        public void bind(StockEntryModel stockEntry) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            tvDate = itemView.findViewById(R.id.tvstockDate);
            tvOrderStatus = itemView.findViewById(R.id.tvStockOrderStatus);
            tvItem = itemView.findViewById(R.id.tvStockItem);
            tvQty = itemView.findViewById(R.id.tvStockQty);

            if (stockEntry.getDate()!=null) {
                tvDate.setText(formatDate(stockEntry.getDate()));
            }
            if (stockEntry.getOrderStatus()!=null && !stockEntry.getOrderStatus().isEmpty()){
                tvOrderStatus.setText(stockEntry.getOrderStatus());
            }
            if (!stockEntry.getItem().isEmpty()){

                tvItem.setText(stockEntry.getItem());
            }
            if (stockEntry.getQty()!=null){
                tvQty.setText(String.valueOf(decimalFormat.format(stockEntry.getQty())));
            }
        }
    }
}
