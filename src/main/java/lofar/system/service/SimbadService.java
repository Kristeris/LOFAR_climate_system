package lofar.system.service;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
 
/**
 * SimbadService
 *
 * Queries the SIMBAD Astronomical Database at the University of Strasbourg
 * (http://simbad.u-strasbg.fr) to resolve a named celestial object into
 * J2000 RA/Dec coordinates.
 *
 * The service uses the SIMBAD TAP/script interface which returns a plain-text
 * VOTable-like response that we parse with a regex.
 *
 * Coordinate systems returned:
 *   - raRad  / decRad  — radians (what LOFAR beamctl --anadir expects)
 *   - raDeg  / decDeg  — decimal degrees (human-readable)
 *   - raHms  / decDms  — sexagesimal strings
 */
@Service
public class SimbadService {
 
    private static final Logger logger = LoggerFactory.getLogger(SimbadService.class);
 
    /**
     * SIMBAD script endpoint.
     * We use the "sim-script" interface with a short ADQL-like script
     * to retrieve just the coordinates for a given identifier.
     */
    private static final String SIMBAD_SCRIPT_URL =
        "https://simbad.u-strasbg.fr/simbad/sim-script";
 
    /**
     * Timeout values for the HTTP connection.
     * SIMBAD can be slow during peak hours.
     */
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS    = 15_000;
 
    // ---------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------
 
    /**
     * Looks up RA/Dec coordinates for the given object name via SIMBAD.
     *
     * @param objectName  any SIMBAD-resolvable identifier, e.g. "CAS A", "3C 461",
     *                    "M31", "NGC 1275", "B0833-45"
     * @return  Map with keys: raRad, decRad, raDeg, decDeg, raHms, decDms, objectName
     * @throws Exception if the HTTP request fails or parsing produces no result
     */
    public Map<String, Object> lookupCoordinates(String objectName) throws Exception {
        logger.info("SIMBAD lookup requested for: '{}'", objectName);
 
        // Build the SIMBAD script query.
        // "output console=off script=off" suppresses the banner.
        // "query id <name>" resolves the identifier and prints coordinates.
        String script = "output console=off script=off\n"
                      + "format object \"%IDLIST(1)|%COO(d;A)|%COO(d;D)|%COO(A)|%COO(D)\"\n"
                      + "query id " + objectName;
 
        String encoded = URLEncoder.encode(script, StandardCharsets.UTF_8);
        String urlStr  = SIMBAD_SCRIPT_URL + "?script=" + encoded;
 
        logger.debug("SIMBAD URL: {}", urlStr);
 
        // Perform the HTTP GET
        String response = httpGet(urlStr);
        logger.debug("SIMBAD raw response:\n{}", response);
 
        return parseScriptResponse(response, objectName);
    }
 
    // ---------------------------------------------------------------
    //  Parsing
    // ---------------------------------------------------------------
 
    /**
     * Parses the SIMBAD script-format response.
     *
     * Expected output line (when object is found):
     *   primaryId|raDeg|decDeg|raHms|decDms
     *
     * Example:
     *   Cas A|350.850000|+58.815000|23 23 24.000|+58 48 54.00
     */
    private Map<String, Object> parseScriptResponse(String raw, String requestedName) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("Empty response from SIMBAD");
        }
 
        // Check for explicit error markers in SIMBAD output
        if (raw.contains("!!") || raw.toLowerCase().contains("not found")) {
            logger.warn("SIMBAD returned no-match for '{}'", requestedName);
            return Map.of(); // empty → 404 in controller
        }
 
        // Walk through lines looking for the data line (contains '|')
        for (String line : raw.split("\\r?\\n")) {
            line = line.trim();
            if (line.startsWith("::") || line.startsWith("#") || line.isEmpty()) continue;
 
            String[] parts = line.split("\\|");
            if (parts.length < 5) continue;
 
            try {
                String primaryId = parts[0].trim();
                double raDeg     = Double.parseDouble(parts[1].trim());
                double decDeg    = Double.parseDouble(parts[2].trim());
                String raHms     = parts[3].trim();
                String decDms    = parts[4].trim();
 
                // Convert degrees → radians
                double raRad  = Math.toRadians(raDeg);
                double decRad = Math.toRadians(decDeg);
 
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("objectName", primaryId);
                result.put("requestedName", requestedName);
                result.put("raRad",  round6(raRad));
                result.put("decRad", round6(decRad));
                result.put("raDeg",  round6(raDeg));
                result.put("decDeg", round6(decDeg));
                result.put("raHms",  raHms);
                result.put("decDms", decDms);
 
                logger.info("SIMBAD resolved '{}' → RA={} deg  Dec={} deg  (raRad={}, decRad={})",
                    primaryId, raDeg, decDeg, raRad, decRad);
 
                return result;
 
            } catch (NumberFormatException e) {
                logger.debug("Skipping unparseable SIMBAD line: {}", line);
            }
        }
 
        logger.warn("Could not find coordinate data in SIMBAD response for '{}'", requestedName);
        return Map.of(); // empty → 404
    }
 
    // ---------------------------------------------------------------
    //  HTTP helper
    // ---------------------------------------------------------------
 
    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        // SIMBAD wants a recognisable User-Agent
        conn.setRequestProperty("User-Agent",
            "LOFAR-Climate-System/1.0 (contact: admin@lofar-system.local)");
        conn.setRequestProperty("Accept", "text/plain");
 
        int status = conn.getResponseCode();
        logger.debug("SIMBAD HTTP status: {}", status);
 
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
 
    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------
 
    private double round6(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }
}