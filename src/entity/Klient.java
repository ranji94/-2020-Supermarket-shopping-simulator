package entity;

public class Klient {
    private int idKlient;
    private int iloscProduktow;
    private boolean przyKasie;

    public Klient(int idKlient, int iloscProduktow, boolean przyKasie) {
        this.idKlient = idKlient;
        this.iloscProduktow = iloscProduktow;
        this.przyKasie = przyKasie;
    }

    public Klient(int idKlient, int iloscProduktow) {
        this.idKlient = idKlient;
        this.iloscProduktow = iloscProduktow;
        this.przyKasie = false;
    }

    public Klient(int idKlient) {
        this.idKlient = idKlient;
        this.iloscProduktow = 0;
        this.przyKasie = false;
    }

    public int getIdKlient() {
        return idKlient;
    }

    public void setIdKlient(int idKlient) {
        this.idKlient = idKlient;
    }

    public int getIloscProduktow() {
        return iloscProduktow;
    }

    public void setIloscProduktow(int iloscProduktow) {
        this.iloscProduktow = iloscProduktow;
    }

    public boolean isPrzyKasie() {
        return przyKasie;
    }

    public void setPrzyKasie(boolean przyKasie) {
        this.przyKasie = przyKasie;
    }
}
