package com.sararoal.proyecto_tracker;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/track")
public class TrackerController {

    private final TrackingService trackingService;

    public TrackerController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingNumber}")
    public Map<String, Object> getTrackingInfo(
        @PathVariable String trackingNumber,
        @RequestParam(required = false) String carrier  // <- Añadido
) {
    return trackingService.getTrackingInfo(trackingNumber, carrier); // <- Cambiado
}

}

// --- Añade esto fuera de la clase TrackerController ---

@RestController
class CarrierController {

    @GetMapping("/carriers")
    public Map<String, Object> getCarriers() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://res.17track.net/asset/carrier/info/apicarrier.all.json";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}