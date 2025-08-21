package damnosol.triggeriq.sentiment.quora;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.mockito.Mockito.*;

class QuoraRetrySchedulerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Mock
    private ListOperations<String, String> listOps;

    private QuoraRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Clock fixedClock = Clock.fixed(Instant.parse("2025-08-21T10:00:00Z"), ZoneId.of("UTC"));
        scheduler = new QuoraRetryScheduler(redisTemplate, fixedClock);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
    }

    @Test
    void scheduleRetry_underMaxAttempts_addsToZSet() {
        String url = "https://quora.com/test";

        scheduler.scheduleRetry(url, 1);

        // verify scheduled 2s later
        verify(zSetOps).add(eq("quora:retry"),
                startsWith("https://quora.com/test|"),
                eq(Instant.parse("2025-08-21T10:00:02Z").toEpochMilli() * 1.0));
        verify(listOps, never()).leftPush(eq("quora:dead"), anyString());
    }

    @Test
    void scheduleRetry_exceedsMaxAttempts_goesToDeadLetter() {
        String url = "https://quora.com/fail";

        scheduler.scheduleRetry(url, 6);

        // Should push to dead-letter list
        verify(listOps).leftPush("quora:dead", url);
        // Should never schedule in ZSet
        verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void processRetries_movesDueEntriesBackToPending_andReschedules() {
        String entry = "https://quora.com/test|2";

        when(zSetOps.rangeByScore(eq("quora:retry"), anyDouble(), anyDouble()))
                .thenReturn(Set.of(entry));

        // Spy the scheduler to intercept internal scheduleRetry call
        QuoraRetryScheduler spy = spy(scheduler);
        doNothing().when(spy).scheduleRetry(anyString(), anyInt());

        spy.processRetries();

        // Verify expired entry removed from retry ZSet
        verify(zSetOps).remove("quora:retry", entry);
        // Verify it was pushed back to pending queue
        verify(listOps).leftPush("quora:pending", "https://quora.com/test");
        // Verify internal rescheduling called with next attempt
        verify(spy).scheduleRetry("https://quora.com/test", 3);
    }

    @Test
    void processRetries_nothingDue_doesNothing() {
        when(zSetOps.rangeByScore(eq("quora:retry"), anyDouble(), anyDouble()))
                .thenReturn(Set.of());

        scheduler.processRetries();

        verifyNoInteractions(listOps);
        verify(zSetOps).rangeByScore(eq("quora:retry"), anyDouble(), anyDouble());
    }
}