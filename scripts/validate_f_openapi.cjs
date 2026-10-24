const fs = require('node:fs');
const path = require('node:path');
let SwaggerParser;
try { SwaggerParser = require('@apidevtools/swagger-parser'); }
catch { SwaggerParser = require(path.join(__dirname, '../.work/api-tools/node_modules/@apidevtools/swagger-parser')); }
const root = path.resolve(__dirname, '..');
const report = { passed: false, tool: '@apidevtools/swagger-parser', scope: 'F API schema and internal references only' };
(async () => {
  try {
    const api = await SwaggerParser.validate(path.join(root, 'api/F-audit-openapi.json'), { resolve: { external: false } });
    Object.assign(report, { passed: true, operations: Object.keys(api.paths).length, version: api.info.version });
  } catch (error) { report.error = error.message; process.exitCode = 1; }
  fs.writeFileSync(path.join(root, 'validation/F-openapi-report.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report));
})();
