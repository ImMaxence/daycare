package com.back.daycare.config;

import com.back.daycare.entity.Daycare;
import com.back.daycare.entity.DaycareStatus;
import com.back.daycare.entity.EstablishmentType;
import com.back.daycare.entity.User;
import com.back.daycare.repository.DaycareRepository;
import com.back.daycare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final int BATCH_SIZE = 500;

    private static final String MONENFANT_CSV_FILE = "daycares.geocoded.csv";
    private static final String SOURCE_MONENFANT = "MONENFANT";

    private static final String FINESS_CSV_FILE = "finess_geocoded_clean.csv";
    private static final String SOURCE_FINESS = "FINESS";

    // Ex: "20 LOT DES ALGUES 83120 STE MAXIME" -> voie / code postal / commune
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^(.*?)\\s+(\\d{5})\\s+(.+)$");
    // Ex: "20 LOT DES ALGUES" -> numéro / voie
    private static final Pattern HOUSE_NUMBER_PATTERN = Pattern.compile("^(\\d+\\s*[A-Za-z]{0,3})\\s+(.+)$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DaycareRepository daycareRepository;

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
            log.info("Des établissements sont déjà présents en base, ingestion CSV ignorée.");
            return;
        }

        List<Daycare> daycaresToInsert = new ArrayList<>();
        daycaresToInsert.addAll(loadFromMonEnfantCsv());
        daycaresToInsert.addAll(loadFromFinessCsv());

        int insertedCount = saveInBatches(daycaresToInsert);
        log.info("=== Rapport d'ingestion global ===");
        log.info("Total d'établissements insérés en base : {}", insertedCount);
    }

    // ------------------------------------------------------------------
    // monenfant.fr (daycares.geocoded.csv)
    // ------------------------------------------------------------------

    private List<Daycare> loadFromMonEnfantCsv() throws Exception {
        List<Daycare> daycares = new ArrayList<>();
        int ignoredCount = 0;
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = openResource(MONENFANT_CSV_FILE);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                total++;

                Double latitude = parseDouble(getField(record, "latitude"));
                Double longitude = parseDouble(getField(record, "longitude"));
                if (latitude == null || longitude == null) {
                    ignoredCount++;
                    continue;
                }

                String name = getField(record, "name");
                if (!StringUtils.hasText(name)) {
                    name = "Inconnu - " + UUID.randomUUID();
                }

                String postcode = firstNonBlank(getField(record, "result_postcode"));
                String city = firstNonBlank(getField(record, "result_city"), getField(record, "commune"));

                Daycare daycare = Daycare.builder()
                        .externalId(getField(record, "id"))
                        .type(parseMonEnfantType(getField(record, "type")))
                        .name(name)
                        .latitude(latitude)
                        .longitude(longitude)
                        .houseNumber(getField(record, "result_housenumber"))
                        .street(getField(record, "result_street"))
                        .postcode(postcode)
                        .city(city)
                        .department(departmentFromPostcode(postcode))
                        .phone(firstNonBlank(getField(record, "phone"), getField(record, "phone2")))
                        .email(getField(record, "mail"))
                        .websiteUrl(getField(record, "siteWeb"))
                        .source(SOURCE_MONENFANT)
                        .status(DaycareStatus.A_CONTACTER)
                        .build();

                daycares.add(daycare);
            }
        }

        log.info("=== Rapport d'ingestion monenfant.fr ({}) ===", MONENFANT_CSV_FILE);
        log.info("Total de lignes lues                                  : {}", total);
        log.info("Total d'établissements retenus                        : {}", daycares.size());
        log.info("Total de lignes ignorées (coordonnées manquantes)     : {}", ignoredCount);

        return daycares;
    }

    private EstablishmentType parseMonEnfantType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return EstablishmentType.AUTRE;
        }
        try {
            return EstablishmentType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Type monenfant.fr inconnu '{}', valeur AUTRE utilisée.", rawType);
            return EstablishmentType.AUTRE;
        }
    }

    // ------------------------------------------------------------------
    // FINESS (finess_geocoded_clean.csv)
    // ------------------------------------------------------------------

    private List<Daycare> loadFromFinessCsv() throws Exception {
        List<Daycare> daycares = new ArrayList<>();
        int ignoredCount = 0;
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = openResource(FINESS_CSV_FILE);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord record : parser) {
                total++;

                Double latitude = parseDouble(getField(record, "latitude"));
                Double longitude = parseDouble(getField(record, "longitude"));
                if (latitude == null || longitude == null) {
                    ignoredCount++;
                    continue;
                }

                String name = getField(record, "name");
                if (!StringUtils.hasText(name)) {
                    name = "Inconnu - " + UUID.randomUUID();
                }

                String[] parsedAddress = parseAddress(getField(record, "address"));
                String department = normalizeDepartment(getField(record, "department"));

                Daycare daycare = Daycare.builder()
                        .externalId(getField(record, "id"))
                        .type(parseFinessCategory(getField(record, "category")))
                        .name(name)
                        .latitude(latitude)
                        .longitude(longitude)
                        .houseNumber(parsedAddress[0])
                        .street(parsedAddress[1])
                        .postcode(parsedAddress[2])
                        .city(parsedAddress[3])
                        .department(department)
                        .source(SOURCE_FINESS)
                        .status(DaycareStatus.A_CONTACTER)
                        .build();

                daycares.add(daycare);
            }
        }

        log.info("=== Rapport d'ingestion FINESS ({}) ===", FINESS_CSV_FILE);
        log.info("Total de lignes lues                                  : {}", total);
        log.info("Total d'établissements retenus                        : {}", daycares.size());
        log.info("Total de lignes ignorées (coordonnées manquantes)     : {}", ignoredCount);

        return daycares;
    }

    private EstablishmentType parseFinessCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return EstablishmentType.AUTRE;
        }
        return switch (category.trim()) {
            case "166" -> EstablishmentType.CENTRE_MATERNEL;
            case "176" -> EstablishmentType.VILLAGE_ENFANTS;
            case "177" -> EstablishmentType.MECS;
            case "223" -> EstablishmentType.PMI;
            case "355" -> EstablishmentType.CENTRE_HOSPITALIER;
            default -> {
                log.warn("Catégorie FINESS inconnue '{}', valeur AUTRE utilisée.", category);
                yield EstablishmentType.AUTRE;
            }
        };
    }

    private String normalizeDepartment(String rawDepartment) {
        if (!StringUtils.hasText(rawDepartment)) {
            return null;
        }
        String trimmed = rawDepartment.trim();
        if (trimmed.length() == 1) {
            return "0" + trimmed;
        }
        return trimmed;
    }

    /**
     * Découpe une adresse à plat (ex: "20 LOT DES ALGUES 83120 STE MAXIME")
     * en numéro de voie, voie, code postal et commune.
     */
    private String[] parseAddress(String rawAddress) {
        if (!StringUtils.hasText(rawAddress)) {
            return new String[] { null, null, null, null };
        }

        String address = rawAddress.trim();
        Matcher addressMatcher = ADDRESS_PATTERN.matcher(address);
        if (!addressMatcher.matches()) {
            return new String[] { null, address, null, null };
        }

        String streetPart = addressMatcher.group(1).trim();
        String postcode = addressMatcher.group(2).trim();
        String city = addressMatcher.group(3).trim();

        Matcher houseNumberMatcher = HOUSE_NUMBER_PATTERN.matcher(streetPart);
        if (houseNumberMatcher.matches()) {
            return new String[] {
                    houseNumberMatcher.group(1).trim(),
                    houseNumberMatcher.group(2).trim(),
                    postcode,
                    city
            };
        }

        return new String[] { null, streetPart, postcode, city };
    }

    // ------------------------------------------------------------------
    // Utilitaires communs
    // ------------------------------------------------------------------

    private Reader openResource(String fileName) throws Exception {
        ClassPathResource resource = new ClassPathResource(fileName);
        return new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String getField(CSVRecord record, String name) {
        if (!record.isMapped(name)) {
            return null;
        }
        String value = record.get(name);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String departmentFromPostcode(String postcode) {
        if (!StringUtils.hasText(postcode) || postcode.length() < 2) {
            return null;
        }
        if (postcode.startsWith("97") || postcode.startsWith("98")) {
            return postcode.length() >= 3 ? postcode.substring(0, 3) : postcode.substring(0, 2);
        }
        return postcode.substring(0, 2);
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
}

