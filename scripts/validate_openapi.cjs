// Standards validation of the design file; no HTTP requests to a backend are made.
const fs=require('fs'),path=require('path'),{createHash}=require('crypto');
const root=path.resolve(__dirname,'..');
let SwaggerParser;
try{SwaggerParser=require('@apidevtools/swagger-parser');}
catch{SwaggerParser=require(path.join(root,'.work/api-tools/node_modules/@apidevtools/swagger-parser'));}
(async()=>{
 const report={date:new Date().toISOString(),tool:'@apidevtools/swagger-parser',scope:'OpenAPI schema and internal references only',passed:false,inputSha256:createHash('sha256').update(fs.readFileSync(path.join(root,'api/openapi.json'))).digest('hex')};
 try{
  const api=await SwaggerParser.validate(path.join(root,'api/openapi.json'),{resolve:{external:false}});
  Object.assign(report,{passed:true,openapi:api.openapi,version:api.info.version,operations:Object.values(api.paths).reduce((n,p)=>n+Object.keys(p).filter(k=>['get','post','put','patch','delete','head','options','trace'].includes(k)).length,0)});
 }catch(error){report.error=error.message;process.exitCode=1;}
 fs.writeFileSync(path.join(root,'validation/openapi-report.json'),JSON.stringify(report,null,2)+'\n');
 console.log(JSON.stringify(report));
})();
