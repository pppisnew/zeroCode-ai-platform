import type * as Monaco from 'monaco-editor'
import type { ComputedRef, Ref } from 'vue'

import type { GeneratedFile } from '../types/generatedProject'

let monacoApi: typeof Monaco | undefined

export function useMonacoEditor(
  editorContainer: Ref<HTMLElement | undefined>,
  selectedFile: ComputedRef<GeneratedFile | undefined>,
) {
  let codeEditor: Monaco.editor.IStandaloneCodeEditor | undefined
  let isApplyingEditorValue = false

  async function createEditor() {
    if (!editorContainer.value || codeEditor) {
      return
    }

    const monaco = await loadMonaco()
    if (!editorContainer.value || codeEditor) {
      return
    }

    codeEditor = monaco.editor.create(editorContainer.value, {
      value: selectedFile.value?.content ?? '',
      language: getEditorLanguage(selectedFile.value),
      theme: 'vs-dark',
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 13,
      lineHeight: 22,
      scrollBeyondLastLine: false,
      tabSize: 2,
      wordWrap: 'on',
    })
    codeEditor.onDidChangeModelContent(() => {
      if (isApplyingEditorValue) {
        return
      }

      const file = selectedFile.value
      if (file) {
        file.content = codeEditor?.getValue() ?? ''
      }
    })
  }

  function syncEditorValue() {
    if (!codeEditor) {
      return
    }

    const file = selectedFile.value
    isApplyingEditorValue = true
    codeEditor.setValue(file?.content ?? '')
    const model = codeEditor.getModel()
    if (model) {
      monacoApi?.editor.setModelLanguage(model, getEditorLanguage(file))
    }
    isApplyingEditorValue = false
  }

  function layoutEditor() {
    codeEditor?.layout()
  }

  function disposeEditor() {
    codeEditor?.dispose()
  }

  return {
    createEditor,
    syncEditorValue,
    layoutEditor,
    disposeEditor,
  }
}

async function loadMonaco() {
  if (monacoApi) {
    return monacoApi
  }

  const [
    monacoModule,
    editorWorkerModule,
    cssWorkerModule,
    htmlWorkerModule,
    tsWorkerModule,
  ] = await Promise.all([
    import('monaco-editor/esm/vs/editor/editor.api'),
    import('monaco-editor/esm/vs/editor/editor.worker?worker'),
    import('monaco-editor/esm/vs/language/css/css.worker?worker'),
    import('monaco-editor/esm/vs/language/html/html.worker?worker'),
    import('monaco-editor/esm/vs/language/typescript/ts.worker?worker'),
    import('monaco-editor/esm/vs/basic-languages/css/css.contribution'),
    import('monaco-editor/esm/vs/basic-languages/html/html.contribution'),
    import('monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution'),
    import('monaco-editor/esm/vs/basic-languages/typescript/typescript.contribution'),
  ])

  window.MonacoEnvironment = {
    getWorker(_: string, label: string) {
      if (label === 'css') {
        return new cssWorkerModule.default()
      }
      if (label === 'html') {
        return new htmlWorkerModule.default()
      }
      if (label === 'javascript' || label === 'typescript') {
        return new tsWorkerModule.default()
      }
      return new editorWorkerModule.default()
    },
  }
  monacoApi = monacoModule
  return monacoApi
}

function getEditorLanguage(file?: GeneratedFile) {
  if (!file) {
    return 'plaintext'
  }
  if (file.fileType === 'html' || file.filePath.endsWith('.html')) {
    return 'html'
  }
  if (file.fileType === 'css' || file.filePath.endsWith('.css')) {
    return 'css'
  }
  if (file.fileType === 'js' || file.filePath.endsWith('.js')) {
    return 'javascript'
  }
  if (file.fileType === 'ts' || file.filePath.endsWith('.ts')) {
    return 'typescript'
  }
  if (file.fileType === 'tsx' || file.filePath.endsWith('.tsx')) {
    return 'typescript'
  }
  if (file.fileType === 'vue' || file.filePath.endsWith('.vue')) {
    return 'html'
  }
  if (file.fileType === 'json' || file.filePath.endsWith('.json')) {
    return 'json'
  }
  return 'plaintext'
}
