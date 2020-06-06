package entity;

import java.util.ArrayList;
import java.util.List;

public class Kolejka {
    private String idKolejki;
    private String idKasy;
    private int sredniCzasObslugi;
    private List<Klient> listaKlientow = new ArrayList<>();

    public Kolejka() {}

    public Kolejka(String idKolejki) {
        this.idKolejki = idKolejki;
    }

    public String getIdKolejki() {
        return idKolejki;
    }

    public void setIdKolejki(String idKolejki) {
        this.idKolejki = idKolejki;
    }

    public String getIdKasy() {
        return idKasy;
    }

    public void setIdKasy(String idKasy) {
        this.idKasy = idKasy;
    }

    public int getDlugoscKolejki() {
        return listaKlientow.size();
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
