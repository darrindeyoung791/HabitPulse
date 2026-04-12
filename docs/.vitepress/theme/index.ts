import type { Theme } from 'vitepress'
import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'

import Layout from './Layout.vue'

export default {
  extends: DefaultTheme,
  Layout: () => h(Layout)
} satisfies Theme
