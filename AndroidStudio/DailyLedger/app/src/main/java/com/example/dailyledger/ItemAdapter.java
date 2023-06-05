package com.example.dailyledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<ItemModel> itemslist;
    Context context;

    FirebaseFirestore db;
    private ProgressDialog progressDialog;
    DecimalFormat decimalFormat = new DecimalFormat("#.##");



    public ItemAdapter(@NonNull Context context, List<ItemModel> itemslist) {
        this.itemslist = itemslist;
        this.context = context;
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_items, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemModel itemModel = itemslist.get(position);
        holder.bind(itemModel);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                showInputItemDialog(itemModel.getItemName(), itemModel.getItemPrice(), itemModel.getItemStock(),itemModel.getItemID());
                Intent i = new Intent(context,stockReport.class);
                i.putExtra("stockFilter",itemModel.getItemName());
                i.putExtra("stockItemTotal",String.valueOf(decimalFormat.format(itemModel.getItemStock())));
                i.putExtra("stockItemPrice",String.valueOf(decimalFormat.format(itemModel.getItemPrice())));
                i.putExtra("stockItemID",itemModel.getItemID());
                context.startActivity(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemslist != null ? itemslist.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Define your ViewHolder views here
        private TextView tvitemname, tvitemprice, tvitemStock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvitemname = itemView.findViewById(R.id.tvitemname);
            tvitemprice = itemView.findViewById(R.id.tvitemprice);
            tvitemStock = itemView.findViewById(R.id.tvitemStock);
            // Initialize your ViewHolder views here
        }

        public void bind(ItemModel ItemEntry) {
            if (!ItemEntry.getItemName().isEmpty()) {
                tvitemname.setText(ItemEntry.getItemName());
            }
            if (ItemEntry.getItemPrice() !=null) {
                tvitemprice.setText(String.valueOf(decimalFormat.format(ItemEntry.getItemPrice())));
            }

            if (ItemEntry.getItemStock() != null) {
                tvitemStock.setText(String.valueOf(decimalFormat.format(ItemEntry.getItemStock())));
            }

        }
    }

    private void showInputItemDialog(String itemName, Double itemPrice, Double itemStock,String itemID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Details");
        builder.setMessage("Item: " + itemName);


        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_item_page, null);
        builder.setView(dialogView);

        final EditText price = dialogView.findViewById(R.id.etItemPrice);
        final EditText stock = dialogView.findViewById(R.id.etItemStock);

        price.setText(String.valueOf(itemPrice));
        stock.setText(String.valueOf(itemStock));

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.show();
                String edprice = "";
                String edStock = "";
                if (!price.getText().toString().isEmpty()){
                    edprice = price.getText().toString();
                }
                if (!stock.getText().toString().isEmpty()){
                    edStock = stock.getText().toString();
                }
                updateItemStock(edprice,edStock,itemID);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void updateItemStock(String price, String stock,String itemID) {
        progressDialog.show();
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String userID = sharedPreferences.getString("storedUserId","userID");

        Map<String,Object> map = new HashMap<>();
        if (!price.isEmpty()){
            map.put("ItemPrice",Double.parseDouble(price));
        }
        if (!stock.isEmpty()){
            map.put("ItemStock",Double.parseDouble(stock));
        }
        DocumentReference partiesRef = db.collection("ProfileCollection")
                .document(userID)
                .collection("Items").document(itemID);

        partiesRef.update(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(context,"stock Updated",Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    Intent i = new Intent(context,ItemsPage.class);
                    context.startActivity(i);
                    ((Activity) context).finish();
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PriceUpdate",e.getMessage().toString());
                });
    }



}
