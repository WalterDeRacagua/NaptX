package com.example.offlinepaymentsystem.ui.emisor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.model.WhitelistItem;
import com.example.offlinepaymentsystem.utils.CryptoUtils;

import java.math.BigDecimal;
import java.util.List;

public class ReceptorSpinnerAdapter extends ArrayAdapter<WhitelistItem> {

    private final Context context;
    private final List<WhitelistItem> receptores;

    public ReceptorSpinnerAdapter(Context context, List<WhitelistItem> receptores) {
        super(context, 0, receptores);
        this.context = context;
        this.receptores = receptores;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent, true);
    }

    private View createView(int position, View convertView, ViewGroup parent, boolean isDropDown) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(
                    isDropDown ? android.R.layout.simple_spinner_dropdown_item : android.R.layout.simple_spinner_item,
                    parent,
                    false
            );
        }

        WhitelistItem item = receptores.get(position);
        TextView textView = (TextView) convertView;

        // Formato: "Nombre - Límite: X ETH"
        String nombre = item.getNombre();
        if (nombre == null || nombre.isEmpty()) {
            nombre = acortarDireccion(item.getDireccion());
        }

        long limiteWei = item.getLimite();
        BigDecimal limiteETH = CryptoUtils.convertirWeiAETHBigDecimal(limiteWei);

        String texto = nombre + " - Límite: " + limiteETH.toPlainString() + " ETH";
        textView.setText(texto);
        textView.setTextColor(context.getResources().getColor(R.color.text_on_dark, null));

        return convertView;
    }

    private String acortarDireccion(String direccion) {
        if (direccion.length() > 20) {
            return direccion.substring(0, 10) + "..." + direccion.substring(direccion.length() - 8);
        }
        return direccion;
    }
}