"""Collect actual F test evidence and package sources; never uploads or runs services."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

ROOT = Path(__file__).resolve().parents[1]


def read_json(path: str):
    return json.loads((ROOT / path).read_text(encoding="utf-8"))


def write_json(path: str, data):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def extract_api():
    baseline = read_json("api/openapi.json")
    subset = {"openapi": baseline["openapi"], "info": {
        "title": "RBAC0 F 审计模块 API", "version": "1.0.0",
        "description": "从V1.2契约提取的四个只读接口。demo使用服务端验证的Basic账号；生产Bearer认证由A/E宿主提供，非demo独立运行默认拒绝访问。独立安全事件仅为内部端口，无公开写入/修改/删除接口。空action等同未筛选；from/to为UTC半开区间，默认近7个上海自然日，最多90日。"},
        "servers": [{"url": "http://127.0.0.1:8080", "description": "F模块本地演示；仅在demo profile启用Basic"}],
        "security": [{"bearerAuth": []}, {"demoBasic": []}],
        "paths": {key: copy.deepcopy(value) for key, value in baseline["paths"].items() if "/audit-logs" in key},
        "components": {"securitySchemes": copy.deepcopy(baseline["components"]["securitySchemes"]), "schemas": {}},
    }
    subset["components"]["securitySchemes"]["demoBasic"] = {"type": "http", "scheme": "basic", "description": "仅demo；DemoOnly12345可通过AUDIT_DEMO_PASSWORD覆盖"}

    def collect(node):
        if isinstance(node, dict):
            ref = node.get("$ref", "")
            if ref.startswith("#/components/schemas/"):
                name = ref.rsplit("/", 1)[1]
                if name not in subset["components"]["schemas"]:
                    schema = copy.deepcopy(baseline["components"]["schemas"][name])
                    subset["components"]["schemas"][name] = schema
                    collect(schema)
            for value in node.values():
                collect(value)
        elif isinstance(node, list):
            for value in node:
                collect(value)

    for path in subset["paths"].values():
        operation = path["get"]
        operation["tags"] = ["F-审计查询"]
        operation["responses"]["200"]["description"] = "成功，JSON中的ID保持字符串。"
        operation["responses"].pop("429", None)  # 集成网关的限流不属于本模块。
    collect(subset["paths"])
    assert len(subset["paths"]) == 4
    write_json("api/F-audit-openapi.json", subset)


def suites(path: Path):
    root = ET.parse(path).getroot()
    return [root] if root.tag == "testsuite" or root.findall("testcase") else root.findall(".//testsuite")


def summarize_tests(paths):
    cases = []
    for path in paths:
        for suite in suites(path):
            for case in suite.findall("testcase"):
                cases.append({"class": case.get("classname"), "name": case.get("name"), "failed": case.find("failure") is not None or case.find("error") is not None, "skipped": case.find("skipped") is not None})
    assert cases and not any(case["failed"] or case["skipped"] for case in cases), "Tests must pass without skipped cases"
    return {"tests": len(cases), "failed": 0, "skipped": 0, "cases": cases}


def build_report():
    backend = summarize_tests(sorted((ROOT / "backend/target/surefire-reports").glob("TEST-*.xml")))
    frontend = summarize_tests([ROOT / "validation/F-frontend-junit.xml"])
    assert read_json("validation/F-openapi-report.json")["passed"]
    browser = read_json("validation/F-browser-report.json")
    assert not browser["errors"] and browser["checks"] and all(item["passed"] for item in browser["checks"])
    for file, digest in browser["artifactSha256"].items():
        assert sha256(ROOT / file) == digest, f"Browser evidence is stale: {file}"
    coverage = {}
    for counter in ET.parse(ROOT / "backend/target/site/jacoco/jacoco.xml").getroot().findall("counter"):
        covered, missed = int(counter.get("covered")), int(counter.get("missed"))
        coverage[counter.get("type")] = {"covered": covered, "missed": missed, "percent": round(100 * covered / (covered + missed), 2) if covered + missed else 100}
    shutil.copytree(ROOT / "backend/target/site/jacoco", ROOT / "validation/F-audit-jacoco", dirs_exist_ok=True)
    source_paths = [file for folder in ["backend", "frontend"] for file in (ROOT / folder).rglob("*") if file.is_file() and not ({"target", "node_modules", "dist"} & set(file.relative_to(ROOT).parts))]
    report = {"generatedAt": datetime.now(timezone.utc).isoformat(), "scope": "F-only / RBAC0", "passed": True,
        "backend": {"command": "mvn verify", "javaRelease": 17, "springBoot": "3.5.16", "databaseTested": "H2 2.x / MySQL mode / JDBC", **backend},
        "frontend": {"command": "npm test && npm run build", "typecheck": "vue-tsc --noEmit passed", **frontend},
        "browser": {"command": "node scripts/check_f_browser.cjs", "checks": len(browser["checks"]), "passed": len(browser["checks"]), "apiMocked": False, "evidence": "validation/F-browser-report.json"},
        "jacoco": coverage, "artifactSha256": browser["artifactSha256"],
        "sourceSha256": {str(file.relative_to(ROOT)).replace("\\", "/"): sha256(file) for file in sorted(source_paths)},
        "limits": ["仅F四个查询接口与内部审计/安全事件端口", "A—E生产认证和业务需要集成", "未执行MySQL实库验收", "未进行全组系统验收、服务器部署、压力测试或课程网站提交"],
    }
    write_json("validation/F-module-report.json", report)
    summary = ET.Element("testsuite", name="RBAC0-F-backend", tests=str(backend["tests"]), failures="0", errors="0", skipped="0")
    for item in backend["cases"]:
        ET.SubElement(summary, "testcase", classname=item["class"], name=item["name"])
    ET.ElementTree(summary).write(ROOT / "validation/F-backend-junit.xml", encoding="utf-8", xml_declaration=True)
    (ROOT / "validation/F-module-report.md").write_text(
        f"# F 模块实际验证\n\n记录时间：{report['generatedAt']}\n\n"
        f"| 检查 | 结果 |\n| --- | --- |\n| Java/Maven 测试 | {backend['tests']}/{backend['tests']} 通过 |\n"
        f"| Vue/TS 纯函数测试 | {frontend['tests']}/{frontend['tests']} 通过 |\n"
        f"| 浏览器真实接口联调 | {len(browser['checks'])}/{len(browser['checks'])} 通过；0脚本错误 |\n"
        f"| vue-tsc + Vite | 构建通过 |\n| JaCoCo 行覆盖率 | {coverage['LINE']['percent']}% |\n"
        f"| JaCoCo 分支覆盖率 | {coverage['BRANCH']['percent']}% |\n| OpenAPI 子集 | 4个操作，标准校验通过 |\n\n"
        "覆盖范围：F 的成功审计、安全事件、四个只读查询接口、实际 API 页面。数据库测试为 H2/JDBC；没有执行 MySQL 实库、A—E 集成和服务器部署。\n\n"
        "## 检查入口\n\n"
        "1. 打开 `F-audit-jacoco/index.html`，查看实际覆盖率及逐行标记。\n"
        "2. 查看 `F-module-report.json` 的测试名称、源文件和构建产物哈希。\n"
        "3. 查看 `F-browser-report.json` 的真实请求状态和检查记录，以及三张 `F-audit-*.png` 截图。\n"
        "4. 后端执行 `mvn clean verify`，前端执行 `npm test`、`npm run build` 可重新验证。\n", encoding="utf-8")
    print(json.dumps({"backendTests": backend["tests"], "frontendTests": frontend["tests"], "browserChecks": len(browser["checks"]), "lineCoverage": coverage["LINE"]}, ensure_ascii=False))


def package():
    files = set()
    for folder in ["backend", "frontend", "validation/F-audit-jacoco"]:
        for file in (ROOT / folder).rglob("*"):
            if file.is_file() and not ({"target", "node_modules"} & set(file.relative_to(ROOT).parts)):
                files.add(file)
    for pattern in ["docs/detail-design/F-*", "uml/detail-design/F-*", "validation/F-*", "database/F-*", "api/F-*"]:
        files.update(file for file in ROOT.glob(pattern) if file.is_file() and file.name != "F-delivery-manifest.json")
    for relative in ["database/001_rbac0.sql", "docs/detail-design/00-公共函数与依赖约定.adoc", "docs/10-F模块实现与验证.adoc", "F-审计模块使用说明.md", "scripts/check_f_browser.cjs", "scripts/build_f_delivery.py", "scripts/validate_f_openapi.cjs", "scripts/package.json", "scripts/package-lock.json", "api/openapi.json", "backend/target/rbac0-audit-1.0.0.jar"]:
        files.add(ROOT / relative)
    target = ROOT / "RBAC0-F审计模块代码包.zip"
    with ZipFile(target, "w", ZIP_DEFLATED, compresslevel=6) as output:
        for file in sorted(files):
            relative = file.relative_to(ROOT).as_posix()
            if relative == "backend/target/rbac0-audit-1.0.0.jar":
                relative = "build/rbac0-audit-1.0.0.jar"
            output.write(file, "RBAC0-F/" + relative)
        output.writestr("RBAC0-F/README.md", "# RBAC0 F 审计模块\n\n请先阅读 F-审计模块使用说明.md。\n\n源码在 backend/、frontend/，五份设计在 docs/detail-design/ 和 uml/detail-design/。\n\n已编译后端在 build/rbac0-audit-1.0.0.jar，可使用 Java 17+ 执行：\n\n```sh\njava -jar build/rbac0-audit-1.0.0.jar --spring.profiles.active=demo\n```\n\n前端执行 npm ci 与 npm run dev 后访问开发服务器；frontend/dist 是已构建网页，需通过 HTTP 服务及 /api 代理访问，不能双击离线使用。\n\n这只是 F 模块。demo 使用 H2，MySQL 和 A/E 认证需要进一步集成。\n")
    with ZipFile(target) as archive:
        assert archive.testzip() is None
        assert all("node_modules/" not in name and "/.work/" not in name for name in archive.namelist())
    write_json("validation/F-delivery-manifest.json", {"file": target.name, "bytes": target.stat().st_size, "sha256": sha256(target), "files": len(files) + 1})
    print(f"Packaged {len(files) + 1} files: {target.name} ({target.stat().st_size} bytes)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-only", action="store_true")
    options = parser.parse_args()
    extract_api()
    if not options.api_only:
        build_report()
        package()
