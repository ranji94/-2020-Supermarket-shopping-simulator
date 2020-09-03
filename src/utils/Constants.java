package utils;

public class Constants {
    ///////// Simulation parameters ///////////////////////////
    public static double TIME_STEP = 1.0;
    public static int CLIENTS_INFLOW_RATIO = 5;
    public static int MAX_CLIENTS_IN_SHOP = 200;
    public static int MAX_AVERAGE_SERVICE_TIME = 3;
    public static int CLIENTS_PRIVILEGED_PERCENTAGE = 25;
    public static float FINISH_SHOPPING_PROBABILITY = 0.2f;
    public static int MAX_CASH_QUEUE_LENGTH = 10;
    public static int MAX_TOTAL_PRODUCTS_COUNT = 40;
    public static float OPEN_PRIVILEGED_CASH_REGISTER_PROBABILITY = 0.1f;
    public static float PRODUCT_RETURN_PROBABILITY = 0.1f;

    public static boolean LOG_TIME_ADVANCE = true;
    public static boolean LOG_TIME_REQUEST = false;
}
