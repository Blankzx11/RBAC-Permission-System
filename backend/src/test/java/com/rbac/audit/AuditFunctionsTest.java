package com.rbac.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.rbac.audit.domain.*;
import com.rbac.audit.functional.AuditFunctions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditFunctionsTest {
    private final ObjectMapper json = new ObjectMapper();
    private final Instant now = Instant.parse("2026-10-24T04:00:00Z");

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "01", "9223372036854775808", "1 OR 1=1", "", "1.2"})
    void rejectsInvalidIds(String id) { assertThrows(AuditFailure.class, () -> AuditFunctions.positiveId(id, "id")); }

    @Test void preservesBigintPrecision() { assertEquals(Long.MAX_VALUE, AuditFunctions.positiveId("9223372036854775807", "id")); }

    @Test void defaultsToSevenShanghaiCalendarDays() {
        var range = AuditFunctions.normalizeAuditRange(null, null, now);
        assertEquals(Instant.parse("2026-10-17T16:00:00Z"), range.from());
        assertEquals(Instant.parse("2026-10-24T16:00:00Z"), range.to());
    }

    @Test void validatesPairedOrderedAndNinetyDayBounds() {
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeAuditRange(now, null, now));
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeAuditRange(now, now, now));
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeAuditRange(now, now.minusSeconds(1), now));
        Instant start = Instant.parse("2026-01-01T16:00:00Z");
        assertDoesNotThrow(() -> AuditFunctions.normalizeAuditRange(start, start.plus(Duration.ofDays(90)), now));
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeAuditRange(start, start.plus(Duration.ofDays(90)).plusMillis(1), now));
    }

    @Test void whitelistDropsCredentialsAndCanonicalizesIdsWithoutMutatingInput() throws Exception {
        JsonNode input = json.readTree("{\"roleIds\":[\"10\",\"2\",\"2\"],\"version\":3,\"password\":\"secret\",\"token\":\"secret\",\"extra\":{\"secret\":\"hidden\"}}");
        String original = input.toString();
        JsonNode result = AuditFunctions.redactSnapshot(AuditChange.TargetType.MEMBER, input);
        assertEquals("{\"roleIds\":[\"2\",\"10\"],\"version\":\"3\"}", result.toString());
        assertEquals(original, input.toString());
        assertFalse(result.toString().contains("secret"));
    }

    @Test void validatesAllowedFieldTypesAndRemovesControlCharacters() throws Exception {
        assertThrows(AuditFailure.class, () -> AuditFunctions.redactSnapshot(AuditChange.TargetType.USER, json.readTree("{\"displayName\":{\"password\":\"secret\"}}")));
        assertThrows(AuditFailure.class, () -> AuditFunctions.redactSnapshot(AuditChange.TargetType.ROLE, json.readTree("{\"builtin\":\"true\"}")));
        assertThrows(AuditFailure.class, () -> AuditFunctions.redactSnapshot(AuditChange.TargetType.PERMISSION, json.readTree("{\"sortOrder\":-1}")));
        assertThrows(AuditFailure.class, () -> AuditFunctions.redactSnapshot(AuditChange.TargetType.MEMBER, json.readTree("[]")));
        assertThrows(AuditFailure.class, () -> AuditFunctions.redactSnapshot(AuditChange.TargetType.MEMBER, json.readTree("{\"roleIds\":[{}]}")));
        assertEquals("ab", AuditFunctions.cleanText("a\r\nb", 64));
        assertEquals("a***e", AuditFunctions.maskUsername("alice"));
        assertEquals("***", AuditFunctions.maskUsername(""));
        assertEquals("陈***雨", AuditFunctions.maskUsername("陈雨"));
    }

    @Test void reportsAddedRemovedChangedAndExplicitNull() throws Exception {
        var before = json.readTree("{\"a\":1,\"removed\":2,\"explicit\":null}");
        var after = json.readTree("{\"a\":2,\"added\":3}");
        var result = AuditFunctions.buildAuditDiff(before, after);
        assertEquals(4, result.size());
        assertEquals(AuditQuery.ChangeType.CHANGED, result.get(0).change());
        assertEquals(AuditQuery.ChangeType.ADDED, result.get(1).change());
        assertEquals(AuditQuery.ChangeType.REMOVED, result.get(2).change());
        assertTrue(AuditFunctions.buildAuditDiff(null, null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test void queryValidationAndOffsetAvoidIntegerOverflow() {
        var valid = new AuditQuery.Input(null, null, null, null, Integer.MAX_VALUE, 100);
        var result = AuditFunctions.normalizeQuery(new AuditScope.System(1), valid, now);
        assertEquals(214748364600L, result.offset());
        assertNull(AuditFunctions.normalizeQuery(new AuditScope.System(1), new AuditQuery.Input(null,null,null,"",1,20), now).action());
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeQuery(new AuditScope.System(1), new AuditQuery.Input(null,null,null,null,0,20), now));
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeQuery(new AuditScope.System(1), new AuditQuery.Input(null,null,null,null,1,101), now));
        assertThrows(AuditFailure.class, () -> AuditFunctions.normalizeQuery(new AuditScope.System(1), new AuditQuery.Input(null,null,null,"x\n",1,20), now));
    }
}
