package com.inventrax.karthikm.merlinwmscipher_vip_rdc.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inventrax.karthikm.merlinwmscipher_vip_rdc.Pojos.InventoryDTO;
import com.inventrax.karthikm.merlinwmscipher_vip_rdc.R;

import java.util.List;

public class PalletInfoAdapter extends   RecyclerView.Adapter{


    private List<InventoryDTO> liveStockList;

    Context context;
    public PalletInfoAdapter(Context context, List<InventoryDTO> list) {
        this.context = context;
        this.liveStockList = list;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView txtSerialNo,txtQty;// init the item view's

        public MyViewHolder(View itemView) {

            super(itemView);
            // get the reference of item view's
            txtSerialNo = (TextView) itemView.findViewById(R.id.txtSerialNo);
            txtQty = (TextView) itemView.findViewById(R.id.txtQty);

        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // infalte the item Layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_palletinfo, parent, false);

        // set the view's size, margins, paddings and layout parameters
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        InventoryDTO inventoryDTO = (InventoryDTO) liveStockList.get(position);

        ((MyViewHolder) holder).txtSerialNo.setText(inventoryDTO.getProductSerialNumber());
        ((MyViewHolder) holder).txtQty.setText(inventoryDTO.getQuantity());


    }


    @Override
    public int getItemCount() {
        return liveStockList.size();
    }

}
