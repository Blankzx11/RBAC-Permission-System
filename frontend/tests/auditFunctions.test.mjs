import test from 'node:test'
import assert from 'node:assert/strict'
import { assertId, dateRangeToInstants, buildAuditDiff, stableJson } from '../src/features/audit/auditFunctions.ts'

test('BIGINT IDs preserve precision and reject overflow or injection', () => {
  assert.equal(assertId('9223372036854775807'), '9223372036854775807')
  for (const id of ['0', '-1', '01', '9223372036854775808', '1/2', '1 OR 1=1']) assert.throws(() => assertId(id))
})
test('Shanghai dates become UTC half-open intervals', () => {
  assert.deepEqual(dateRangeToInstants('2026-10-24', '2026-10-24'), {from:'2026-10-23T16:00:00.000Z',to:'2026-10-24T16:00:00.000Z'})
  assert.deepEqual(dateRangeToInstants('', ''), {})
})
test('paired, valid, ordered dates and 90-day bounds are enforced', () => {
  assert.throws(() => dateRangeToInstants('2026-10-24', ''))
  assert.throws(() => dateRangeToInstants('2026-02-30', '2026-03-01'))
  assert.throws(() => dateRangeToInstants('2026-10-25', '2026-10-24'))
  assert.doesNotThrow(() => dateRangeToInstants('2026-01-01', '2026-03-31'))
  assert.throws(() => dateRangeToInstants('2026-01-01', '2026-04-01'))
})
test('diff is deterministic, treats explicit null separately, and leaves input unchanged', () => {
  const before = Object.freeze({a:1, gone:2, explicit:null}), after = Object.freeze({a:2, added:3})
  const rows = buildAuditDiff(before, after)
  assert.deepEqual(rows.map(row => [row.field,row.change]), [['a','修改'],['added','新增'],['explicit','移除'],['gone','移除']])
  assert.deepEqual(buildAuditDiff(null, null), [])
  assert.equal(before.a, 1)
})
test('object property order does not produce a false difference', () => {
  assert.equal(stableJson({z:1,a:{b:2,a:3}}), stableJson({a:{a:3,b:2},z:1}))
  assert.deepEqual(buildAuditDiff({x:{a:1,b:2}}, {x:{b:2,a:1}}), [])
})
