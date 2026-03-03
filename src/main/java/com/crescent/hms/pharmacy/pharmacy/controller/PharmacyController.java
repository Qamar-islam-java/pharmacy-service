package com.crescent.hms.pharmacy.pharmacy.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HttpServletRequest request;

    // Helper method to forward the Authorization Token
    private HttpEntity<String> getEntityWithAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");

        if (token != null) {
            headers.set("Authorization", token);
        }
        headers.set("Content-Type", "application/json");

        return new HttpEntity<>(headers);
    }

    // --- ENDPOINT 1: Simplified Search (Used by React Dashboard) ---
    // Usage: GET /api/pharmacy/find?token=105
    @GetMapping("/find")
    public ResponseEntity<?> getPrescriptionByTokenNumber(@RequestParam Integer token) {

        // 1. Call Gateway -> Gateway routes to Appointment Service
        // Gateway Port: 8091
        String appointmentUrl = "http://appointment:8084/api/appointment/search-by-token?token=" + token;

        ResponseEntity<Map> aptResponse = restTemplate.exchange(
                appointmentUrl,
                HttpMethod.GET,
                getEntityWithAuthHeader(),
                Map.class
        );

        if (aptResponse.getBody() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found");
        }

        Long appointmentId = Long.valueOf(aptResponse.getBody().get("id").toString());

        // 2. Call Gateway -> Gateway routes to Medical Record Service
        String medicalUrl = "http://medical-record:8085/api/medical-record/appointment/" + appointmentId;
                //"http://localhost:8091/api/medical-record/appointment/" + appointmentId;

        ResponseEntity<Object> medResponse = restTemplate.exchange(
                medicalUrl,
                HttpMethod.GET,
                getEntityWithAuthHeader(),
                Object.class
        );

        return ResponseEntity.ok(medResponse.getBody());
    }

    // --- ENDPOINT 2: Old Search (Compatibility) ---
    @GetMapping("/prescription")
    public ResponseEntity<?> getPrescriptionByTokenAndDoctor(@RequestParam Integer token, @RequestParam Long doctorId) {

        String appointmentUrl = "http://appointment:8084/api/appointment/search?token=" + token + "&doctorId=" + doctorId;
                //"http://localhost:8091/api/appointment/search?token=" + token + "&doctorId=" + doctorId;

        ResponseEntity<Map> aptResponse = restTemplate.exchange(
                appointmentUrl,
                HttpMethod.GET,
                getEntityWithAuthHeader(),
                Map.class
        );

        if (aptResponse.getBody() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found");
        }

        Long appointmentId = Long.valueOf(aptResponse.getBody().get("id").toString());

        String medicalUrl = "http://medical-record:8085/api/medical-record/appointment/" + appointmentId;
                //"http://localhost:8091/api/medical-record/appointment/" + appointmentId;

        ResponseEntity<Object> medResponse = restTemplate.exchange(
                medicalUrl,
                HttpMethod.GET,
                getEntityWithAuthHeader(),
                Object.class
        );

        return ResponseEntity.ok(medResponse.getBody());
    }
}
/*
package com.crescent.hms.pharmacy.pharmacy.controller;

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
                .uri("http://localhost:8091/api/appointment/search?token=" + token + "&doctorId=" + doctorId)
                .retrieve()
                .bodyToMono(Map.class) // Expecting JSON with "id" (appointmentId)
                .flatMap(response -> {
                    Long appointmentId = Long.valueOf(response.get("id").toString());

                    // Step 2: Use Appointment ID to get Medical Record
                    return client.get()
                            .uri("http://localhost:8091/api/medical-record/appointment/" + appointmentId)
                            .retrieve()
                            .bodyToMono(Object.class);
                });
    }
    //--------------------------------------------------------------
    // Usage: GET /api/pharmacy/find?token=105
    @GetMapping("/find")
    public Mono<Object> getPrescriptionByTokenNumber(@RequestParam Integer token) {

        WebClient client = webClientBuilder.build();

        // Step 1: Find the Appointment ID using Token + DoctorID
        // We call the Appointment Service.
        return client.get()
                .uri("http://appointment-service/api/appointment/search?token=" + token)
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
//    @GetMapping("/find")
//    public Mono<Object> getPrescriptionByTokenOnly(@RequestParam Integer token) {
//        WebClient client = webClientBuilder
//                // Ensure token propagation if needed, though GET might be public
//                .defaultHeader(HttpHeaders.AUTHORIZATION, getCurrentAuthToken())
//                .build();
//
//        // 1. Find the Appointment by Token (Search globally, not just by doctor)
//        // Note: You might need to add a new method in AppointmentRepository: findByTokenNumber(Integer token)
//        return client.get()
//                .uri("lb://APPOINTMENT-SERVICE/api/appointment/search-by-token?token=" + token)
//                .retrieve()
//                .bodyToMono(Appointment.class) // Returns the full Appointment object
//                .flatMap(appointment -> {
//                    // 2. Use the Appointment ID to get the Medical Record
//                    return client.get()
//                            .uri("lb://MEDICAL-RECORD-SERVICE/api/medical-record/appointment/" + appointment.getId())
//                            .retrieve()
//                            .bodyToMono(Object.class);
//                });
//    }
//====================================================================================================//
*/
