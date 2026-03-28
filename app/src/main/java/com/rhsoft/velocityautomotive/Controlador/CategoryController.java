package com.rhsoft.velocityautomotive.Controlador;

import com.rhsoft.velocityautomotive.Data.Category;
import com.rhsoft.velocityautomotive.Data.DatabaseSingleton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Request;
import okhttp3.Response;

public class CategoryController {

    public interface CategoryCallback {
        void onSuccess(List<Category> categories);
        void onError(String error);
    }

    public void getCategorias(CategoryCallback callback) {
        new Thread(() -> {
            try {
                DatabaseSingleton db = DatabaseSingleton.getInstance();

                Request request = db.getRequestBuilder(
                        "/rest/v1/cars?select=category&order=category.asc"
                ).get().build();

                Response response = db.getClient().newCall(request).execute();
                String body = response.body().string();

                JSONArray arr = new JSONArray(body);
                List<Category> categorias = new ArrayList<>();
                List<String> yaAgregadas = new ArrayList<>();

                String[] iconos = {"🏎️", "🚀", "🏁", "⚡", "💎"};
                int iconIndex = 0;

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String nombre = obj.optString("category", "");

                    if (!nombre.isEmpty() && !yaAgregadas.contains(nombre)) {
                        yaAgregadas.add(nombre);
                        String icono = iconos[iconIndex % iconos.length];
                        categorias.add(new Category(iconIndex + 1, nombre, icono));
                        iconIndex++;
                    }
                }

                callback.onSuccess(categorias);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}