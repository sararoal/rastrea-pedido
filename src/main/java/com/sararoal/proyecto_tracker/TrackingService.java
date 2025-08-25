package com.sararoal.proyecto_tracker;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TrackingService {

    @Value("${tracking.api.url}")
    private String apiUrl;

    @Value("${tracking.api.key}")
    private String apiKey;

    public Map<String, Object> getTrackingInfo(String trackingNumber, String carrier) {
        final RestTemplate restTemplate = new RestTemplate();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String cleanTrackingNumber = trackingNumber.trim();

        try {
            boolean canProceed = registerTrackingNumber(restTemplate, objectMapper, cleanTrackingNumber, carrier);

            if (canProceed) {
                Map<String, Object> apiResponse = fetchTrackingDetails(restTemplate, objectMapper, cleanTrackingNumber, carrier);
                return transformResponse(apiResponse, cleanTrackingNumber);
            } else {
                return createErrorResponse(cleanTrackingNumber, "No se pudo registrar el número de seguimiento en la API externa.");
            }
        } catch (RestClientException e) {
            e.printStackTrace();
            return createErrorResponse(cleanTrackingNumber, "Error de comunicación con la API de seguimiento.");
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(cleanTrackingNumber, "Error al procesar la respuesta de la API.");
        }
    }

    private boolean registerTrackingNumber(RestTemplate restTemplate, ObjectMapper mapper, String trackingNumber, String carrier) throws Exception {
        String registerUrl = apiUrl + "/register";

        Map<String, String> requestData = new HashMap<>();
        requestData.put("number", trackingNumber);
        if (carrier != null && !carrier.isEmpty()) {
            requestData.put("carrier", carrier);
        }
        List<Map<String, String>> body = List.of(requestData);

        ResponseEntity<String> response = executePostRequest(restTemplate, registerUrl, body);
        Map<String, Object> apiResponse = mapper.readValue(response.getBody(), new TypeReference<>() {});

        if (!"0".equals(String.valueOf(apiResponse.get("code")))) {
            System.err.println("API de registro devolvió un código de error: " + apiResponse);
            return false;
        }

        Map<String, Object> data = (Map<String, Object>) apiResponse.get("data");
        if (data == null) return false;

        List<Map<String, Object>> accepted = (List<Map<String, Object>>) data.get("accepted");
        if (accepted != null && accepted.stream().anyMatch(item -> trackingNumber.equals(item.get("number")))) {
            return true;
        }

        List<Map<String, Object>> rejected = (List<Map<String, Object>>) data.get("rejected");
        if (rejected != null) {
            for (Map<String, Object> item : rejected) {
                if (trackingNumber.equals(item.get("number"))) {
                    Map<String, Object> error = (Map<String, Object>) item.get("error");
                    if (error != null && Integer.valueOf(-18019901).equals(error.get("code"))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Map<String, Object> fetchTrackingDetails(RestTemplate restTemplate, ObjectMapper mapper, String trackingNumber, String carrier) throws Exception {
        String getInfoUrl = apiUrl + "/gettrackinfo";

        Map<String, String> requestData = new HashMap<>();
        requestData.put("number", trackingNumber);
        if (carrier != null && !carrier.isEmpty()) {
            requestData.put("carrier", carrier);
        }
        List<Map<String, String>> body = List.of(requestData);

        ResponseEntity<String> response = executePostRequest(restTemplate, getInfoUrl, body);
        System.out.println("Respuesta cruda de la API: " + response.getBody());
        return mapper.readValue(response.getBody(), new TypeReference<>() {});
    }

    private ResponseEntity<String> executePostRequest(RestTemplate restTemplate, String url, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("17token", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private Map<String, Object> transformResponse(Map<String, Object> apiResponse, String trackingNumber) {
        Map<String, Object> result = new HashMap<>();
        result.put("trackingNumber", trackingNumber);
        result.put("carrier", "Desconocido");
        List<Map<String, String>> eventsList = new ArrayList<>();

        if (apiResponse == null || !"0".equals(String.valueOf(apiResponse.get("code")))) {
            eventsList.add(Map.of("date", "", "status", "No se pudo obtener la información de seguimiento."));
            result.put("events", eventsList);
            return result;
        }

        try {
            Map<String, Object> data = (Map<String, Object>) apiResponse.get("data");
            if (data == null) return createErrorResponse(trackingNumber, "La respuesta de la API no contiene datos.");

            List<Map<String, Object>> accepted = (List<Map<String, Object>>) data.get("accepted");
            if (accepted == null || accepted.isEmpty()) return createErrorResponse(trackingNumber, "No se encontró información para el número de seguimiento.");

            Map<String, Object> trackData = accepted.stream()
                .filter(item -> trackingNumber.equals(item.get("number")))
                .findFirst()
                .orElse(null);

            if (trackData == null) return createErrorResponse(trackingNumber, "No se encontró información para el número de seguimiento.");

            Map<String, Object> trackInfo = (Map<String, Object>) trackData.get("track_info");
            if (trackInfo == null) return result;

            Map<String, Object> tracking = (Map<String, Object>) trackInfo.get("tracking");
            if (tracking == null) return result;

            List<Map<String, Object>> providers = (List<Map<String, Object>>) tracking.get("providers");
            if (providers == null || providers.isEmpty()) return result;

            Map<String, Object> providerInfo = providers.get(0);

            Map<String, Object> providerDetails = (Map<String, Object>) providerInfo.get("provider");
            if (providerDetails != null && providerDetails.get("name") != null) {
                result.put("carrier", providerDetails.get("name").toString());
            }

            List<Map<String, Object>> events = (List<Map<String, Object>>) providerInfo.get("events");
            if (events != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy HH:mm", new Locale("es", "ES"));
                for (Map<String, Object> event : events) {
                    Map<String, String> e = new HashMap<>();
                    // Formatear fecha y hora en español
                    String rawDate = Objects.toString(event.get("time_iso"), "");
                    String fechaFormateada = "";
                    if (!rawDate.isEmpty()) {
                        try {
                            ZonedDateTime zdt = ZonedDateTime.parse(rawDate);
                            fechaFormateada = zdt.format(formatter);
                        } catch (Exception ex) {
                            fechaFormateada = rawDate;
                        }
                    }
                    e.put("date", fechaFormateada);

                    // Información/estado
                    e.put("status", Objects.toString(event.get("description"), ""));

                    // Solo poner location si no está vacía
                    String location = Objects.toString(event.get("location"), "");
                    if (location != null && !location.trim().isEmpty()) {
                        e.put("location", location);
                    }

                    eventsList.add(e);
                }
            }
        } catch (ClassCastException | NullPointerException e) {
            e.printStackTrace();
            return createErrorResponse(trackingNumber, "Error al procesar la estructura de la respuesta de la API.");
        }

        if (eventsList.isEmpty()) {
            eventsList.add(Map.of("date", "", "status", "Aún no hay información de seguimiento disponible."));
        }

        result.put("events", eventsList);
        return result;
    }

    private Map<String, Object> createErrorResponse(String trackingNumber, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("trackingNumber", trackingNumber);
        error.put("carrier", "Desconocido");
        error.put("events", List.of(Map.of("date", "", "status", message)));
        return error;
    }
}