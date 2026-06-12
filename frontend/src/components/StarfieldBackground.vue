<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvas = ref<HTMLCanvasElement>()
const STAR_COUNT = 120
const STAR_COLORS = [
  'rgba(212,168,83,0.9)',   // gold
  'rgba(240,208,96,0.8)',   // bright gold
  'rgba(110,198,248,0.7)',  // starlight blue
  'rgba(167,139,250,0.5)',  // stardust purple
  'rgba(232,224,208,0.6)',  // warm white
]

interface Star {
  x: number
  y: number
  radius: number
  color: string
  baseAlpha: number
  phase: number
  speed: number
}

function createStars(w: number, h: number): Star[] {
  return Array.from({ length: STAR_COUNT }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    radius: 0.4 + Math.random() * 1.8,
    color: STAR_COLORS[Math.floor(Math.random() * STAR_COLORS.length)],
    baseAlpha: 0.3 + Math.random() * 0.7,
    phase: Math.random() * Math.PI * 2,
    speed: 0.005 + Math.random() * 0.02,
  }))
}

let stars: Star[] = []
let animationId = 0
let width = 0
let height = 0

function draw() {
  const ctx = canvas.value?.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)

  for (const star of stars) {
    star.phase += star.speed
    const alpha = star.baseAlpha * (0.5 + 0.5 * Math.sin(star.phase))

    ctx.beginPath()
    ctx.arc(star.x, star.y, star.radius, 0, Math.PI * 2)
    ctx.fillStyle = star.color.replace(/[\d.]+\)$/, `${alpha.toFixed(2)})`)
    ctx.fill()

    // Glow halo for brighter stars
    if (star.radius > 1.2 && alpha > 0.5) {
      ctx.beginPath()
      ctx.arc(star.x, star.y, star.radius * 2.5, 0, Math.PI * 2)
      ctx.fillStyle = star.color.replace(/[\d.]+\)$/, `${(alpha * 0.12).toFixed(3)})`)
      ctx.fill()
    }
  }

  animationId = requestAnimationFrame(draw)
}

function resize() {
  width = window.innerWidth
  height = window.innerHeight
  if (canvas.value) {
    canvas.value.width = width
    canvas.value.height = height
    stars = createStars(width, height)
  }
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  draw()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  cancelAnimationFrame(animationId)
})
</script>

<template>
  <canvas ref="canvas" class="starfield" aria-hidden="true" />
</template>

<style scoped>
.starfield {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
</style>
