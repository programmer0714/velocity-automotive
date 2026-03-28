package com.rhsoft.velocityautomotive.network;

import com.rhsoft.velocityautomotive.model.Car;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    // Obtener todos los carros
    @GET("get_cars.php")
    Call<List<Car>> getCars();

    // Buscar carros por texto
    @GET("get_cars.php")
    Call<List<Car>> searchCars(@Query("search") String query);

    // Obtener un carro por ID
    @GET("get_cars.php")
    Call<Car> getCarById(@Query("id") int id);
}