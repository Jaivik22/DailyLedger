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

public class DataEntryAdapter extends RecyclerView.Adapter<DataEntryAdapter.ViewHolder> {
    private List<DataEntryModel> dataEntries;
    Context context;
    Double PartyTotal;
    String PartyID = "partyID";
    String type = "";

    public DataEntryAdapter(Context context,List<DataEntryModel> dataEntries,Double PartyTotal,String PartyID,String type) {
        this.dataEntries = dataEntries;
        this.context = context;
        this.PartyTotal = PartyTotal;
        this.PartyID = PartyID;
        this.type = type;
    }
    public DataEntryAdapter(Context context,List<DataEntryModel> dataEntries) {
        this.dataEntries = dataEntries;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_party_details, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataEntryModel dataEntry = dataEntries.get(position);
        holder.bind(dataEntry);

        holder.itemView.setOnClickListener(view -> {
            if (context != null) {
                Intent i = new Intent(context, showDataEntry.class);
                i.putExtra("dataEntry", dataEntry);
                i.putExtra("PartyTotal", PartyTotal);
                if (!PartyID.equals("")) {
                    i.putExtra("PartyID", PartyID);
                }
                context.startActivity(i);
                ((Activity) context).finish();
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataEntries.size();
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Define your ViewHolder views here
        private TextView dateTextView,orderStatusTextView,itemTotaltv,itemtv,weighttv,pricetv,partyNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize your ViewHolder views here
        }

        private String formatDate(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(date);
        }
        public void bind(DataEntryModel dataEntry) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            dateTextView = itemView.findViewById(R.id.dateTextView);
            orderStatusTextView = itemView.findViewById(R.id.orderStatusTextView);
            itemTotaltv = itemView.findViewById(R.id.ItemTotal);
            itemtv = itemView.findViewById(R.id.itemTextView);
            weighttv = itemView.findViewById(R.id.weightTextView);
            pricetv = itemView.findViewById(R.id.priceTextView);
            partyNameTextView = itemView.findViewById(R.id.partyNameTextView);

            dateTextView.setText(formatDate(dataEntry.getDate()));
            orderStatusTextView.setText(dataEntry.getOrderStatus());

            if (dataEntry.getOrderStatus().equals("Sell")){
                itemTotaltv.setTextColor(context.getResources().getColor(R.color.dark_green));
                orderStatusTextView.setTextColor(context.getResources().getColor(R.color.dark_green));
            }
            if (dataEntry.getOrderStatus().equals("Purchase")){
                itemTotaltv.setTextColor(context.getResources().getColor(R.color.navy_blue));
                orderStatusTextView.setTextColor(context.getResources().getColor(R.color.navy_blue));
            }
            if (dataEntry.getOrderStatus().equals("Payment In")){
                itemTotaltv.setTextColor(context.getResources().getColor(R.color.orange));
                orderStatusTextView.setTextColor(context.getResources().getColor(R.color.orange));
            }
            if (dataEntry.getOrderStatus().equals("Payment Out")){
                itemTotaltv.setTextColor(context.getResources().getColor(R.color.dark_red));
                orderStatusTextView.setTextColor(context.getResources().getColor(R.color.dark_red));
            }
            itemTotaltv.setText(String.valueOf(decimalFormat.format(dataEntry.getTotal())));

            if (!dataEntry.getItem().isEmpty()) {
                itemtv.setText(dataEntry.getItem());
            }
            if (dataEntry.getQty()!=null) {
                weighttv.setText(String.valueOf(decimalFormat.format(dataEntry.getQty())));
            }
            if (dataEntry.getPrice()!=null) {
                pricetv.setText(String.valueOf(decimalFormat.format(dataEntry.getPrice())));
            }
            if ("specific".equals(type)) {
                partyNameTextView.setVisibility(View.GONE);
            }

            partyNameTextView.setText(dataEntry.getPartyName());
        }
    }
}
