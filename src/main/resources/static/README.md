# Rastrea tu pedido 📦

Este proyecto es una aplicación web para consultar el estado de envíos y paquetes mediante número de seguimiento y compañía de transporte.

## Tecnologías usadas

- **Java 17**
- **Spring Boot** (backend REST)
- **HTML, CSS, JavaScript** (frontend)
- **Select2** (buscador en el selector de compañías)
- **Maven** (gestión de dependencias)

## ¿Cómo ponerlo en funcionamiento?

1. **Clona el repositorio:**
   ```sh
   git clone https://github.com/tuusuario/proyecto-tracker.git
   cd proyecto-tracker
   ```

2. **Configura las variables de entorno o el archivo `application.properties`**  
   (añade tu clave y URL de la API de tracking si es necesario).

3. **Compila y ejecuta el backend:**
   ```sh
   mvn clean package
   mvn spring-boot:run
   ```

4. **Abre tu navegador y accede a:**
   ```
   http://localhost:8080/
   ```