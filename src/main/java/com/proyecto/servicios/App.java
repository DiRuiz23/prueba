package com.proyecto.servicios;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;



@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@Slf4j

public class App implements CommandLineRunner {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) {
        if (buildProperties != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
            log.info("Iniciando {} v{} (Compilado: {})",
                    buildProperties.getName(),
                    buildProperties.getVersion(),
                    formatter.format(buildProperties.getTime()));
        }
    }
}
