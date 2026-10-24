package com.example.rbac.common;

import java.util.concurrent.atomic.AtomicLong;

public final class Ids {
    private static final AtomicLong LAST = new AtomicLong(System.currentTimeMillis() * 1000L);

    private Ids() {
    }

    public static long next() {
        return LAST.incrementAndGet();
    }
}
