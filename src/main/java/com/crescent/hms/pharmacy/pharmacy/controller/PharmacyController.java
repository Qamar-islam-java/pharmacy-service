package com.crescent.hms.pharmacy.pharmacy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    // Base URLs for other services (using Gateway or direct Eureka discovery)
    // Ideally, we use Gateway for security, but for internal microservice comms, direct is faster.
    // Let's use Gateway (port 8081) so the Auth filter applies to everything.
    private final WebClient.Builder webClientBuilder;

    public PharmacyController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    // 1. Get Prescription by Token
    // Usage: GET /api/pharmacy/prescription?token=1&doctorId=1
    @GetMapping("/prescription")
    public Mono<Object> getPrescriptionByToken(@RequestParam Integer token, @RequestParam Long doctorId) {

        WebClient client = webClientBuilder.build();

        // Step 1: Find the Appointment ID using Token + DoctorID
        // We call the Appointment Service.
        return client.get()
                .uri("http://pharmacy-service/api/appointment/search?token=" + token + "&doctorId=" + doctorId)
                .retrieve()
                .bodyToMono(Map.class) // Expecting JSON with "id" (appointmentId)
                .flatMap(response -> {
                    Long appointmentId = Long.valueOf(response.get("id").toString());

                    // Step 2: Use Appointment ID to get Medical Record
                    return client.get()
                            .uri("http://medical-record-service/api/medical-record/appointment/" + appointmentId)
                            .retrieve()
                            .bodyToMono(Object.class);
                });
    }

}
