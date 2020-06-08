package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Sklep {
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
        return wszyscyKlienciWSklepie.size();
    }

    public int getSumaKlientowZakupy() {
        return getSumaWszystkichKlientow() - getSumaKlientowKolejka();
    }

    public int getSumaKlientowKolejka() {
        int total = 0;

        for(Map.Entry<String, Kolejka> q : wszystkieKolejkiWSklepie.entrySet()) {
            total += q.getValue().getDlugoscKolejki();
        }

        return total;
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

    @Override
    public String toString() {
        return "Sklep{" +
                "sumaKlientowZakupy=" + sumaKlientowZakupy +
                ", sumaKlientowKolejka=" + sumaKlientowKolejka +
                ", maxDlugoscKolejki=" + maxDlugoscKolejki +
                ", wszystkieKolejkiWSklepie=" + wszystkieKolejkiWSklepie +
                '}';
    }
}
