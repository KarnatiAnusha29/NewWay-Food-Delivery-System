package engine;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.DayOfWeek;

/**
 * PredictionEngine — Enhanced Multi-Variable Linear Regression ETA System
 * NewWay Food Delivery Time Prediction System
 *
 * INTELLIGENT MODEL (9 features):
 *   T = b0
 *       + b1 * distance_km             (distance effect)
 *       + b2 * traffic_factor          (traffic multiplier)
 *       + b3 * weather_penalty         (weather penalty)
 *       + b4 * peak_hour_factor        (meal-rush periods)
 *       + b5 * weekend_factor          (Fri/Sat/Sun surge)
 *       + b6 * order_volume            (concurrent orders)
 *       + b7 * preparation_time        (restaurant prep estimate)
 *       + b8 * festival_factor         (Indian festivals/events)
 *
 * WEATHER CODES:
 *   0 = Clear sky        => 0 min penalty
 *   1 = Light rain       => +5 min
 *   2 = Heavy storm      => +12 min
 *   3 = Fog/mist         => +8 min
 *   4 = Extreme heat     => +4 min
 *
 * PEAK WINDOWS (Indian delivery patterns):
 *   Morning Rush  08:00-10:30  => +8 min
 *   Lunch Rush    12:00-14:30  => +12 min
 *   Evening Peak  19:00-22:00  => +18 min
 *   Late Night    23:00-01:00  => +6 min
 *
 * ONLINE LEARNING:
 *   SGD gradient descent updates coefficients when actual delivery
 *   time is recorded, gradually improving accuracy over time.
 */
public class PredictionEngine {

    // Model Coefficients (tuned for Indian urban delivery)
    private double beta0 = 7.0;    // base intercept (min)
    private double beta1 = 4.2;    // distance per km
    private double beta2 = 5.5;    // traffic factor (1.0-3.0)
    private double beta3 = 1.0;    // weather penalty multiplier
    private double beta4 = 18.0;   // peak hour impact
    private double beta5 = 9.0;    // weekend/holiday surge
    private double beta6 = 0.04;   // order volume (concurrent)
    private double beta7 = 1.0;    // preparation time factor
    private double beta8 = 12.0;   // festival/event factor

    // Peak Windows
    private static final LocalTime MORNING_RUSH_START = LocalTime.of(8,  0);
    private static final LocalTime MORNING_RUSH_END   = LocalTime.of(10, 30);
    private static final LocalTime LUNCH_RUSH_START   = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_RUSH_END     = LocalTime.of(14, 30);
    private static final LocalTime EVENING_PEAK_START = LocalTime.of(19, 0);
    private static final LocalTime EVENING_PEAK_END   = LocalTime.of(22, 0);

    private static PredictionEngine instance;
    private PredictionEngine() {}
    public static PredictionEngine getInstance() {
        if (instance == null) instance = new PredictionEngine();
        return instance;
    }

    // =========================================================================
    //  FEATURE FUNCTIONS
    // =========================================================================

    /** Peak-hour intensity factor (0.0-1.0) */
    public double getPeakHourFactor() { return getPeakHourFactor(LocalTime.now()); }
    public double getPeakHourFactor(LocalTime at) {
        if (!at.isBefore(EVENING_PEAK_START) && at.isBefore(EVENING_PEAK_END)) return 1.00;  // +18 min
        if (!at.isBefore(LUNCH_RUSH_START)   && at.isBefore(LUNCH_RUSH_END))   return 0.67;  // +12 min
        if (!at.isBefore(MORNING_RUSH_START) && at.isBefore(MORNING_RUSH_END)) return 0.44;  // +8 min
        // Late night - fewer riders, longer wait
        int h = at.getHour();
        if (h == 23 || h == 0) return 0.33;
        return 0.0;
    }

