package com.rbac.audit.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rbac.audit.domain.AuditChange.TargetType;
import com.rbac.audit.domain.AuditFailure;
import com.rbac.audit.domain.AuditQuery;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** 无数据库、网络和系统时间读取；相同输入始终得到相同结果。 */
public final class AuditFunctions {
    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    private static final Instant MIN_DATABASE_TIME = Instant.parse("1000-01-01T00:00:00Z");
    private static final Instant MAX_DATABASE_TIME = Instant.parse("+10000-01-01T00:00:00Z");
    private static final Set<String> ID_FIELDS = Set.of("id", "sysId", "userId", "parentId", "version", "policyVersion");
    private static final Set<String> BOOL_FIELDS = Set.of("platformAdmin", "builtin", "credentialChanged");
    private static final Map<TargetType, Set<String>> FIELDS = Map.of(
        TargetType.USER, Set.of("id", "username", "displayName", "status", "platformAdmin", "version", "credentialChanged"),
        TargetType.SYSTEM, Set.of("id", "code", "name", "description", "status", "version", "policyVersion"),
        TargetType.MEMBER, Set.of("userId", "sysId", "displayName", "status", "roleIds", "version"),
        TargetType.ROLE, Set.of("id", "sysId", "code", "name", "description", "builtin", "status", "permissionIds", "version"),
        TargetType.PERMISSION, Set.of("id", "sysId", "code", "name", "type", "parentId", "routePath", "sortOrder", "status", "version")
    );

    private AuditFunctions() { }

    public static long positiveId(String value, String field) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) throw AuditFailure.invalid(field + "必须是正BIGINT字符串");
        try { return Long.parseLong(value); }
        catch (NumberFormatException ex) { throw AuditFailure.invalid(field + "超出BIGINT范围"); }
    }

    public static AuditQuery.Range normalizeAuditRange(Instant from, Instant to, Instant now) {
        if ((from == null) != (to == null)) throw AuditFailure.invalid("from和to必须成对提供");
        if (from == null) {
            LocalDate today = now.atZone(SHANGHAI).toLocalDate();
            return new AuditQuery.Range(today.minusDays(6).atStartOfDay(SHANGHAI).toInstant(), today.plusDays(1).atStartOfDay(SHANGHAI).toInstant());
        }
        if (!from.isBefore(to)) throw AuditFailure.invalid("from必须早于to");
        if (from.isBefore(MIN_DATABASE_TIME) || !to.isBefore(MAX_DATABASE_TIME))
            throw AuditFailure.invalid("时间超出数据库可表示范围");
        LocalDate first = from.atZone(SHANGHAI).toLocalDate();
        LocalDate last = to.minusNanos(1).atZone(SHANGHAI).toLocalDate();
        if (first.getYear() < 1000 || last.getYear() > 9999 || ChronoUnit.DAYS.between(first, last) + 1 > 90)
            throw AuditFailure.invalid("单次查询最多覆盖90个上海时区自然日");
        return new AuditQuery.Range(from, to);
    }

    public static AuditQuery.Criteria normalizeQuery(com.rbac.audit.domain.AuditScope scope, AuditQuery.Input input, Instant now) {
        if (input.page() < 1 || input.pageSize() < 1 || input.pageSize() > 100) throw AuditFailure.invalid("page至少为1，pageSize为1至100");
        if (input.actorId() != null && input.actorId() <= 0) throw AuditFailure.invalid("actorId无效");
        String action = input.action();
        if ("".equals(action)) action = null;
        if (action != null && (action.length() > 64 || action.isBlank() || !cleanText(action, 64).equals(action)))
            throw AuditFailure.invalid("action必须是1至64个可见字符");
        return new AuditQuery.Criteria(scope, normalizeAuditRange(input.from(), input.to(), now), input.actorId(), action, input.page(), input.pageSize());
    }

    public static JsonNode redactSnapshot(TargetType type, JsonNode input) {
        if (input == null || input.isNull()) return null;
        if (!input.isObject()) throw AuditFailure.invalid("审计快照必须是对象或null");
        ObjectNode result = JSON.objectNode();
        input.fieldNames().forEachRemaining(key -> {
            if (!FIELDS.get(type).contains(key)) return;
            JsonNode value = input.get(key);
            if (value.isNull()) { result.putNull(key); return; }
            if (key.equals("roleIds") || key.equals("permissionIds")) {
                if (!value.isArray() || value.size() > 1000) throw AuditFailure.invalid("审计ID集合无效");
                TreeSet<Long> ids = new TreeSet<>();
                value.forEach(node -> { if (!node.isTextual() && !node.isIntegralNumber()) throw AuditFailure.invalid("审计ID必须为整数或字符串"); ids.add(positiveId(node.asText(), key)); });
                var array = result.putArray(key); ids.forEach(id -> array.add(Long.toString(id))); return;
            }
            if (ID_FIELDS.contains(key)) {
                if (!value.isTextual() && !value.isIntegralNumber()) throw AuditFailure.invalid("审计ID或版本类型错误");
                result.put(key, Long.toString(positiveId(value.asText(), key))); return;
            }
            if (BOOL_FIELDS.contains(key)) {
                if (!value.isBoolean()) throw AuditFailure.invalid("审计布尔字段类型错误");
                result.put(key, value.booleanValue()); return;
            }
            if (key.equals("sortOrder")) {
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0 || value.intValue() > 9999) throw AuditFailure.invalid("审计排序字段无效");
                result.put(key, value.intValue()); return;
            }
            if (!value.isTextual()) throw AuditFailure.invalid("审计文本字段不接受嵌套对象");
            int max = key.equals("description") ? 500 : key.equals("routePath") ? 200 : key.equals("code") ? 80 : 64;
            result.put(key, cleanText(value.textValue(), max));
        });
        if (result.toString().getBytes(StandardCharsets.UTF_8).length > 65536) throw AuditFailure.invalid("审计快照过大");
        return result;
    }

    public static String cleanText(String value, int maxCodePoints) {
        if (value == null) return "";
        return value.codePoints().filter(c -> !Character.isISOControl(c)).limit(maxCodePoints)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    public static String maskUsername(String username) {
        int[] points = cleanText(username, 32).codePoints().toArray();
        if (points.length == 0) return "***";
        return new String(points, 0, 1) + "***" + (points.length > 1 ? new String(points, points.length - 1, 1) : "");
    }

    public static List<AuditQuery.Difference> buildAuditDiff(JsonNode before, JsonNode after) {
        Set<String> fields = new TreeSet<>();
        if (before != null && before.isObject()) before.fieldNames().forEachRemaining(fields::add);
        if (after != null && after.isObject()) after.fieldNames().forEachRemaining(fields::add);
        return fields.stream().filter(key -> !Objects.equals(field(before, key), field(after, key))).map(key -> {
            JsonNode oldValue = field(before, key), newValue = field(after, key);
            AuditQuery.ChangeType change = oldValue == null ? AuditQuery.ChangeType.ADDED : newValue == null ? AuditQuery.ChangeType.REMOVED : AuditQuery.ChangeType.CHANGED;
            return new AuditQuery.Difference(key, change, copy(oldValue), copy(newValue));
        }).collect(Collectors.toUnmodifiableList());
    }

    private static JsonNode field(JsonNode node, String name) { return node == null ? null : node.get(name); }
    private static JsonNode copy(JsonNode node) { return node == null ? null : node.deepCopy(); }
}
