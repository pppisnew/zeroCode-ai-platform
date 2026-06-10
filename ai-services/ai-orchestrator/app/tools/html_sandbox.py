import re
from dataclasses import dataclass
from html.parser import HTMLParser
from os import getenv

from playwright.sync_api import Error as PlaywrightError
from playwright.sync_api import sync_playwright

from app.models.generated_project import GeneratedFile

BROWSER_SANDBOX_ENV = "ZEROCODE_ENABLE_BROWSER_SANDBOX"
PREVIEW_CSP = (
    "default-src 'none'; "
    "img-src data: blob:; "
    "style-src 'unsafe-inline'; "
    "script-src 'unsafe-inline'; "
    "connect-src 'none'; "
    "font-src data:; "
    "media-src data: blob:;"
)
NETWORK_SCRIPT_PATTERN = re.compile(
    r"\b(fetch|XMLHttpRequest|WebSocket|EventSource)\s*\(",
    re.IGNORECASE,
)
DANGEROUS_SCRIPT_PATTERN = re.compile(
    r"\beval\s*\(|\bnew\s+Function\s*\(|\bset(?:Timeout|Interval)\s*\(\s*['\"]",
)
EXTERNAL_CSS_URL_PATTERN = re.compile(
    r"(?:@import\s+)?url\(\s*['\"]?(?:https?:)?//",
    re.IGNORECASE,
)
URL_ATTRIBUTES = {"action", "href", "poster", "src"}
STYLE_FILE_TYPES = {"css"}
SCRIPT_FILE_TYPES = {"js", "jsx", "ts", "tsx", "vue"}


@dataclass(frozen=True)
class SandboxCheckResult:
    issues: list[str]
    report: list[str]


class HtmlInspectionParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.element_count = 0
        self.ids: set[str] = set()
        self.external_urls: list[str] = []
        self.inline_event_handlers: list[str] = []
        self.inline_scripts = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.element_count += 1
        normalized_attrs = {name.lower(): value for name, value in attrs}
        if tag.lower() == "script" and "src" not in normalized_attrs:
            self.inline_scripts += 1
        for name, value in attrs:
            normalized_name = name.lower()
            if normalized_name == "id" and value:
                self.ids.add(value)
            if normalized_name.startswith("on") and value is not None:
                self.inline_event_handlers.append(name)
            if normalized_name in URL_ATTRIBUTES and value and is_external_url(value):
                self.external_urls.append(value)


def is_external_url(value: str) -> bool:
    normalized = value.strip().lower()
    return normalized.startswith(("http://", "https://", "//"))


def build_preview_document(files: list[GeneratedFile]) -> str:
    html_file = find_project_file(files, "html", "index.html")
    css_file = find_project_file(files, "css", "style.css")
    js_file = find_project_file(files, "js", "script.js")

    return (
        "<!doctype html>"
        '<html lang="zh-CN">'
        "<head>"
        '<meta charset="UTF-8" />'
        f'<meta http-equiv="Content-Security-Policy" content="{PREVIEW_CSP}" />'
        '<meta name="viewport" content="width=device-width, initial-scale=1.0" />'
        f"<style>{escape_style_content(css_file.content if css_file else '')}</style>"
        "</head>"
        "<body>"
        f"{html_file.content if html_file else ''}"
        f"<script>{escape_script_content(js_file.content if js_file else '')}</script>"
        "</body>"
        "</html>"
    )


def escape_style_content(content: str) -> str:
    return content.replace("</style", "<\\/style")


def escape_script_content(content: str) -> str:
    return content.replace("</script", "<\\/script")


def find_project_file(
    files: list[GeneratedFile],
    file_type: str,
    file_path: str,
) -> GeneratedFile | None:
    return next(
        (
            file
            for file in files
            if file.file_type == file_type or file.file_path == file_path
        ),
        None,
    )


def inspect_html(content: str) -> HtmlInspectionParser:
    parser = HtmlInspectionParser()
    parser.feed(content)
    parser.close()
    return parser