    /** Weekend surge factor (0.0-1.0) */
    public double getWeekendFactor() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dow = now.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY)                         return 1.0;
        if (dow == DayOfWeek.SATURDAY)                       return 0.9;
        if (dow == DayOfWeek.FRIDAY && now.getHour() >= 18)  return 0.5;
        return 0.0;
    }

    /**
     * Festival factor for Indian calendar events.
     * Major festival seasons cause heavy ordering -> longer ETA.
     */
    public double getFestivalFactor() {
        LocalDateTime now = LocalDateTime.now();
        int month = now.getMonthValue();
        int day   = now.getDayOfMonth();
        boolean isFestival = switch (month) {
            case 10, 11 -> true;                              // Diwali season
            case 8      -> day >= 25 && day <= 31;            // Onam / Raksha Bandhan
            case 3      -> day >= 1  && day <= 10;            // Holi
            case 1      -> day >= 13 && day <= 16;            // Pongal / Sankranti
            default     -> false;
        };
        return isFestival ? 1.0 : 0.0;
    }

    /** Estimate concurrent order volume by time of day */
    public double estimateCurrentVolume() {
        int h = LocalTime.now().getHour();
        if (h >= 19 && h < 22) return 140;
        if (h >= 12 && h < 15) return 95;
        if (h >= 8  && h < 11) return 60;
        if (h >= 22 || h <  6) return 12;
        return 35;
    }

    /** Restaurant prep time estimate by food category (minutes) */
    public double estimatePrepTime(String category) {
        if (category == null) return 8.0;
        return switch (category.toLowerCase()) {
            case "biryani"   -> 18.0;
            case "grill"     -> 15.0;
            case "curry"     -> 12.0;
            case "meals"     -> 10.0;
            case "breakfast" -> 8.0;
            case "snacks"    -> 6.0;
            case "beverages" -> 3.0;
            case "dessert"   -> 5.0;
            default          -> 8.0;
        };
    }

    /** Weather penalty in minutes: 0=Clear, 1=LightRain+5, 2=Storm+12, 3=Fog+8, 4=Heat+4 */
    public double getWeatherPenalty(int weatherCode) {
        return switch (Math.max(0, Math.min(4, weatherCode))) {
            case 1 -> 5.0;
            case 2 -> 12.0;
            case 3 -> 8.0;
            case 4 -> 4.0;
            default -> 0.0;
        };
    }

    // =========================================================================
    //  CORE PREDICTION
    // =========================================================================

    /** Backward-compatible signature */
    public int predict(double distanceKm, double trafficFactor, int weatherCode) {
        return predict(distanceKm, trafficFactor, weatherCode, null);
    }

    /** Full prediction with food category for prep-time factor */
    public int predict(double distanceKm, double trafficFactor, int weatherCode,
                       String foodCategory) {
        distanceKm    = Math.max(0.3, distanceKm);
        trafficFactor = Math.max(1.0, Math.min(3.0, trafficFactor));
        weatherCode   = Math.max(0,   Math.min(4,   weatherCode));

        double pk   = getPeakHourFactor();
        double we   = getWeekendFactor();
        double vol  = estimateCurrentVolume();
        double prep = estimatePrepTime(foodCategory);
        double fest = getFestivalFactor();

        double T = beta0
                 + (beta1 * distanceKm)
                 + (beta2 * (trafficFactor - 1.0))
                 + (beta3 * getWeatherPenalty(weatherCode))
                 + (beta4 * pk)
                 + (beta5 * we)
                 + (beta6 * vol)
                 + (beta7 * prep)
                 + (beta8 * fest);

        // Realistic stochastic jitter (+/-1 min)
        double jitter = (Math.random() - 0.5) * 2.0;
        int result = (int) Math.max(5, Math.round(T + jitter));

        System.out.printf(
            "[PredictionEngine] ETA=%d min | dist=%.1fkm tf=x%.1f wx=%d " +
            "peak=%.2f wknd=%.1f vol=%.0f prep=%.0f fest=%.0f%n",
            result, distanceKm, trafficFactor, weatherCode, pk, we, vol, prep, fest);
        return result;
    }

    public int predict(PredictionInput input) {
        return predict(input.distanceKm(), input.trafficFactor(), input.weatherCode());
    }

    // =========================================================================
    //  SGD ONLINE LEARNING
    // =========================================================================
    public void updateModel(PredictionInput input, int actualTime, double lr) {
        double predicted = predict(input);
        double error     = actualTime - predicted;

        double pk   = getPeakHourFactor();
        double we   = getWeekendFactor();
        double vol  = estimateCurrentVolume();
        double fest = getFestivalFactor();

        beta0 += lr * error;
        beta1 += lr * error * input.distanceKm();
        beta2 += lr * error * (input.trafficFactor() - 1.0);
        beta3 += lr * error * (getWeatherPenalty(input.weatherCode()));
        beta4 += lr * error * pk;
        beta5 += lr * error * we;
        beta6 += lr * error * vol;
        beta8 += lr * error * fest;

        // Constrain to prevent divergence
        beta1 = Math.max(1.0, Math.min(10.0, beta1));
        beta2 = Math.max(0.0, Math.min(15.0, beta2));
        beta3 = Math.max(0.0, Math.min(3.0,  beta3));
        beta4 = Math.max(0.0, Math.min(30.0, beta4));
        beta5 = Math.max(0.0, Math.min(20.0, beta5));
        beta6 = Math.max(0.0, Math.min(0.2,  beta6));

        System.out.printf(
            "[PredictionEngine] SGD error=%.2f | b=[%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.4f,%.3f,%.3f]%n",
            error, beta0, beta1, beta2, beta3, beta4, beta5, beta6, beta7, beta8);
    }

    // =========================================================================
    //  HUMAN-READABLE EXPLANATION (for Tracking panel)
    // =========================================================================
    public String explainPrediction(double distanceKm, double trafficFactor, int weatherCode) {
        double pk   = getPeakHourFactor();
        double we   = getWeekendFactor();
        double fest = getFestivalFactor();
        int    total = predict(distanceKm, trafficFactor, weatherCode);

        String[] wxNames = {"Clear", "Light Rain", "Heavy Storm", "Fog/Mist", "Extreme Heat"};
        int wc = Math.max(0, Math.min(4, weatherCode));

        String peakLbl = pk == 1.0 ? "Evening Peak"  : pk >= 0.6 ? "Lunch Rush"
                       : pk >= 0.4 ? "Morning Rush"  : pk > 0.0  ? "Late Night" : "Off-Peak";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Estimate: %d min\n", total));
        sb.append(String.format("  Base time        : %.0f min\n", beta0));
        sb.append(String.format("  Distance %.1f km  : +%.1f min\n", distanceKm, beta1*distanceKm));
        sb.append(String.format("  Traffic x%.1f    : +%.1f min\n", trafficFactor, beta2*(trafficFactor-1.0)));
        sb.append(String.format("  Weather (%s): +%.1f min\n", wxNames[wc], getWeatherPenalty(wc)));
        sb.append(String.format("  %s (%s): +%.0f min\n",
            peakLbl, pk > 0 ? "PEAK" : "normal", beta4*pk));
        if (we > 0) sb.append(String.format("  Weekend Surge   : +%.0f min\n", beta5*we));
        if (fest > 0) sb.append(String.format("  Festival Season : +%.0f min\n", beta8*fest));
        return sb.toString();
    }

    public String currentPeakStatus() {
        double pk = getPeakHourFactor();
        double fe = getFestivalFactor();
        if (pk == 1.0) return "Evening Peak +18 min";
        if (pk >= 0.6) return "Lunch Rush +12 min";
        if (pk >= 0.4) return "Morning Rush +8 min";
        if (pk >  0.0) return "Late Night +6 min";
        if (getWeekendFactor() >= 1.0) return "Weekend Surge +9 min";
        if (fe >= 1.0) return "Festival Season +12 min";
        return "";
    }

    // =========================================================================
    //  GETTERS / SETTERS
    // =========================================================================
    public void setCoefficients(double b0,double b1,double b2,double b3,double b4){
        this.beta0=b0; this.beta1=b1; this.beta2=b2; this.beta3=b3; this.beta4=b4;
    }
    public double[] getCoefficients(){
        return new double[]{beta0,beta1,beta2,beta3,beta4,beta5,beta6,beta7,beta8};
    }

    // Legacy helpers
    public int getPeakHourFactor(LocalTime at, boolean asInt) {
        return getPeakHourFactor(at) >= 1.0 ? 1 : 0;
    }
    public boolean isCurrentlyPeakHour() { return getPeakHourFactor() >= 1.0; }

    public record PredictionInput(double distanceKm, double trafficFactor, int weatherCode) {}
}
