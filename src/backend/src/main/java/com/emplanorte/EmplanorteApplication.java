package com.emplanorte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class EmplanorteApplication {

    public static void main(String[] args) {
        // Render suele operar en UTC. La aplicación almacena TIMESTAMP sin zona,
        // por eso se fija Bogotá antes de crear fechas de negocio o auditoría.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        SpringApplication.run(EmplanorteApplication.class, args);
    }
}
