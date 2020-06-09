package entity;

import java.util.*;

public class Kolejka {
    private String idKolejki;
    private String idKasy;
    private int sredniCzasObslugi;
    private Queue<Klient> listaKlientow = new LinkedList<>();

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

    public Queue<Klient> getListaKlientow() {
        return listaKlientow;
    }

    public void setListaKlientow(Queue<Klient> listaKlientow) {
        this.listaKlientow = listaKlientow;
    }

    @Override
    public String toString() {
        return "Kolejka{" +
                "idKolejki='" + idKolejki + '\'' +
                ", idKasy='" + idKasy + '\'' +
                ", sredniCzasObslugi=" + sredniCzasObslugi +
                '}';
    }
}
