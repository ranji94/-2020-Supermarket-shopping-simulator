package utils;

public class Constants {
    ///////// PARAMETRY SYMULACJI ///////////////////////////
    public static double TIME_STEP = 1.0;
    public static int WSPOLCZYNNIK_NAPLYWU_KLIENTOW = 5; // IM MNIEJ TYM SZYBCIEJ
    public static int MAX_KLIENTOW_W_SKLEPIE = 200;
    public static int MAX_SREDNI_CZAS_OBSLUGI = 3; // MINIMALNA WARTOSC: 1
    public static int PROCENT_KLIENTOW_KUPUJACYCH_5_PRODUKTOW = 25;
    public static float PRAWDOPODOBIENSTWO_ZAKONCZENIA_ZAKUPOW = 0.2f;
    public static int MAX_DLUGOSC_KOLEJKI = 10;
    public static int MAX_PRODUKTOW_KLIENTA = 40;

    public static boolean LOG_TIME_ADVANCE = true;
    public static boolean LOG_TIME_REQUEST = false;
}
