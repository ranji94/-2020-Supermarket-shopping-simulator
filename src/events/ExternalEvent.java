package events;

import java.util.Comparator;

public class ExternalEvent<T> {

    public enum EventType {
        CLIENT_ENTERED_SHOP,
        CLOSE_CASH_REGISTER,
        OPEN_CASH_REGISTER,
        SHOPPING_FINISHED,
        CLIENT_TO_QUEUE,
        OPEN_CASH_QUEUE,
        CLOSE_CASH_QUEUE,
        CLIENT_TO_CASH_REGISTER,
        STOP_SIMULATION,
        SHOP_STATS,
        CASH_QUEUE_STATS,
        CASH_REGISTER_STATS
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