# Backend - EMPLANORTE S.A.S.

Módulo de servidor construido utilizando **Java 17/21** y el framework **Spring Boot 3.x**. Provee una API REST JSON para conectar la interfaz de usuario con la base de datos PostgreSQL.

## 🛠️ Requisitos de Software
- **Java Development Kit (JDK):** Versión 17 o superior.
- **Maven:** Gestor de dependencias (se incluye `mvnw` en la raíz del backend).
- **IDE Recomendado:** IntelliJ IDEA / VS Code / Spring Tool Suite.

## 📂 Estructura Principal
- `config/`: Configuraciones del sistema (CORS, seguridad, JWT).
- `controller/`: Controladores REST que manejan las solicitudes HTTP del Frontend.
- `service/`: Capa de lógica de negocio (cálculos, validaciones, flujos).
- `repository/`: Interfaces Spring Data JPA para la base de datos PostgreSQL.
- `model/`: Entidades JPA que representan las tablas de la base de datos.
- `dto/`: Clases Data Transfer Object para transporte de información.

## 🚀 Cómo Iniciar en Desarrollo
1. Configurar la base de datos en `src/main/resources/application.properties`.
2. Compilar el proyecto:
   ```bash
   mvn clean install
   ```
3. Ejecutar la aplicación:
   ```bash
   mvn spring-boot:run
   ```
