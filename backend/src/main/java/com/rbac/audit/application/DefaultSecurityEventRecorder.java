package com.rbac.audit.application;

import com.rbac.audit.domain.SecurityEvent;
import com.rbac.audit.port.SecurityEventRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultSecurityEventRecorder implements SecurityEventRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSecurityEventRecorder.class);
    private final SecurityEventStore store;

    public DefaultSecurityEventRecorder(SecurityEventStore store) { this.store = store; }

    @Override
    public boolean recordSecurityEvent(SecurityEvent event) {
        try {
            // 单独的Bean保证REQUIRES_NEW经过事务代理，避免同类自调用失效。
            store.appendInNewTransaction(event);
            return true;
        } catch (RuntimeException failure) {
            // 不输出事件原文、SQL参数、凭据或异常消息；错误日志供运维告警。
            LOG.error("Security event persistence failed; event remains denied");
            return false;
        }
    }
}
