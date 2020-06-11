package events;

import java.util.Comparator;

public class ExternalEvent<T> {

    public enum EventType {
        KLIENT_WCHODZI,
        ZAMKNIJ_KASE,
        OTWORZ_KASE,
        KONIEC_ZAKUPOW,
        KLIENT_DO_KOLEJKI,
        OTWORZ_KOLEJKE,
        ZAMKNIJ_KOLEJKE,
        KLIENT_DO_KASY,
        STOP_SIMULATION,
        STATYSTYKI_SKLEP,
        STATYSTYKI_KOLEJKA,
        STATYSTYKI_KASA
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