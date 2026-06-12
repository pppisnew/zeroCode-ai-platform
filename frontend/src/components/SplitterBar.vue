<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  currentRatio: number
}>()

const emit = defineEmits<{
  resize: [ratio: number]
}>()

const isActive = ref(false)
let startX = 0
let startRatio = 0
let containerWidth = 0

function onMouseDown(event: MouseEvent) {
  isActive.value = true
  startX = event.clientX
  startRatio = props.currentRatio
  const container = (event.target as HTMLElement).parentElement
  containerWidth = container?.clientWidth ?? window.innerWidth

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  event.preventDefault()
}

function onMouseMove(event: MouseEvent) {
  if (!isActive.value) return
  const delta = event.clientX - startX
  const newRatio = Math.min(0.75, Math.max(0.25, startRatio + delta / containerWidth))
  emit('resize', newRatio)
}

function onMouseUp() {
  isActive.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
}
</script>

<template>
  <div
    class="splitter-bar"
    :class="{ active: isActive }"
    @mousedown="onMouseDown"
  >
    <div class="splitter-grip" />
  </div>
</template>

<style scoped>
.splitter-bar {
  width: 5px;
  cursor: col-resize;
  background: transparent;
  position: relative;
  flex-shrink: 0;
  z-index: 5;
  transition: background 0.15s ease;
}

.splitter-bar:hover,
.splitter-bar.active {
  background: rgba(212, 168, 83, 0.15);
}

.splitter-grip {
  position: absolute;
  inset: 0 1px;
  background: rgba(212, 168, 83, 0.25);
  border-radius: 1px;
  transition: background 0.15s ease;
}

.splitter-bar.active .splitter-grip {
  background: rgba(212, 168, 83, 0.5);
}
</style>
