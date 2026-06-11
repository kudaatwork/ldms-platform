package projectlx.trip.tracking.business.logic.support;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TripNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger dailySequence = new AtomicInteger(0);
    private volatile String currentDate = "";

    public synchronized String generate() {
        String today = LocalDate.now().format(DATE_FORMAT);
        if (!today.equals(currentDate)) {
            currentDate = today;
            dailySequence.set(0);
        }
        int seq = dailySequence.incrementAndGet();
        return String.format("TRP-%s-%04d", today, seq);
    }
}
