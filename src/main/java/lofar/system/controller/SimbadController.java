package lofar.system.controller;
 
import java.util.Map;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import lofar.system.service.SimbadService;
 
/**
 * SimbadController
 *
 * GET /api/simbad/coordinates?object=CAS+A
 *
 * Returns RA and Dec in radians for a named celestial object
 * by querying the SIMBAD astronomical database at Strasbourg.
 *
 * Angular forum form calls this when the user types a target name
 * so coordinates are auto-filled instead of manually entered.
 */
@RestController
@RequestMapping("/api/simbad")
@CrossOrigin(origins = "http://localhost:4200")
public class SimbadController {
 
    @Autowired
    private SimbadService simbadService;
 
    /**
     * Looks up RA/Dec for a named object.
     *
     * @param object  object name, e.g. "CAS A", "3C 461", "JUPITER"
     * @return  { "raRad": 6.123, "decRad": 1.026, "raDeg": 350.8, "decDeg": 58.8,
     *            "raHms": "23 23 27.9", "decDms": "+58 48 42", "objectName": "CAS A" }
     */
    @GetMapping("/coordinates")
    public ResponseEntity<?> getCoordinates(@RequestParam String object) {
        if (object == null || object.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Object name must not be empty"));
        }
        try {
            Map<String, Object> result = simbadService.lookupCoordinates(object.trim());
            if (result == null || result.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "Object not found in SIMBAD: " + object));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                .body(Map.of("error", "SIMBAD lookup failed: " + e.getMessage()));
        }
    }
}