import { afterEach, describe, expect, it, vi } from 'vitest'

import type { GeneratedFile, ProjectType } from '../types/generatedProject'
import { buildPreviewDocument } from './previewDocument'

function file(overrides: Partial<GeneratedFile> = {}): GeneratedFile {
  return {
    filePath: 'index.html',
    fileType: 'html',
    content: '<main>Ready</main>',
    ...overrides,
  }
}

function build(files: GeneratedFile[], projectType: ProjectType = 'html') {
  return buildPreviewDocument(files, projectType)
}

describe('previewDocument', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('adds sandbox CSP metadata', () => {
    const document = build([file()])

    expect(document).toContain('<!doctype html>')
    expect(document).toContain('Content-Security-Policy')
    expect(document).toContain("default-src 'none'")
    expect(document).toContain("connect-src 'none'")
  })

  it('escapes embedded style and script closing tags', () => {
    const document = build([
      file(),
      file({ filePath: 'style.css', fileType: 'css', content: '</style><img />' }),
      file({ filePath: 'script.js', fileType: 'js', content: '</script><img />' }),
    ])

    expect(document).toContain('<\\/style><img />')
    expect(document).toContain('<\\/script><img />')
    expect(document).not.toContain('</style><img />')
    expect(document).not.toContain('</script><img />')
  })

  it('removes external css urls before preview injection', () => {
    const document = build([
      file(),
      file({
        filePath: 'style.css',
        fileType: 'css',
        content: 'main { background-image: url("https://example.com/bg.png"); color: red; }',
      }),
    ])

    expect(document).not.toContain('https://example.com/bg.png')
    expect(document).toContain('color: red')
  })

  it('removes dangerous html when document is available', () => {
    vi.stubGlobal('document', {
      createElement() {
        return new FakeTemplateElement()
      },
    })

    const document = build([
      file({
        content:
          '<main><script>alert(1)</script><button onclick="alert(1)">Run</button><img src="https://example.com/logo.png"><a href="/local">Local</a></main>',
      }),
    ])

    expect(document).not.toContain('alert(1)')
    expect(document).not.toContain('onclick')
    expect(document).not.toContain('https://example.com/logo.png')
    expect(document).toContain('<button>Run</button>')
    expect(document).toContain('<a href="/local">Local</a>')
  })

  it('drops dangerous html project scripts and keeps safe scripts', () => {
    expect(build([
      file(),
      file({ filePath: 'script.js', fileType: 'js', content: "fetch('/api/data')" }),
    ])).not.toContain("fetch('/api/data')")

    expect(build([
      file(),
      file({ filePath: 'script.js', fileType: 'js', content: "console.log('ready')" }),
    ])).toContain("console.log('ready')")
  })

  it('extracts Vue and React preview markup without injecting scripts', () => {
    const vueDocument = build([
      file({
        filePath: 'src/App.vue',
        fileType: 'vue',
        content: '<template><main class="app">Vue Ready</main></template>',
      }),
      file({ filePath: 'src/style.css', fileType: 'css', content: '.app { color: red; }' }),
    ], 'vue')
    const reactDocument = build([
      file({
        filePath: 'src/App.tsx',
        fileType: 'tsx',
        content: 'export function App() {\n  return (\n    <main className="app">React Ready</main>\n  )\n}',
      }),
      file({ filePath: 'src/style.css', fileType: 'css', content: '.app { color: blue; }' }),
    ], 'react')

    expect(vueDocument).toContain('<main class="app">Vue Ready</main>')
    expect(vueDocument).toContain('.app { color: red; }')
    expect(reactDocument).toContain('<main class="app">React Ready</main>')
    expect(reactDocument).toContain('.app { color: blue; }')
  })
})

class FakeTemplateElement {
  content = new FakeTemplateContent()

  set innerHTML(markup: string) {
    this.content.innerHTML = markup
  }

  get innerHTML() {
    return this.content.innerHTML
  }
}

class FakeTemplateContent {
  innerHTML = ''

  querySelectorAll(selector: string) {
    if (selector === 'script') {
      return this.scriptElements()
    }
    if (selector === '*') {
      return this.allElements()
    }
    return []
  }

  private scriptElements() {
    const scripts = [...this.innerHTML.matchAll(/<script\b[\s\S]*?<\/script>/gi)]
      .map((match) => new FakeElement(match[0], this))
    return scripts
  }

  private allElements() {
    const elements: FakeElement[] = []
    const tagPattern = /<([a-z][\w-]*)([^>]*)>/gi
    for (const match of this.innerHTML.matchAll(tagPattern)) {
      if (match[1].toLowerCase() === 'script') {
        continue
      }
      elements.push(new FakeElement(match[0], this, parseAttributes(match[2] ?? '')))
    }
    return elements
  }
}

class FakeElement {
  private readonly source: string
  private readonly owner: FakeTemplateContent
  readonly attributes: FakeAttribute[]

  constructor(source: string, owner: FakeTemplateContent, attributes: FakeAttribute[] = []) {
    this.source = source
    this.owner = owner
    this.attributes = attributes
  }

  remove() {
    this.owner.innerHTML = this.owner.innerHTML.replace(this.source, '')
  }

  removeAttribute(name: string) {
    const attribute = this.attributes.find((candidate) => candidate.name === name)
    if (!attribute) {
      return
    }

    const nextSource = removeAttributeFromSource(this.source, attribute)
    this.owner.innerHTML = this.owner.innerHTML.replace(this.source, nextSource)
    this.attributes.splice(this.attributes.indexOf(attribute), 1)
  }
}

type FakeAttribute = {
  name: string
  raw: string
  value: string
}

function parseAttributes(rawAttributes: string) {
  const attributes: FakeAttribute[] = []
  const attributePattern = /\s+([^\s=/>]+)(?:\s*=\s*("[^"]*"|'[^']*'|[^\s>]+))?/g
  for (const match of rawAttributes.matchAll(attributePattern)) {
    const rawValue = match[2] ?? ''
    attributes.push({
      name: match[1],
      raw: match[0],
      value: rawValue.replace(/^['"]|['"]$/g, ''),
    })
  }
  return attributes
}

function removeAttributeFromSource(source: string, attribute: FakeAttribute) {
  const nextSource = source.replace(attribute.raw, '')
  if (nextSource.includes(' ')) {
    return nextSource
  }
  return nextSource.replace(`<${attribute.name}`, `<${attribute.name} `)
}
