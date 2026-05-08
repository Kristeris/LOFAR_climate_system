package lofar.system.service;
 
import org.springframework.stereotype.Service;
 
/**
 * SubbandFrequencyService
 *
 * Implements the LOFAR subband → frequency conversion formula from:
 * https://science.astron.nl/telescopes/lofar/lofar-system-overview/
 *         technical-specification/frequency-subband-selection-and-rfi/
 *
 * Formula:
 *   f_center = (clock / 2) * (subband / 512)    [MHz]
 *
 * Clock is determined by RSP mode (bus number):
 *   modes 1–4  → bus 1  → clock = 200 MHz
 *   mode  5    → bus 2  → clock = 160 MHz
 *   mode  6    → bus 3  → clock = 160 MHz   (some sources say 200; spec says 160)
 *   mode  7    → bus 3  → clock = 200 MHz
 *
 * The user also chooses clock explicitly (160 or 200) via a dropdown,
 * which overrides the mode-derived value.
 *
 * Subband range: 0–511.
 * A "subband range" string like "40:283" gives min subband = 40, max = 283.
 */
@Service
public class SubbandFrequencyService {
 
    private static final int SUBBAND_COUNT = 512;   // subbands per Nyquist zone
 
    // ---------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------
 
    /**
     * Calculates the minimum and maximum centre frequencies (MHz) for a
     * given subband range string (e.g. "40:283") and clock speed (MHz).
     *
     * @param subbandRange  e.g. "40:283" or a single value "100"
     * @param clockMhz      clock speed in MHz (160 or 200)
     * @return  double[2] = { minFreqMHz, maxFreqMHz }
     * @throws IllegalArgumentException if the input cannot be parsed
     */
    public double[] calculateFrequencyRange(String subbandRange, int clockMhz) {
        int[] minMax = parseSubbandRange(subbandRange);
        double minFreq = subbandToFrequency(minMax[0], clockMhz);
        double maxFreq = subbandToFrequency(minMax[1], clockMhz);
        return new double[]{ round2(minFreq), round2(maxFreq) };
    }
 
    /**
     * Derives the clock speed (MHz) from the RSP mode number,
     * following the bus-mapping rule:
     *   mode 1–4 → bus 1 → 200 MHz
     *   mode 5   → bus 2 → 160 MHz
     *   mode 6/7 → bus 3 → 200 MHz
     *
     * This is used as a default when the user has not explicitly chosen a clock.
     */
    public int clockFromMode(int mode) {
        if (mode >= 1 && mode <= 4) return 200;
        if (mode == 5)              return 160;
        if (mode == 6 || mode == 7) return 200;
        return 200; // safe default
    }
 
    /**
     * Convenience overload: derive clock from mode, then calculate frequency range.
     */
    public double[] calculateFrequencyRange(String subbandRange, String clockStr, int mode) {
        int clockMhz;
        try {
            clockMhz = Integer.parseInt(clockStr.trim());
        } catch (NumberFormatException e) {
            clockMhz = clockFromMode(mode);
        }
        return calculateFrequencyRange(subbandRange, clockMhz);
    }
 
    // ---------------------------------------------------------------
    //  Core maths
    // ---------------------------------------------------------------
 
    /**
     * LOFAR subband centre frequency:
     *   f = (clock / 2) * (subband / 512)   [MHz]
     */
    public double subbandToFrequency(int subband, int clockMhz) {
        return (clockMhz / 2.0) * ((double) subband / SUBBAND_COUNT);
    }
 
    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------
 
    /**
     * Parses a subband range string into [min, max].
     * Accepted formats:  "40:283",  "100",  "40:283,300:400" (uses overall min/max)
     */
    private int[] parseSubbandRange(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Subband range must not be empty");
        }
 
        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
 
        // A subband range can be a comma-separated list of segments
        for (String segment : raw.split(",")) {
            segment = segment.trim();
            if (segment.contains(":")) {
                String[] parts = segment.split(":");
                int lo = parseSubband(parts[0].trim());
                int hi = parseSubband(parts[1].trim());
                if (lo > hi) { int t = lo; lo = hi; hi = t; }
                if (lo < globalMin) globalMin = lo;
                if (hi > globalMax) globalMax = hi;
            } else {
                int v = parseSubband(segment);
                if (v < globalMin) globalMin = v;
                if (v > globalMax) globalMax = v;
            }
        }
 
        if (globalMin == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Could not parse subband range: " + raw);
        }
        return new int[]{ globalMin, globalMax };
    }
 
    private int parseSubband(String s) {
        int v = Integer.parseInt(s);
        if (v < 0 || v > 511) {
            throw new IllegalArgumentException("Subband out of range (0–511): " + v);
        }
        return v;
    }
 
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
 