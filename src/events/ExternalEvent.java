package events;

import java.util.Comparator;

public class ExternalEvent<T> {

    public enum EventType {
        KLIENT_WCHODZI,
        WYZNACZ_KOLEJKE,
        KLIENT_OBSLUZONY,
        ZADANIE_POLICZENIE,
        PRZEKIEROWANIE_KLIENTA,
        ZWROCENIE_LICZBY_PRODUKTOW,
        PRZEKAZ_DANE_KOLEJKI,
        ZAMKNIJ_KASE,
        OTWORZ_KASE,
        KONIEC_ZAKUPOW,
        KLIENT_DO_KOLEJKI,
        OTWORZ_KOLEJKE,
        ZAMKNIJ_KOLEJKE,
        NASTEPNY_KLIENT,
        STOP_SIMULATION
    }

    private T data;
    private EventType eventType;
    private Double time;

    public ExternalEvent(T data, EventType eventType, Double time) {
        this.data = data;
        this.eventType = eventType;
        this.time = time;
    }

    public EventType getEventType() {
        return eventType;
    }

    public T getData() {
        return data;
    }

    public double getTime() {
        return time;
    }

    public static class ExternalEventComparator implements Comparator<ExternalEvent> {

        @Override
        public int compare(ExternalEvent o1, ExternalEvent o2) {
            return o1.time.compareTo(o2.time);
        }
    }

}