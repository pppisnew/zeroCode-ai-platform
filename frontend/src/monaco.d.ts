declare module 'monaco-editor/esm/vs/editor/editor.api' {
  export * from 'monaco-editor'
}

declare module 'monaco-editor/esm/vs/basic-languages/css/css.contribution'
declare module 'monaco-editor/esm/vs/basic-languages/html/html.contribution'
declare module 'monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution'
declare module 'monaco-editor/esm/vs/basic-languages/typescript/typescript.contribution'

declare global {
  interface Window {
    MonacoEnvironment?: import('monaco-editor').Environment
  }
}
