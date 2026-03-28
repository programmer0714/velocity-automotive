package com.rhsoft.velocityautomotive.Vista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rhsoft.velocityautomotive.Controlador.CategoryController;
import com.rhsoft.velocityautomotive.Data.Category;
import com.rhsoft.velocityautomotive.R;
import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private ProgressBar progressBar;
    private CategoryController controller;
    private List<Category> categorias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        rvCategories = findViewById(R.id.rvCategories);
        progressBar  = findViewById(R.id.progressBar);
        controller   = new CategoryController();

        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadCategorias();
    }

    private void loadCategorias() {
        progressBar.setVisibility(View.VISIBLE);

        controller.getCategorias(new CategoryController.CategoryCallback() {
            @Override
            public void onSuccess(List<Category> list) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    categorias.clear();
                    categorias.addAll(list);
                    setupAdapter();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CategoryActivity.this,
                            "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupAdapter() {
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter =
                new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = getLayoutInflater().inflate(
                                R.layout.item_category, parent, false);
                        return new RecyclerView.ViewHolder(view) {};
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                        Category cat = categorias.get(position);
                        TextView tvIcon = holder.itemView.findViewById(R.id.tvCategoryIcon);
                        TextView tvName = holder.itemView.findViewById(R.id.tvCategoryName);

                        tvIcon.setText(cat.getIcono());
                        tvName.setText(cat.getNombre());

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(CategoryActivity.this,
                                    CarListByCategoryActivity.class);
                            intent.putExtra("category", cat.getNombre());
                            intent.putExtra("icono",    cat.getIcono());
                            startActivity(intent);
                            overridePendingTransition(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out);
                        });
                    }

                    @Override
                    public int getItemCount() { return categorias.size(); }
                };

        rvCategories.setAdapter(adapter);
    }
}