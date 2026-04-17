import type { Theme } from 'vitepress'
import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'

import Layout from './Layout.vue'

import FeatureCard from '../components/FeatureCard.vue'


export default {
  extends: DefaultTheme,
  Layout: () => h(Layout),
  enhanceApp({ app }) {
    app.component('FeatureCard', FeatureCard)
  }
} satisfies Theme
