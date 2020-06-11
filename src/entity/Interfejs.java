package entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interfejs {
    private int maxDlugoscKolejki;
    private int liczbaOtwartychKas;
    private Map<String, Kolejka> wszystkieKolejki = new HashMap<>();
    private Map<String, Kasa> wszystkieKasy = new HashMap<>();
    private int sredniCzasObslugiKolejki;

    private static Interfejs INSTANCE;

    private Interfejs() {}

    public static Interfejs getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Interfejs();
        }

        return INSTANCE;
    }

    public int getMaxDlugoscKolejki() {
        return maxDlugoscKolejki;
    }

    public void setMaxDlugoscKolejki(int maxDlugoscKolejki) {
        this.maxDlugoscKolejki = maxDlugoscKolejki;
    }

    public int getLiczbaOtwartychKas() {
        return liczbaOtwartychKas;
    }

    public void setLiczbaOtwartychKas(int liczbaOtwartychKas) {
        this.liczbaOtwartychKas = liczbaOtwartychKas;
    }

    public Map<String, Kolejka> getWszystkieKolejki() {
        return wszystkieKolejki;
    }

    public Kolejka getNajkrotszaKolejka() {
        int shortestQueueLong = Integer.MAX_VALUE;
        Kolejka najkrotsza = null;

        for(Map.Entry<String, Kolejka> q : wszystkieKolejki.entrySet()) {
            if(q.getValue().getDlugoscKolejki() < shortestQueueLong) {
                shortestQueueLong = q.getValue().getDlugoscKolejki();
                najkrotsza = q.getValue();
            }
        }

        return najkrotsza;
    }

    public Kolejka getNajkrotszaUprzywilejowana() {
        int shortestQueueLong = Integer.MAX_VALUE;
        Kolejka najkrotsza = null;

        for(Map.Entry<String, Kolejka> q : wszystkieKolejki.entrySet()) {
            if(q.getValue().getDlugoscKolejki() < shortestQueueLong) {
                shortestQueueLong = q.getValue().getDlugoscKolejki();

                if(q.getValue().isUprzywilejowana()) {
                    najkrotsza = q.getValue();
                }
            }
        }

        return najkrotsza != null ? najkrotsza : getNajkrotszaKolejka();
    }

    public void setWszystkieKolejki(Map<String, Kolejka> wszystkieKolejki) {
        this.wszystkieKolejki = wszystkieKolejki;
    }

    public int getSredniCzasObslugiKolejki() {
        return sredniCzasObslugiKolejki;
    }

    public void setSredniCzasObslugiKolejki(int sredniCzasObslugiKolejki) {
        this.sredniCzasObslugiKolejki = sredniCzasObslugiKolejki;
    }

    public Map<String, Kasa> getWszystkieKasy() {
        return wszystkieKasy;
    }

    public void setWszystkieKasy(Map<String, Kasa> wszystkieKasy) {
        this.wszystkieKasy = wszystkieKasy;
    }
}
