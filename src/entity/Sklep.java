package entity;

public class Sklep {
    private int sumaWszystkichKlientow;
    private int sumaKlientowZakupy;
    private int sumaKlientowKolejka;
    private int maxDlugoscKolejki;

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
}
