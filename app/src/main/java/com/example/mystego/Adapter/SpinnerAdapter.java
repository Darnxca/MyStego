package com.example.mystego.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mystego.firebase.model.User.User;

import java.util.ArrayList;

/**
 *  Classe che permette di costumizzare lo spinner (select) degli utenti
 */
public class SpinnerAdapter extends ArrayAdapter<User> {

    private final User[] values;

    public SpinnerAdapter(Context context, int textViewResourceId, ArrayList<User> values){
        super(context,textViewResourceId,values);
        this.values = values.toArray(new User[0]);
    }

    public int getCount(){ return values.length; }

    public User getItem(int position){ return values[position];}

    public long getItemId(int position){ return position; }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        TextView label = (TextView) super.getView(position,convertView, parent);
        label.setTextColor(Color.BLACK);

        label.setText(values[position].getEmail());
        return label;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position,convertView,parent);
        label.setTextColor(Color.BLACK);

        label.setText(values[position].getEmail());
        return label;
    }
}
