package entity;

import java.util.List;

public class Kolejka {
    private int idKolejki;
    private int idKasy;
    private int dlugoscKolejki;
    private int sredniCzasObslugi;
    private List<Klient> listaKlientow;

    public Kolejka() {}

    public int getIdKolejki() {
        return idKolejki;
    }

    public void setIdKolejki(int idKolejki) {
        this.idKolejki = idKolejki;
    }

    public int getIdKasy() {
        return idKasy;
    }

    public void setIdKasy(int idKasy) {
        this.idKasy = idKasy;
    }

    public int getDlugoscKolejki() {
        return dlugoscKolejki;
    }

    public void setDlugoscKolejki(int dlugoscKolejki) {
        this.dlugoscKolejki = dlugoscKolejki;
    }

    public int getSredniCzasObslugi() {
        return sredniCzasObslugi;
    }

    public void setSredniCzasObslugi(int sredniCzasObslugi) {
        this.sredniCzasObslugi = sredniCzasObslugi;
    }

    public List<Klient> getListaKlientow() {
        return listaKlientow;
    }

    public void setListaKlientow(List<Klient> listaKlientow) {
        this.listaKlientow = listaKlientow;
    }
}
