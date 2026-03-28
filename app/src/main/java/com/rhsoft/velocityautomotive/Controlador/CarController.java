package com.rhsoft.velocityautomotive.Controlador;

import com.rhsoft.velocityautomotive.Data.DatabaseSingleton;
import com.rhsoft.velocityautomotive.model.Car;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Request;
import okhttp3.Response;

public class CarController {

    public interface CarCallback {
        void onSuccess(List<Car> cars);
        void onError(String error);
    }

    public void getCarsByCategory(String category, CarCallback callback) {
        new Thread(() -> {
            try {
                DatabaseSingleton db = DatabaseSingleton.getInstance();

                String endpoint = "/rest/v1/cars?category=eq." + category + "&select=*";

                Request request = db.getRequestBuilder(endpoint)
                        .get()
                        .build();

                Response response = db.getClient().newCall(request).execute();
                String body = response.body().string();

                JSONArray arr = new JSONArray(body);
                List<Car> cars = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.getJSONObject(i);
                    Car car = new Car();
                    car.setId(c.optInt("id"));
                    car.setBrand(c.optString("brand"));
                    car.setModel(c.optString("model"));
                    car.setLocation(c.optString("location"));
                    car.setEngine(c.optString("engine"));
                    car.setPrice(c.optString("price"));
                    car.setHorsepower(String.valueOf(c.optInt("horsepower")));
                    car.setAcceleration(c.optString("acceleration"));
                    car.setTopSpeed(String.valueOf(c.optInt("top_speed")));
                    car.setImageUrl(c.optString("image_url"));
                    car.setCategory(c.optString("category"));
                    car.setYear(c.optInt("year"));
                    cars.add(car);
                }

                callback.onSuccess(cars);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}