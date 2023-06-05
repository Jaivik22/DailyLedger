package com.example.dailyledger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PartyAdapter extends RecyclerView.Adapter<PartyAdapter.PartyViewHolder> {
    private List<PartyModel> partyModelList;
    Context context;

    // Constructor
    public PartyAdapter(Context context, List<PartyModel> partyModelList) {
        this.partyModelList = partyModelList;
        this.context = context;
    }

    // ViewHolder class
    public static class PartyViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewPartyName;
        public TextView textViewPartyTotal,tvLastDate;


        public PartyViewHolder(View itemView) {
            super(itemView);
            textViewPartyName = itemView.findViewById(R.id.textViewPartyName);
            textViewPartyTotal = itemView.findViewById(R.id.textViewPartyTotal);
            tvLastDate = itemView.findViewById(R.id.tvLastDate);
        }
    }

    @NonNull
    @Override
    public PartyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_party, parent, false);
        return new PartyViewHolder(itemView);
    }



    @Override
    public void onBindViewHolder(@NonNull PartyViewHolder holder, int position) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        PartyModel partyModel = partyModelList.get(position);
        if (!partyModel.getPartyName().isEmpty()) {
            holder.textViewPartyName.setText(partyModel.getPartyName());
        }
        if (partyModel.getPartyTotal()!=null) {
            holder.textViewPartyTotal.setText(String.valueOf(decimalFormat.format( partyModel.getPartyTotal())));
        }
        if (partyModel.getPartyTotal()>0) {
            holder.textViewPartyTotal.setTextColor(Color.parseColor("#0cba7b"));
        }
        else {
            holder.textViewPartyTotal.setTextColor(Color.parseColor("#FF0000"));
        }

        Date date = partyModel.getDate();
        if (date != null) {
            holder.tvLastDate.setText(formatDate(date));
        } else {
            holder.tvLastDate.setText("");
        }



        AppCompatActivity activity = (AppCompatActivity) context;

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(activity,"Click it",Toast.LENGTH_SHORT).show();
                Intent i = new Intent(activity,PartyDetails.class);
                Bundle b = new Bundle();
                b.putString("PartyName", partyModel.getPartyName());
                b.putDouble("PartyTotal", partyModel.getPartyTotal());
                b.putString("PartyID", partyModel.getPartyID());
                i.putExtras(b);
                context.startActivity(i);
            }
        });
    }

    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }
    @Override
    public int getItemCount() {
        return partyModelList.size();
    }
}
