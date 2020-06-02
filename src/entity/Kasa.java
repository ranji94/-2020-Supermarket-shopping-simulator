package entity;

public class Kasa {
    private int idKasy;
    private int idKolejki;
    private int idAktualnyKlient;
    private boolean uprzywilejowana;

    public Kasa(int idKasy, int idKolejki, int idAktualnyKlient, boolean uprzywilejowana) {
        this.idKasy = idKasy;
        this.idKolejki = idKolejki;
        this.idAktualnyKlient = idAktualnyKlient;
        this.uprzywilejowana = uprzywilejowana;
    }

    public Kasa(int idKasy, int idKolejki, int idAktualnyKlient) {
        this.idKasy = idKasy;
        this.idKolejki = idKolejki;
        this.idAktualnyKlient = idAktualnyKlient;
        this.uprzywilejowana = false;
    }

    public int getIdKasy() {
        return idKasy;
    }

    public void setIdKasy(int idKasy) {
        this.idKasy = idKasy;
    }

    public int getIdKolejki() {
        return idKolejki;
    }

    public void setIdKolejki(int idKolejki) {
        this.idKolejki = idKolejki;
    }

    public int getIdAktualnyKlient() {
        return idAktualnyKlient;
    }

    public void setIdAktualnyKlient(int idAktualnyKlient) {
        this.idAktualnyKlient = idAktualnyKlient;
    }

    public boolean isUprzywilejowana() {
        return uprzywilejowana;
    }

    public void setUprzywilejowana(boolean uprzywilejowana) {
        this.uprzywilejowana = uprzywilejowana;
    }
}
