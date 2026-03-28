package com.rhsoft.velocityautomotive.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rhsoft.velocityautomotive.R;
import java.util.List;

public class CarThumbnailAdapter extends RecyclerView.Adapter<CarThumbnailAdapter.ThumbViewHolder> {

    // Interfaz que notifica a la Activity qué thumbnail tocó el usuario
    // y qué URL de imagen corresponde a esa posición
    public interface OnThumbClickListener {
        void onThumbClick(String imageUrl, int position);
    }

    private final Context context;
    private final List<String> imageUrls;
    private final OnThumbClickListener listener;

    // Guarda la posición del thumbnail actualmente seleccionado
    // por defecto el primero (posición 0) está seleccionado
    private int selectedPosition = 0;

    // Constructor — recibe el contexto, la lista de URLs y el listener
    public CarThumbnailAdapter(Context context, List<String> imageUrls, OnThumbClickListener listener) {
        this.context   = context;
        this.imageUrls = imageUrls;
        this.listener  = listener;
    }

    // Infla el layout de cada thumbnail (item_car_thumbnail.xml)
    // Se llama solo cuando RecyclerView necesita una vista nueva
    @NonNull
    @Override
    public ThumbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_car_thumbnail, parent, false);
        return new ThumbViewHolder(view);
    }

    // Asigna los datos a cada thumbnail visible en pantalla
    // Se llama por cada imagen en la lista
    @Override
    public void onBindViewHolder(@NonNull ThumbViewHolder holder, int position) {
        String url = imageUrls.get(position);

        // Glide carga la imagen desde internet con placeholder
        // si la URL falla muestra el ícono de carro por defecto
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_car_placeholder)
                .error(R.drawable.ic_car_placeholder)
                .centerCrop()
                .into(holder.ivThumb);

        // Efecto visual — el thumbnail seleccionado se ve
        // completo (alpha=1, escala=1) y los demás se ven
        // opacos y más pequeños (alpha=0.5, escala=0.85)
        holder.itemView.setAlpha(selectedPosition == position ? 1f : 0.5f);
        holder.itemView.setScaleX(selectedPosition == position ? 1f : 0.85f);
        holder.itemView.setScaleY(selectedPosition == position ? 1f : 0.85f);

        // Al tocar un thumbnail — actualiza la selección
        // notifica al item anterior y al nuevo para que
        // actualicen su efecto visual, luego avisa al listener
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;       // guardar posición anterior
            selectedPosition = holder.getAdapterPosition(); // nueva posición
            notifyItemChanged(prev);           // actualizar thumbnail anterior
            notifyItemChanged(selectedPosition); // actualizar thumbnail nuevo
            if (listener != null) listener.onThumbClick(url, selectedPosition);
        });
    }

    // Indica cuántos thumbnails tiene la lista
    // si la lista es null retorna 0 para evitar crash
    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    // ViewHolder — guarda la referencia al ImageView de cada thumbnail
    // evita llamar findViewById() repetidamente mejorando el rendimiento
    static class ThumbViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;

        ThumbViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
        }
    }
}