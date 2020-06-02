package entity;

import java.util.List;

public class Interfejs {
    private int maxDlugoscKolejki;
    private int liczbaOtwartychKas;
    private List<Integer> liczbaOsobKolejki;
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

    public List<Integer> getLiczbaOsobKolejki() {
        return liczbaOsobKolejki;
    }

    public void setLiczbaOsobKolejki(List<Integer> liczbaOsobKolejki) {
        this.liczbaOsobKolejki = liczbaOsobKolejki;
    }

    public int getSredniCzasObslugiKolejki() {
        return sredniCzasObslugiKolejki;
    }

    public void setSredniCzasObslugiKolejki(int sredniCzasObslugiKolejki) {
        this.sredniCzasObslugiKolejki = sredniCzasObslugiKolejki;
    }

    public static Interfejs getINSTANCE() {
        return INSTANCE;
    }

    public static void setINSTANCE(Interfejs INSTANCE) {
        Interfejs.INSTANCE = INSTANCE;
    }
}
