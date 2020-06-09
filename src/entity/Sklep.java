package entity;

import java.util.HashMap;
import java.util.Map;

public class Sklep {
    private int sumaKlientowZakupy;
    private int sumaKlientowKolejka;
    private int maxDlugoscKolejki;

    // GLOBAL VARIABLES
    private Map<String, Klient> wszyscyKlienciWSklepie = new HashMap<>();
    private Map<String, Klient> klienciWKolejkach = new HashMap<>();
    private Map<String, Klient> klienciNaZakupach = new HashMap<>();

    private static Sklep INSTANCE;

    private Sklep() {}

    public static Sklep getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Sklep();
        }

        return INSTANCE;
    }

    public int getSumaWszystkichKlientow() {
        return wszyscyKlienciWSklepie.size();
    }

    public int getSumaKlientowKolejka() {
        return klienciWKolejkach.size();
    }

    public int getSumaKlientowNaZakupach() { return klienciNaZakupach.size(); }

    public Map<String, Klient> getWszyscyKlienciWSklepie() {
        return wszyscyKlienciWSklepie;
    }

    public void setWszyscyKlienciWSklepie(Map<String, Klient> wszyscyKlienciWSklepie) {
        this.wszyscyKlienciWSklepie = wszyscyKlienciWSklepie;
    }



    public int getMaxDlugoscKolejki() {
        return maxDlugoscKolejki;
    }

    public void setMaxDlugoscKolejki(int maxDlugoscKolejki) {
        this.maxDlugoscKolejki = maxDlugoscKolejki;
    }

    public Map<String, Klient> getKlienciWKolejkach() {
        return klienciWKolejkach;
    }

    public void setKlienciWKolejkach(Map<String, Klient> klienciWKolejkach) {
        this.klienciWKolejkach = klienciWKolejkach;
    }

    public Map<String, Klient> getKlienciNaZakupach() {
        return klienciNaZakupach;
    }

    public void setKlienciNaZakupach(Map<String, Klient> klienciNaZakupach) {
        this.klienciNaZakupach = klienciNaZakupach;
    }

    @Override
    public String toString() {
        return "Sklep{" +
                "sumaKlientowZakupy=" + sumaKlientowZakupy +
                ", sumaKlientowKolejka=" + sumaKlientowKolejka +
                ", maxDlugoscKolejki=" + maxDlugoscKolejki +
                '}';
    }
}
