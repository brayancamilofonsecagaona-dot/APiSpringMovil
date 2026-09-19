package com.lecto.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration //e dice a Spring que cree esta clase al arrancar
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    // El contenido completo del JSON de la cuenta de servicio, como variable de entorno
    @Value("${FIREBASE_CREDENTIALS}") //inyecta la variable de entorno
    private String credencialesJson;

    // @PostConstruct: se ejecuta una vez, después de que Spring crea este bean
    @PostConstruct //marca el metodo que corre una vez, después de inyectar los campos
    public void inicializarFirebase() throws IOException {

        // initializeApp lanza excepción si se llama dos veces, así que verificamos primero
        if (!FirebaseApp.getApps().isEmpty()) {
            return;

        }

        // GoogleCredentials necesita un InputStream; convertimos el texto del JSON
        InputStream credenciales = new ByteArrayInputStream(
                credencialesJson.getBytes(StandardCharsets.UTF_8));

        FirebaseOptions opciones = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credenciales))
                .setHttpTransport(new NetHttpTransport())
                .build();

        FirebaseApp.initializeApp(opciones);
        log.info("Firebase inicializado correctamente");
    }
}