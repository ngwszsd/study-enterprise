import { defineConfig, loadEnv } from '@rsbuild/core'
import { pluginReact } from '@rsbuild/plugin-react'
import { pluginTailwindcss } from '@rsbuild/plugin-tailwindcss'

const { publicVars } = loadEnv({ prefixes: ['VITE_'] })

export default defineConfig({
  plugins: [pluginReact(), pluginTailwindcss()],
  dev: {
    client: {
      port: 15173,
    },
  },
  server: {
    port: 15173,
  },
  source: {
    entry: {
      index: './src/main.tsx',
    },
    define: publicVars,
  },
  html: {
    title: 'study-enterprise',
  },
})
