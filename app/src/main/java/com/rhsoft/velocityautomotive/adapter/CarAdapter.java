package com.rhsoft.velocityautomotive.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rhsoft.velocityautomotive.R;
import com.rhsoft.velocityautomotive.model.Car;
import java.util.List;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    public interface OnCarClickListener {
        void onCarClick(Car car, int position);
    }

    private final Context context;
    private final List<Car> cars;
    private final OnCarClickListener listener;

    public CarAdapter(Context context, List<Car> cars, OnCarClickListener listener) {
        this.context  = context;
        this.cars     = cars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = cars.get(position);

        // Marca + Modelo
        String title = (car.getBrand() != null ? car.getBrand() : "") + " "
                + (car.getModel() != null ? car.getModel() : "");
        holder.tvCarName.setText(title.trim());

        // Motor
        if (holder.tvEngine != null)
            holder.tvEngine.setText(car.getEngine() != null ? car.getEngine() : "—");

        // Potencia
        if (holder.tvHorsepower != null)
            holder.tvHorsepower.setText(car.getHorsepower() != null
                    ? car.getHorsepower() + " HP" : "—");

        // Precio
        if (holder.tvPrice != null)
            holder.tvPrice.setText(car.getPrice() != null
                    ? "$" + car.getPrice() : "Consultar");

        // Imagen con Glide
        if (holder.ivCar != null) {
            Glide.with(context)
                    .load(car.getImageUrl())
                    .placeholder(R.drawable.ic_car_placeholder)
                    .error(R.drawable.ic_car_placeholder)
                    .centerCrop()
                    .into(holder.ivCar);
        }

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCarClick(car, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return cars != null ? cars.size() : 0;
    }

    // ── ViewHolder ──────────────────────────────────────────
    static class CarViewHolder extends RecyclerView.ViewHolder {
        TextView  tvCarName, tvEngine, tvHorsepower, tvPrice;
        ImageView ivCar;

        CarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarName    = itemView.findViewById(R.id.tvCarName);
            tvEngine     = itemView.findViewById(R.id.tvEngine);
            tvHorsepower = itemView.findViewById(R.id.tvHorsepower);
            tvPrice      = itemView.findViewById(R.id.tvPrice);
            ivCar        = itemView.findViewById(R.id.ivCar);
        }
    }
}