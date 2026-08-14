package com.daycare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Script unique : va chercher, sur monenfant.fr, toutes les fiches (crèches, relais
 * parents-enfants, accueils de loisirs) pour la liste d'URLs ci-dessous, dédoublonne
 * par id et exporte en CSV.
 *
 * Usage: java -jar ... [headless]   (headless=true par défaut)
 */
public class Main {

    // label -> URL de recherche (fournies telles quelles)
    private static final String[][] URLS = {
            {"AIX EN PROVENCE CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Aix-en-Provence,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.447381703606&y=43.529801070774&radius=30000"},
            {"AIX EN PROVENCE RELAIS ET PARENT", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Aix-en-Provence,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.447381703606&y=43.529801070774&radius=30000"},
            {"AIX EN PROVENCE LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Aix-en-Provence,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.447381703606&y=43.529801070774&radius=30000"},

            {"SALON DE PROVENCE CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Salon-de-Provence,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.09895601003&y=43.640656495782&radius=30000"},
            {"SALON DE PROVENCE RELAIS ET PARENT", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Salon-de-Provence,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.09895601003&y=43.640656495782&radius=30000"},
            {"SALON DE PROVENCE LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Salon-de-Provence,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.09895601003&y=43.640656495782&radius=30000"},

            {"PERTUIS CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Pertuis,%20Vaucluse,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.503105877061&y=43.694632089555&radius=30000"},
            {"PERTUIS RELAIS ET PARENT", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Pertuis,%20Vaucluse,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.503105877061&y=43.694632089555&radius=30000"},
            {"PERTUIS LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Pertuis,%20Vaucluse,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.503105877061&y=43.694632089555&radius=30000"},

            {"MANOSQUE CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Manosque,%20Alpes-de-Haute-Provence,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.782337194148&y=43.833734199439&radius=30000"},
            {"MANOSQUE RELAIS ET PARENT", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Manosque,%20Alpes-de-Haute-Provence,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.782337194148&y=43.833734199439&radius=30000"},
            {"MANOSQUE LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Manosque,%20Alpes-de-Haute-Provence,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.782337194148&y=43.833734199439&radius=30000"},

            {"MARSEILLE CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Marseille,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.369840720842&y=43.296537711497&radius=30000"},
            {"MARSEILLE RELAIS ET PARENTS", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Marseille,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.369840720842&y=43.296537711497&radius=30000"},
            {"MARSEILLE LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Marseille,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.369840720842&y=43.296537711497&radius=30000"},

            {"VITROLLES CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Vitrolles,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.249805234188&y=43.447990676167&radius=30000"},
            {"VITROLLES RELAIS ET PARENTS", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Vitrolles,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.249805234188&y=43.447990676167&radius=30000"},
            {"VITROLLES LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Vitrolles,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.249805234188&y=43.447990676167&radius=30000"},

            {"TRETS CRECHE", "https://monenfant.fr/que-recherchez-vous?structure=EAJE&location=Trets,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.683881839008&y=43.447237567895&radius=30000"},
            {"TRETS RELAIS ET PARENTS", "https://monenfant.fr/que-recherchez-vous?structure=RPE;LAEP&location=Trets,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.683881839008&y=43.447237567895&radius=30000"},
            {"TRETS LOISIR", "https://monenfant.fr/que-recherchez-vous?structure=ALSH&location=Trets,%20Bouches-du-Rh%C3%B4ne,%20Provence-Alpes-C%C3%B4te%20d%27Azur,%20FRA&x=5.683881839008&y=43.447237567895&radius=30000"},
    };

    private static final String COOKIE_BUTTON_SELECTOR = "#_it_smc_liferay_privacy_web_portlet_PrivacyPortlet_okButton";
    private static final String LIST_VIEW_BUTTON_SELECTOR = "button[title=\"Afficher la liste\"]";
    private static final String NEXT_PAGE_LINK_SELECTOR = "li.pagination-next.page-item:not(.disabled) a.page-link";
    private static final String NEXT_PAGE_DISABLED_SELECTOR = "li.pagination-next.page-item.disabled";
    // Important: garder le "?" final. Le site expose aussi /modedegarde/search-count?...
    // qui contient "search" en préfixe et se ferait matcher par erreur sans le "?".
    private static final String SEARCH_API_MARKER = "/modedegarde/search?";

    // Un user-agent + viewport + locale "normaux" sont nécessaires : le fingerprint
    // headless par défaut de Playwright est bloqué par la protection anti-bot du site
    // (Radware Bot Manager), qui renvoie alors une page de captcha au lieu du vrai contenu.
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        boolean headless = args.length > 0 ? Boolean.parseBoolean(args[0]) : true;

        Path outputDir = Path.of("output");
        Files.createDirectories(outputDir);

        List<Map<String, Object>> all = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent(DESKTOP_USER_AGENT)
                    .setViewportSize(1366, 900)
                    .setLocale("fr-FR");

            for (String[] entry : URLS) {
                String label = entry[0];
                String url = entry[1];
                System.out.println("Scraping: " + label + " -> " + url);
                try {
                    List<Map<String, Object>> records = scrapeOneUrl(browser, contextOptions, url, label);
                    System.out.println("  -> " + records.size() + " résultat(s)");
                    all.addAll(records);
                } catch (Exception e) {
                    System.err.println("  !! Erreur pour " + label + ": " + e.getMessage());
                }
            }
            browser.close();
        }

        writeJson(outputDir.resolve("raw_all.json"), all);
        System.out.println("Total brut: " + all.size() + " -> output/raw_all.json");

        List<Map<String, Object>> deduped = dedupeById(all);
        writeJson(outputDir.resolve("deduped.json"), deduped);
        System.out.println("Après dédoublonnage (par id): " + deduped.size() + " -> output/deduped.json");

        writeCsv(outputDir.resolve("daycares.csv"), deduped);
        System.out.println("CSV généré -> output/daycares.csv");
    }

    /** Ouvre une URL, accepte les cookies, passe en vue liste, puis parcourt toutes les pages. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> scrapeOneUrl(Browser browser, Browser.NewContextOptions contextOptions,
                                                            String url, String label) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        try (BrowserContext context = browser.newContext(contextOptions)) {
            Page page = context.newPage();
            page.setDefaultTimeout(30000);
            page.navigate(url);

            acceptCookiesIfPresent(page);

            // Cliquer sur "Afficher la liste" déclenche le 1er appel API (page=0).
            Response response = page.waitForResponse(
                    r -> r.url().contains(SEARCH_API_MARKER),
                    () -> page.locator(LIST_VIEW_BUTTON_SELECTOR).click());

            boolean lastPage = false;
            while (!lastPage) {
                Map<String, Object> json = MAPPER.readValue(response.text(), Map.class);
                List<Map<String, Object>> mainResults = (List<Map<String, Object>>) json.get("mainResults");
                if (mainResults != null) {
                    for (Map<String, Object> record : mainResults) {
                        record.put("searchLabel", label);
                        record.put("searchUrl", url);
                        results.add(record);
                    }
                }
                lastPage = Boolean.TRUE.equals(json.get("lastPage"));
                if (lastPage) {
                    break;
                }
                if (page.locator(NEXT_PAGE_DISABLED_SELECTOR).count() > 0) {
                    break;
                }
                Locator nextLink = page.locator(NEXT_PAGE_LINK_SELECTOR);
                if (nextLink.count() == 0) {
                    break;
                }
                response = page.waitForResponse(
                        r -> r.url().contains(SEARCH_API_MARKER),
                        () -> nextLink.first().click());
            }

            page.close();
        }

        return results;
    }

    private static void acceptCookiesIfPresent(Page page) {
        try {
            Locator cookieButton = page.locator(COOKIE_BUTTON_SELECTOR);
            cookieButton.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(8000));
            cookieButton.click();
        } catch (Exception e) {
            // Bandeau déjà fermé ou absent sur cette page : on continue.
        }
    }

    private static List<Map<String, Object>> dedupeById(List<Map<String, Object>> records) {
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            Object id = record.get("id");
            if (id != null) {
                byId.putIfAbsent(id.toString(), record);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private static void writeJson(Path path, List<Map<String, Object>> records) throws Exception {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), records);
    }

    private static final String[] CSV_COLUMNS = {
            "id", "organizationId", "type", "firstname", "name", "siteWeb", "phone", "phone2",
            "mail", "address", "commune", "distance", "hasDispo", "dateDispo", "isAvip",
            "servesCommune", "searchLabel", "searchUrl"
    };

    private static void writeCsv(Path path, List<Map<String, Object>> records) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(String.join(",", CSV_COLUMNS));
            writer.write("\n");
            for (Map<String, Object> record : records) {
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < CSV_COLUMNS.length; i++) {
                    if (i > 0) {
                        row.append(",");
                    }
                    row.append(csvField(record.get(CSV_COLUMNS[i])));
                }
                writer.write(row.toString());
                writer.write("\n");
            }
        }
    }

    private static String csvField(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
