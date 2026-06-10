import { describe, expect, it } from 'vitest'

import type { GeneratedFile } from '../types/generatedProject'
import { isSafeProjectPath, normalizeProjectPath, validateProjectFiles } from './projectFileSecurity'

function file(overrides: Partial<GeneratedFile> = {}): GeneratedFile {
  return {
    filePath: 'index.html',
    fileType: 'html',
    content: '<main>Ready</main>',
    ...overrides,
  }
}

describe('projectFileSecurity', () => {
  it('normalizes and validates safe project paths', () => {
    expect(normalizeProjectPath('src\\App.vue')).toBe('src/App.vue')
    expect(isSafeProjectPath('src/App.vue')).toBe(true)
    expect(isSafeProjectPath('/src/App.vue')).toBe(false)
    expect(isSafeProjectPath('src/../App.vue')).toBe(false)
    expect(isSafeProjectPath('src//App.vue')).toBe(false)
  })

  it('accepts a valid project', () => {
    expect(validateProjectFiles([
      file(),
      file({ filePath: 'style.css', fileType: 'css', content: 'main { color: red; }' }),
      file({ filePath: 'script.js', fileType: 'js', content: "console.log('ready')" }),
    ], 'zerocode-html-app')).toBeUndefined()
  })

  it('rejects duplicate normalized paths', () => {
    expect(validateProjectFiles([
      file({ filePath: 'src/App.vue', fileType: 'vue' }),
      file({ filePath: 'src\\App.vue', fileType: 'vue' }),
    ])).toBe('文件路径重复：src/App.vue')
  })

  it('rejects project and file size limits', () => {
    expect(validateProjectFiles([file()], 'x'.repeat(129))).toBe('项目名称不能超过 128 个字符')
    expect(validateProjectFiles(Array.from({ length: 101 }, (_, index) => file({
      filePath: `file-${index}.html`,
    })))).toBe('项目文件数量不能超过 100 个')
    expect(validateProjectFiles([file({ filePath: 'x'.repeat(501) })]))
      .toBe(`文件路径不能超过 500 个字符：${'x'.repeat(501)}`)
    expect(validateProjectFiles([file({ fileType: 'x'.repeat(33) })]))
      .toBe('文件类型不能超过 32 个字符：index.html')
    expect(validateProjectFiles([file({ content: 'x'.repeat(200_001) })]))
      .toBe('项目文件内容过大：index.html')
  })

  it('rejects dangerous html content', () => {
    expect(validateProjectFiles([file({ content: '<main><script>alert(1)</script></main>' })]))
      .toBe('项目文件不能内联脚本')
    expect(validateProjectFiles([file({ content: '<main><button onclick="alert(1)">Run</button></main>' })]))
      .toBe('项目文件不能使用内联事件处理器')
    expect(validateProjectFiles([file({ content: '<main><img src=https://example.com/logo.png></main>' })]))
      .toBe('项目文件不能引用外部 URL')
  })

  it('rejects dangerous css and script content', () => {
    expect(validateProjectFiles([file({
      filePath: 'style.css',
      fileType: 'css',
      content: 'main { background-image: url("https://example.com/bg.png"); }',
    })])).toBe('项目文件不能引用外部 URL')
    expect(validateProjectFiles([file({
      filePath: 'script.js',
      fileType: 'js',
      content: "fetch('/api/data')",
    })])).toBe('项目文件不能执行网络请求')
    expect(validateProjectFiles([file({
      filePath: 'script.js',
      fileType: 'js',
      content: "new Function('alert(1)')",
    })])).toBe('项目文件不能使用动态代码执行')
  })
})
