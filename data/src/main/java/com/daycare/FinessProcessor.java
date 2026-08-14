package com.daycare;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Filtre, convertit et nettoie le fichier national FINESS (finess.csv).
 *
 * Le fichier source contient plusieurs types de lignes distinguées par leur
 * premier champ ("structureet", "geolocalisation", ...). On associe chaque
 * structure à ses coordonnées Lambert-93 via son identifiant FINESS, on filtre
 * sur les départements et catégories voulus, on convertit les coordonnées en
 * WGS84 (latitude/longitude) avec Proj4J, puis on exporte le résultat en CSV.
 *
 * Usage: java -cp ... com.daycare.FinessProcessor [inputCsv] [outputCsv]
 *   inputCsv  par défaut : input/finess.csv
 *   outputCsv par défaut : output/finess_geocoded_clean.csv
 */
public class FinessProcessor {

    private static final String ROW_TYPE_STRUCTURE = "structureet";
    private static final String ROW_TYPE_GEOLOCATION = "geolocalisation";

    // Index des champs (0-based) dans les lignes "structureet".
    private static final int IDX_STRUCT_ID = 1;
    private static final int IDX_STRUCT_NAME = 3;
    private static final int IDX_STRUCT_ADDR_NUMERO = 7;
    private static final int IDX_STRUCT_ADDR_TYPE_VOIE = 8;
    private static final int IDX_STRUCT_ADDR_NOM_VOIE = 9;
    private static final int IDX_STRUCT_DEPARTMENT = 13;
    private static final int IDX_STRUCT_ADDR_POSTAL_CITY = 15;
    private static final int IDX_STRUCT_CATEGORY = 18;

    // Index des champs (0-based) dans les lignes "geolocalisation".
    private static final int IDX_GEO_ID = 1;
    private static final int IDX_GEO_X = 2;
    private static final int IDX_GEO_Y = 3;

    // Départements retenus : 13 (Bouches-du-Rhône), 84 (Vaucluse), 83 (Var), 04 (Alpes-de-Haute-Provence).
    private static final Set<String> DEPARTMENTS_FILTER = Set.of("13", "84", "83", "4");

    // Catégories retenues : PMI, MECS, Village d'enfants, Centre parents-enfants,
    // Jardin d'enfants spécialisé, Centres hospitaliers.
    private static final Set<String> CATEGORIES_FILTER = Set.of("223", "177", "176", "166", "402", "355");

    private static final String[] OUTPUT_COLUMNS = {
            "id", "name", "address", "department", "category", "longitude", "latitude"
    };

    public static void main(String[] args) throws Exception {
        Path inputPath = Path.of(args.length > 0 ? args[0] : "input/finess.csv");
        Path outputPath = Path.of(args.length > 1 ? args[1] : "output/finess_geocoded_clean.csv");

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        System.out.println("Lecture de " + inputPath + " ...");
        Map<String, double[]> coordinatesById = new HashMap<>();
        Map<String, String[]> structuresById = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(";", -1);
                if (fields.length == 0) {
                    continue;
                }
                String rowType = fields[0];
                if (ROW_TYPE_STRUCTURE.equals(rowType)) {
                    if (fields.length <= IDX_STRUCT_CATEGORY) {
                        continue;
                    }
                    String id = fields[IDX_STRUCT_ID].trim();
                    String department = normalizeDepartment(fields[IDX_STRUCT_DEPARTMENT]);
                    String category = fields[IDX_STRUCT_CATEGORY].trim();
                    if (!DEPARTMENTS_FILTER.contains(department) || !CATEGORIES_FILTER.contains(category)) {
                        continue;
                    }
                    structuresById.put(id, fields);
                } else if (ROW_TYPE_GEOLOCATION.equals(rowType)) {
                    if (fields.length <= IDX_GEO_Y) {
                        continue;
                    }
                    String id = fields[IDX_GEO_ID].trim();
                    try {
                        double x = Double.parseDouble(fields[IDX_GEO_X].trim().replace(',', '.'));
                        double y = Double.parseDouble(fields[IDX_GEO_Y].trim().replace(',', '.'));
                        coordinatesById.put(id, new double[]{x, y});
                    } catch (NumberFormatException e) {
                        // Coordonnée manquante ou invalide : la structure sera ignorée si non géolocalisée.
                    }
                }
            }
        }

        System.out.println("Structures filtrées (département + catégorie) : " + structuresById.size());

        CoordinateTransform transform = buildLambert93ToWgs84Transform();

        int written = 0;
        int missingCoordinates = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(String.join(";", OUTPUT_COLUMNS));
            writer.write("\n");

            for (Map.Entry<String, String[]> entry : structuresById.entrySet()) {
                String id = entry.getKey();
                double[] coordinates = coordinatesById.get(id);
                if (coordinates == null) {
                    missingCoordinates++;
                    continue;
                }

                String[] fields = entry.getValue();
                String name = fields[IDX_STRUCT_NAME].trim();
                String address = buildAddress(fields);
                String department = normalizeDepartment(fields[IDX_STRUCT_DEPARTMENT]);
                String category = fields[IDX_STRUCT_CATEGORY].trim();

                ProjCoordinate source = new ProjCoordinate(coordinates[0], coordinates[1]);
                ProjCoordinate target = new ProjCoordinate();
                transform.transform(source, target);
                double longitude = target.x;
                double latitude = target.y;

                writer.write(String.join(";",
                        csvField(id),
                        csvField(name),
                        csvField(address),
                        csvField(department),
                        csvField(category),
                        csvField(longitude),
                        csvField(latitude)));
                writer.write("\n");
                written++;
            }
        }

        System.out.println("Lignes écrites : " + written + " -> " + outputPath);
        if (missingCoordinates > 0) {
            System.out.println("Structures ignorées faute de géolocalisation : " + missingCoordinates);
        }
    }

    /** Normalise un code département ("04", "004", "4"...) vers sa forme sans zéros de tête. */
    private static String normalizeDepartment(String rawDepartment) {
        String department = rawDepartment.trim();
        String withoutLeadingZeros = department.replaceFirst("^0+(?=.)", "");
        return withoutLeadingZeros.isEmpty() ? department : withoutLeadingZeros;
    }

    /** Concatène numéro + type de voie + nom de voie + code postal/ville en une adresse propre. */
    private static String buildAddress(String[] fields) {
        String numero = fields[IDX_STRUCT_ADDR_NUMERO].trim();
        String typeVoie = fields[IDX_STRUCT_ADDR_TYPE_VOIE].trim();
        String nomVoie = fields[IDX_STRUCT_ADDR_NOM_VOIE].trim();
        String postalCity = fields[IDX_STRUCT_ADDR_POSTAL_CITY].trim();

        StringBuilder address = new StringBuilder();
        for (String part : new String[]{numero, typeVoie, nomVoie, postalCity}) {
            if (!part.isEmpty()) {
                if (address.length() > 0) {
                    address.append(" ");
                }
                address.append(part);
            }
        }
        return address.toString().replaceAll("\\s+", " ").trim();
    }

    /** Construit la transformation Proj4J Lambert-93 (EPSG:2154) -> WGS84 (EPSG:4326). */
    private static CoordinateTransform buildLambert93ToWgs84Transform() {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem lambert93 = crsFactory.createFromName("EPSG:2154");
        CoordinateReferenceSystem wgs84 = crsFactory.createFromName("EPSG:4326");
        return new CoordinateTransformFactory().createTransform(lambert93, wgs84);
    }

    private static String csvField(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
