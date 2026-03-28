# Velocity Automotive

App móvil Android para explorar y gestionar un catálogo de autos de lujo.

## Descripción

Velocity Automotive es una aplicación Android desarrollada en Java que permite a los usuarios explorar un catálogo de vehículos premium, guardar favoritos y agendar citas de prueba de manejo.

## Funcionalidades

- 🔐 Login y Registro de usuarios
- 👁️ Modo invitado (sin registro)
- 🚗 Catálogo de autos con filtros por marca
- 🔍 Búsqueda en tiempo real
- ❤️ Sistema de favoritos
- 📅 Agendado de citas
- 👤 Perfil de usuario
- 🌐 Panel web de administración

## Stack Tecnológico

| Tecnología | Uso |
|------------|-----|
| Java | Desarrollo Android |
| Android Studio | IDE |
| Supabase | Base de datos en la nube |
| OkHttp3 | Peticiones HTTP |
| Glide | Carga de imágenes |
| Material Design | Interfaz de usuario |
| HTML + CSS + JS | Panel web admin |


## 🏗️ Arquitectura
```
App Android (Java)
      ↕ HTTP REST (OkHttp)
Supabase (PostgreSQL)
      ↕
Panel Web Admin (HTML + JS)
```

## 📂 Estructura del Proyecto
```
app/src/main/java/com/rhsoft/velocityautomotive/
├── MainActivity.java
├── LoginActivity.java
├── RegisterActivity.java
├── CarDetailActivity.java
├── AppointmentActivity.java
├── FavoritosActivity.java
├── CitasActivity.java
├── ProfileActivity.java
├── SplashActivity.java
├── adapter/
│   ├── CarAdapter.java
│   └── CarThumbnailAdapter.java
├── model/
│   └── Car.java
├── network/
│   ├── ApiClient.java
│   └── ApiResponse.java
├── Controlador/
│   ├── CarController.java
│   └── CategoryController.java
├── Data/
│   ├── Category.java
│   └── DatabaseSingleton.java
└── Vista/
    ├── CategoryActivity.java
    └── CarListByCategoryActivity.java
```

## 🗄️ Base de Datos

4 tablas en Supabase:

| Tabla | Descripción |
|-------|-------------|
| users | Usuarios registrados |
| cars | Catálogo de autos |
| favorites | Favoritos por usuario |
| appointments | Citas agendadas |

## 🚀 Instalación

1. Clona el repositorio
```bash
git clone https://github.com/programmer0714/velocity-automotive.git
```
2. Abre en Android Studio

3. Crea tu propio proyecto en Supabase

4. Actualiza las credenciales en `ApiClient.java`
```java
public static final String SUPABASE_URL = "TU_URL";
public static final String SUPABASE_KEY = "TU_KEY";
```
5. Ejecuta la app en emulador o dispositivo físico

## 🚗 Marcas disponibles

- Ferrari
- Lamborghini  
- Porsche
- McLaren
- Bugatti
- Aston Martin

## 👨‍💻 Desarrollado por

**programmer0714** — Ingeniería de Software con IA — SENATI

## 📄 Licencia

MIT License — ver archivo LICENSE para más detalles
```

---

## ¿Cómo agregarlo a GitHub?
```
1. En tu repositorio en GitHub
2. Clic en "Add a README"
3. Pega todo el contenido
4. Clic "Commit changes" 
