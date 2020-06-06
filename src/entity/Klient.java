package entity;

public class Klient {
    private String idKlient;
    private int iloscProduktow;
    private boolean przyKasie;

    public Klient(String idKlient, int iloscProduktow, boolean przyKasie) {
        this.idKlient = idKlient;
        this.iloscProduktow = iloscProduktow;
        this.przyKasie = przyKasie;
    }

    public Klient(String idKlient, int iloscProduktow) {
        this.idKlient = idKlient;
        this.iloscProduktow = iloscProduktow;
        this.przyKasie = false;
    }

    public Klient(String idKlient) {
        this.idKlient = idKlient;
        this.iloscProduktow = 0;
        this.przyKasie = false;
    }

    public String getIdKlient() {
        return idKlient;
    }

    public void setIdKlient(String idKlient) {
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
