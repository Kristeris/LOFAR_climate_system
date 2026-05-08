package lofar.system.controller;
 
import java.util.Map;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lofar.system.service.SubbandFrequencyService;

/**
 * SubbandController
 *
 * GET /api/subband/frequency?subbands=40:283&clock=200&mode=3
 *
 * Returns:
 * {
 *   "minFreqMHz": 3.91,
 *   "maxFreqMHz": 27.54,
 *   "clockMhz":   200,
 *   "subbandRange": "40:283"
 * }
 *
 * the Angular forum form calls this whenever the subbands or clock field changes,
 * and displays the min/max frequencies below the subbands input and stuff.
 */



@RestController
@RequestMapping("/api/subband")
@CrossOrigin(origins = "http://localhost:4200")
public class SubbandController {
 
    @Autowired
    private SubbandFrequencyService subbandService;
 
    /**
     * Calculate the centre-frequency range for the given subband string.
     *
     * @param subbands  subband range string, e.g. "40:283"
     * @param clock     clock speed string "160" or "200" (optional; derived from mode if absent)
     * @param mode      RSP mode number 1–7 (used to derive clock when clock param is missing)
     */
    @GetMapping("/frequency")
    public ResponseEntity<?> getFrequencyRange(
            @RequestParam String subbands,
            @RequestParam(required = false, defaultValue = "200") String clock,
            @RequestParam(required = false, defaultValue = "3") int mode) {
 
        try {
            int clockMhz;
            try {
                clockMhz = Integer.parseInt(clock.trim());
                if (clockMhz != 160 && clockMhz != 200) {
                    clockMhz = subbandService.clockFromMode(mode);
                }
            } catch (NumberFormatException e) {
                clockMhz = subbandService.clockFromMode(mode);
            }
 
            double[] freqRange = subbandService.calculateFrequencyRange(subbands, clockMhz);
 
            return ResponseEntity.ok(Map.of(
                "minFreqMHz",   freqRange[0],
                "maxFreqMHz",   freqRange[1],
                "clockMhz",     clockMhz,
                "subbandRange", subbands
            ));
 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}