def run_static_sandbox_checks(files: list[GeneratedFile]) -> SandboxCheckResult:
    issues: list[str] = []
    report: list[str] = []

    html_file = find_project_file(files, "html", "index.html")
    css_file = find_project_file(files, "css", "style.css")
    js_file = find_project_file(files, "js", "script.js")

    for file in files:
        if not file.content.strip():
            issues.append(f"{file.file_path} is empty")
        if "```" in file.content:
            issues.append(f"{file.file_path} contains markdown fences")

    if html_file:
        html_parser = inspect_html(html_file.content)
        if html_parser.element_count == 0:
            issues.append("index.html does not contain HTML elements")
        if html_parser.inline_scripts:
            issues.append("index.html must not inline scripts; use script.js")
        for event_name in html_parser.inline_event_handlers:
            issues.append(f"index.html must not use inline event handler: {event_name}")
        for url in html_parser.external_urls:
            issues.append(f"index.html references external URL: {url}")
        report.append(f"HTML elements: {html_parser.element_count}")

    if css_file:
        css_content = css_file.content.strip()
        if "{" not in css_content or "}" not in css_content:
            issues.append("style.css does not contain valid-looking CSS rules")
        if EXTERNAL_CSS_URL_PATTERN.search(css_content):
            issues.append("style.css must not reference external URLs")
        report.append("CSS rules: present")

    if js_file:
        script_content = js_file.content.strip()
        if "<script" in script_content.lower():
            issues.append("script.js must not contain script tags")
        if NETWORK_SCRIPT_PATTERN.search(script_content):
            issues.append("script.js must not perform network requests")
        if DANGEROUS_SCRIPT_PATTERN.search(script_content):
            issues.append("script.js must not use dynamic code execution")
        report.append("JavaScript: present")

    return SandboxCheckResult(issues=issues, report=report)


def run_source_safety_checks(files: list[GeneratedFile]) -> SandboxCheckResult:
    issues: list[str] = []

    for file in files:
        if file.file_type in STYLE_FILE_TYPES or file.file_path.endswith(".css"):
            if EXTERNAL_CSS_URL_PATTERN.search(file.content):
                issues.append(f"{file.file_path} must not reference external URLs")
        if file.file_type in SCRIPT_FILE_TYPES or file.file_path.endswith(
            (".js", ".jsx", ".ts", ".tsx", ".vue"),
        ):
            if NETWORK_SCRIPT_PATTERN.search(file.content):
                issues.append(f"{file.file_path} must not perform network requests")
            if DANGEROUS_SCRIPT_PATTERN.search(file.content):
                issues.append(f"{file.file_path} must not use dynamic code execution")

    return SandboxCheckResult(
        issues=issues,
        report=["Source safety: failed" if issues else "Source safety: safe"],
    )


def run_playwright_sandbox_checks(files: list[GeneratedFile]) -> SandboxCheckResult:
    if getenv(BROWSER_SANDBOX_ENV) != "true":
        return SandboxCheckResult(
            issues=[],
            report=["Browser sandbox: skipped"],
        )

    try:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=True)
            page = browser.new_page(viewport={"width": 1280, "height": 720})
            page.set_content(build_preview_document(files), wait_until="load")
            body_text = page.locator("body").inner_text(timeout=2_000).strip()
            visible_elements = page.locator("body *:visible").count()
            screenshot = page.screenshot(full_page=True)
            browser.close()
    except PlaywrightError as error:
        return SandboxCheckResult(
            issues=[],
            report=[f"Browser sandbox: skipped ({error.__class__.__name__})"],
        )

    if not body_text:
        return SandboxCheckResult(
            issues=["Browser sandbox rendered an empty page"],
            report=["Browser sandbox: failed"],
        )
    if visible_elements == 0:
        return SandboxCheckResult(
            issues=["Browser sandbox rendered no visible elements"],
            report=["Browser sandbox: failed"],
        )
    if not screenshot:
        return SandboxCheckResult(
            issues=["Browser sandbox screenshot is empty"],
            report=["Browser sandbox: failed"],
        )

    return SandboxCheckResult(
        issues=[],
        report=[
            "Browser sandbox: rendered",
            f"Browser sandbox text length: {len(body_text)}",
            f"Browser sandbox visible elements: {visible_elements}",
            f"Browser sandbox screenshot bytes: {len(screenshot)}",
        ],
    )


def run_html_sandbox_checks(files: list[GeneratedFile]) -> SandboxCheckResult:
    static_result = run_static_sandbox_checks(files)
    browser_result = run_playwright_sandbox_checks(files)

    return SandboxCheckResult(
        issues=[*static_result.issues, *browser_result.issues],
        report=[*static_result.report, *browser_result.report],
    )
