package utils;


import hla.rti1516e.LogicalTime;
import hla.rti1516e.LogicalTimeInterval;
import org.portico.impl.hla1516e.types.time.DoubleTime;
import org.portico.impl.hla1516e.types.time.DoubleTimeInterval;

public class TimeUtils {

    public static LogicalTime convertTime(double time)
    {
        // PORTICO SPECIFIC!!
        return new DoubleTime( time );
    }

    public static double convertTime(LogicalTime logicalTime) {
        // PORTICO SPECIFIC!!
        return ((DoubleTime) logicalTime).getTime();
    }

    public static LogicalTimeInterval convertInterval(double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTimeInterval( time );
    }
}
