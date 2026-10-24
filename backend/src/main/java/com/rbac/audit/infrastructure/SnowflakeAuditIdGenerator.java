package com.rbac.audit.infrastructure;

import com.rbac.audit.port.AuditIdGenerator;
import java.time.Clock;
import java.time.Instant;

public class SnowflakeAuditIdGenerator implements AuditIdGenerator {
    private static final long EPOCH = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    private final Clock clock;
    private final int nodeId;
    private long lastMillis = -1;
    private int sequence;

    public SnowflakeAuditIdGenerator(Clock clock, int nodeId) {
        if (nodeId < 0 || nodeId > 1023) throw new IllegalArgumentException("nodeId must be 0..1023");
        this.clock = clock; this.nodeId = nodeId;
    }

    @Override
    public synchronized long nextId() {
        long now = clock.millis();
        if (now < EPOCH || now - EPOCH >= (1L << 41) || now < lastMillis) throw new IllegalStateException("ID clock is outside its valid range");
        sequence = now == lastMillis ? sequence + 1 : 0;
        if (sequence > 4095) throw new IllegalStateException("Audit ID capacity exceeded for this millisecond");
        lastMillis = now;
        return ((now - EPOCH) << 22) | ((long) nodeId << 12) | sequence;
    }
}
