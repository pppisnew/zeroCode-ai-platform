import { defineStore } from 'pinia'
import type { GeneratedFile, ProjectType } from '../types/generatedProject'

const initialFiles: GeneratedFile[] = [
  {
    filePath: 'index.html',
    fileType: 'html',
    content: '<main class="app"><h1>Todo Studio</h1><p>AI generated preview shell.</p><button>New task</button></main>',
  },
  {
    filePath: 'style.css',
    fileType: 'css',
    content:
      'body{margin:0;font-family:Inter,system-ui;background:#f7f8fb;color:#1f2937}.app{min-height:100vh;display:grid;place-content:center;gap:16px;text-align:center}button{border:0;border-radius:6px;background:#2563eb;color:white;padding:10px 16px}',
  },
  {
    filePath: 'script.js',
    fileType: 'js',
    content: 'console.log("ZeroCode preview ready")',
  },
]

export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({
    prompt: '生成一个现代风格的 Todo 应用',
    projectName: 'preview-shell',
    projectType: 'html' as ProjectType,
    currentAppId: undefined as string | undefined,
    currentVersionNo: undefined as number | undefined,
    selectedFilePath: initialFiles[0]?.filePath ?? '',
    files: initialFiles.map((file) => ({ ...file })),
  }),
  actions: {
    setFiles(files: GeneratedFile[]) {
      this.files = files.map((file) => ({ ...file }))
      this.selectedFilePath = this.files[0]?.filePath ?? ''
    },
  },
})
