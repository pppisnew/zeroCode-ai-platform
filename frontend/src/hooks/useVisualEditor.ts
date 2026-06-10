import type { Editor as GrapesEditor } from 'grapesjs'
import type { ComputedRef, Ref } from 'vue'

import type { GeneratedFile } from '../types/generatedProject'

export function useVisualEditor(
  visualEditorContainer: Ref<HTMLElement | undefined>,
  files: Ref<GeneratedFile[]>,
  canUseVisualEditor: ComputedRef<boolean>,
) {
  let visualEditor: GrapesEditor | undefined
  let isApplyingVisualValue = false

  async function createVisualEditor() {
    if (!canUseVisualEditor.value || !visualEditorContainer.value || visualEditor) {
      return
    }

    const [{ default: grapesjs }] = await Promise.all([
      import('grapesjs'),
      import('grapesjs/dist/css/grapes.min.css'),
    ])
    if (!visualEditorContainer.value || visualEditor) {
      return
    }

    visualEditor = grapesjs.init({
      container: visualEditorContainer.value,
      height: '100%',
      storageManager: false,
      components: findProjectFile('html', '.html')?.content ?? '',
      style: findProjectFile('css', '.css')?.content ?? '',
    })

    visualEditor.BlockManager.add('section', {
      label: 'Section',
      content: '<section><h2>Section title</h2><p>Describe this area.</p></section>',
    })
    visualEditor.BlockManager.add('button', {
      label: 'Button',
      content: '<button type="button">Action</button>',
    })
    visualEditor.on('update', syncVisualFiles)
  }

  function syncVisualEditor() {
    if (!visualEditor || !canUseVisualEditor.value) {
      return
    }

    isApplyingVisualValue = true
    visualEditor.setComponents(findProjectFile('html', '.html')?.content ?? '')
    visualEditor.setStyle(findProjectFile('css', '.css')?.content ?? '')
    isApplyingVisualValue = false
  }

  function syncVisualFiles() {
    if (!visualEditor || isApplyingVisualValue || !canUseVisualEditor.value) {
      return
    }

    upsertProjectFile('index.html', 'html', visualEditor.getHtml())
    upsertProjectFile('style.css', 'css', visualEditor.getCss() ?? '')
  }

  function disposeVisualEditor() {
    visualEditor?.destroy()
  }

  function findProjectFile(fileType: string, extension: string) {
    return files.value.find((file) => file.fileType === fileType || file.filePath.endsWith(extension))
  }

  function upsertProjectFile(filePath: string, fileType: string, content: string) {
    const file = findProjectFile(fileType, `.${fileType}`)
    if (file) {
      file.content = content
      return
    }

    files.value = [
      ...files.value,
      {
        filePath,
        fileType,
        content,
      },
    ]
  }

  return {
    createVisualEditor,
    syncVisualEditor,
    disposeVisualEditor,
  }
}
