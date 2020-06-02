package utils;

public class Constants {
    public static double TIME_STEP = 1.0;
    public static int POCZATKOWA_LICZBA_KLIENTOW = 20;
    public static int[] SKIER_RIDE_COUNT_RANGE = {1, 10};
    public static int[] SKIER_RIDE_TIME_RANGE = {10, 30};
    public static int[] SKI_LIFTS_SPEED = {40, 30, 20}; // simulation time from down to up
    public static int[] SKI_LIFTS_FREE_SPACE_PERIOD = {2, 4, 6};
    public static int[] CONDITIONS_ROUTE_STATUS = {0, 10};
    public static int CONDITION_GENERATION_PERIOD = 20;

    public static boolean LOG_TIME_ADVANCE = true;
    public static boolean LOG_TIME_REQUEST = false;
}
