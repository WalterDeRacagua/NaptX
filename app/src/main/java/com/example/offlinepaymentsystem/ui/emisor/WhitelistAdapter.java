package com.example.offlinepaymentsystem.ui.emisor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.model.WhitelistItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class WhitelistAdapter extends BaseAdapter {

    private Context context;
    private List<WhitelistItem> items;

    public WhitelistAdapter(Context context, List<WhitelistItem> items){
        this.context = context;
        this.items = items;
    }

    /**
     * Sirve para que listview sepa cuantas veces llamará a getView()
     * */
    @Override
    public int getCount(){
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    /**
     * Crear/reutilizar vista para cada item
     * Este méthod se llama para cada item que se muestra en pantalla
     * position = posición del item
     * convertView = Vista reciclada para estos items
     * parent = list view padre
     * */
    public View getView(int position, View convertView, ViewGroup parent){
        View view = convertView;

        if (view == null){
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.item_whitelist, parent, false);
        }

        WhitelistItem item = items.get(position);

        TextView tvNombre = view.findViewById(R.id.tvNombre);
        TextView tvDireccion = view.findViewById(R.id.tvDireccion);
        TextView tvLimite = view.findViewById(R.id.tvLimite);

        String nombre = item.getNombre();
        if (nombre == null || nombre.isEmpty()){
            tvNombre.setText("Receptor "+ (position+1));
        } else  {
            tvNombre.setText(nombre);
        }

        String direccion = item.getDireccion();
        if (direccion.length() > 20){
            direccion = direccion.substring(0,10) + "..." +direccion.substring(direccion.length() -8);
        }

        tvDireccion.setText(direccion);

        long limiteWei = item.getLimite();
        BigDecimal limiteETH = new BigDecimal(limiteWei).divide(new BigDecimal("1000000000000000000"));
        tvLimite.setText("Límite: " + limiteETH.toPlainString()+ "ETH");
        return view;
    }

    public void actualizarDatos(List<WhitelistItem> nuevosItems) {
        this.items = nuevosItems;
        notifyDataSetChanged();
    }

}
