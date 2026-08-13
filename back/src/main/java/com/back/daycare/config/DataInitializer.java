package com.back.daycare.config;

import com.back.daycare.entity.Daycare;
import com.back.daycare.entity.DaycareStatus;
import com.back.daycare.entity.User;
import com.back.daycare.repository.DaycareRepository;
import com.back.daycare.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final int BATCH_SIZE = 500;
    private static final String OSM_EXPORT_FILE = "export.json";
    private static final String DEFAULT_SOURCE = "OSM";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DaycareRepository daycareRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.default-user.username:admin}")
    private String defaultUsername;

    @Value("${app.default-user.password:admin123}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initDefaultUser();
        initDaycares();
    }

    private void initDefaultUser() {
        if (userRepository.existsByUsername(defaultUsername)) {
            log.info("Utilisateur par défaut '{}' déjà présent, aucune action.", defaultUsername);
            return;
        }

        User user = User.builder()
                .username(defaultUsername)
                .password(passwordEncoder.encode(defaultPassword))
                .build();

        userRepository.save(user);
        log.info("Utilisateur par défaut '{}' créé avec succès.", defaultUsername);
    }

    private void initDaycares() throws Exception {
        if (daycareRepository.count() > 0) {
            log.info("Des crèches sont déjà présentes en base, ingestion de '{}' ignorée.", OSM_EXPORT_FILE);
            return;
        }

        OsmResponse osmResponse = loadOsmResponse();
        List<OsmElement> elements = osmResponse.getElements() != null ? osmResponse.getElements() : List.of();
        int totalElements = elements.size();

        int generatedNameCount = 0;
        int ignoredCount = 0;
        List<Daycare> daycaresToInsert = new ArrayList<>();

        for (OsmElement element : elements) {
            double[] coordinates = resolveCoordinates(element);
            if (coordinates == null) {
                ignoredCount++;
                continue;
            }

            Map<String, String> tags = element.getTags() != null ? element.getTags() : Map.of();

            String name = tags.get("name");
            boolean nameGenerated = !StringUtils.hasText(name);
            if (nameGenerated) {
                name = "Inconnu - " + UUID.randomUUID();
            }

            String source = tags.getOrDefault("source", DEFAULT_SOURCE);

            Daycare daycare = Daycare.builder()
                    .osmId(element.getId())
                    .name(name)
                    .latitude(coordinates[0])
                    .longitude(coordinates[1])
                    .houseNumber(tags.get("addr:housenumber"))
                    .street(tags.get("addr:street"))
                    .postcode(tags.get("addr:postcode"))
                    .city(tags.get("addr:city"))
                    .phone(tags.get("phone"))
                    .operator(tags.get("operator"))
                    .siret(tags.get("ref:FR:SIRET"))
                    .note(tags.get("note"))
                    .source(source)
                    .status(DaycareStatus.A_CONTACTER)
                    .build();

            daycaresToInsert.add(daycare);
            if (nameGenerated) {
                generatedNameCount++;
            }
        }

        int insertedCount = saveInBatches(daycaresToInsert);

        log.info("=== Rapport d'ingestion OSM ({}) ===", OSM_EXPORT_FILE);
        log.info("Total d'objets présents dans le JSON                  : {}", totalElements);
        log.info("Total de crèches insérées en base                     : {}", insertedCount);
        log.info("Total de noms auto-générés (UUID)                     : {}", generatedNameCount);
        log.info("Total d'objets ignorés (coordonnées non exploitables) : {}", ignoredCount);
    }

    private OsmResponse loadOsmResponse() throws Exception {
        ClassPathResource resource = new ClassPathResource(OSM_EXPORT_FILE);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, OsmResponse.class);
        }
    }

    private double[] resolveCoordinates(OsmElement element) {
        if (element.getLat() != null && element.getLon() != null) {
            return new double[] { element.getLat(), element.getLon() };
        }

        Bounds bounds = element.getBounds();
        if (bounds != null && bounds.getMinlat() != null && bounds.getMaxlat() != null
                && bounds.getMinlon() != null && bounds.getMaxlon() != null) {
            double centerLat = (bounds.getMinlat() + bounds.getMaxlat()) / 2.0;
            double centerLon = (bounds.getMinlon() + bounds.getMaxlon()) / 2.0;
            return new double[] { centerLat, centerLon };
        }

        return null;
    }

    private int saveInBatches(List<Daycare> daycares) {
        int inserted = 0;
        for (int i = 0; i < daycares.size(); i += BATCH_SIZE) {
            List<Daycare> batch = daycares.subList(i, Math.min(i + BATCH_SIZE, daycares.size()));
            daycareRepository.saveAll(batch);
            inserted += batch.size();
        }
        return inserted;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OsmResponse {
        private List<OsmElement> elements;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OsmElement {
        private String type;
        private Long id;
        private Double lat;
        private Double lon;
        private Bounds bounds;
        private Map<String, String> tags;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Bounds {
        private Double minlat;
        private Double minlon;
        private Double maxlat;
        private Double maxlon;
    }
}

