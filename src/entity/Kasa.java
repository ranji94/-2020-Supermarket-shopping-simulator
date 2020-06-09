package entity;

public class Kasa {
    private String idKasy;
    private String idKolejki;
    private String idAktualnyKlient;
    private int czasObslugi;
    private boolean uprzywilejowana;

    public Kasa(String idKasy, String idKolejki, String idAktualnyKlient, int czasObslugi, boolean uprzywilejowana) {
        this.idKasy = idKasy;
        this.idKolejki = idKolejki;
        this.idAktualnyKlient = idAktualnyKlient;
        this.czasObslugi = czasObslugi;
        this.uprzywilejowana = uprzywilejowana;
    }

    public int getCzasObslugi() {
        return czasObslugi;
    }

    public void setCzasObslugi(int czasObslugi) {
        this.czasObslugi = czasObslugi;
    }

    public String getIdKasy() {
        return idKasy;
    }

    public void setIdKasy(String idKasy) {
        this.idKasy = idKasy;
    }

    public String getIdKolejki() {
        return idKolejki;
    }

    public void setIdKolejki(String idKolejki) {
        this.idKolejki = idKolejki;
    }

    public String getIdAktualnyKlient() {
        return idAktualnyKlient;
    }

    public void setIdAktualnyKlient(String idAktualnyKlient) {
        this.idAktualnyKlient = idAktualnyKlient;
    }

    public boolean isUprzywilejowana() {
        return uprzywilejowana;
    }

    public void setUprzywilejowana(boolean uprzywilejowana) {
        this.uprzywilejowana = uprzywilejowana;
    }
}
