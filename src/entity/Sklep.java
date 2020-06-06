package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Sklep {
    private int sumaWszystkichKlientow;
    private int sumaKlientowZakupy;
    private int sumaKlientowKolejka;
    private int maxDlugoscKolejki;

    private Map<String, Klient> wszyscyKlienciWSklepie = new HashMap<>();
    private Map<String, Kolejka> wszystkieKolejkiWSklepie = new HashMap<>();

    private static Sklep INSTANCE;

    private Sklep() {}

    public static Sklep getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Sklep();
        }

        return INSTANCE;
    }

    public int getSumaWszystkichKlientow() {
        return sumaWszystkichKlientow;
    }

    public void setSumaWszystkichKlientow(int sumaWszystkichKlientow) {
        this.sumaWszystkichKlientow = sumaWszystkichKlientow;
    }

    public int getSumaKlientowZakupy() {
        return sumaKlientowZakupy;
    }

    public void setSumaKlientowZakupy(int sumaKlientowZakupy) {
        this.sumaKlientowZakupy = sumaKlientowZakupy;
    }

    public int getSumaKlientowKolejka() {
        return sumaKlientowKolejka;
    }

    public void setSumaKlientowKolejka(int sumaKlientowKolejka) {
        this.sumaKlientowKolejka = sumaKlientowKolejka;
    }

    public int getMaxDlugoscKolejki() {
        return maxDlugoscKolejki;
    }

    public void setMaxDlugoscKolejki(int maxDlugoscKolejki) {
        this.maxDlugoscKolejki = maxDlugoscKolejki;
    }

    public Map<String, Klient> getWszyscyKlienciWSklepie() {
        return wszyscyKlienciWSklepie;
    }

    public void setWszyscyKlienciWSklepie(Map<String, Klient> wszyscyKlienciWSklepie) {
        this.wszyscyKlienciWSklepie = wszyscyKlienciWSklepie;
    }

    public Map<String, Kolejka> getWszystkieKolejkiWSklepie() {
        return wszystkieKolejkiWSklepie;
    }

    public void setWszystkieKolejkiWSklepie(Map<String, Kolejka> wszystkieKolejkiWSklepie) {
        this.wszystkieKolejkiWSklepie = wszystkieKolejkiWSklepie;
    }
}
