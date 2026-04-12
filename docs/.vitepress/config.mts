import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  srcDir: "docs",
  
  head: [
    ['link', { rel: 'icon', href: '/images/HabitPulse_round_icon.svg' }]
  ],

  title: "HabitPulse Docs",
  description: "A VitePress Site for HabitPulse Users",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: '/images/HabitPulse_round_icon.svg',

    search: {
      provider: 'local'
    },

    lastUpdated: {
      text: '最后编辑',
      formatOptions: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      }
    },

    nav: [
      { text: '主页', link: '/' },
      { text: '关于 HabitPulse', link: '/what-is-habitpulse' },
      { text: '下载', link: '/download' },
    ],

    sidebar: [
      {
        text: '入门指南',
        items: [
          { text: 'HabitPulse 是什么', link: '/what-is-habitpulse' },
          { text: '获取 HabitPulse', link: '/download' }
        ]
      },
      {
        text: '示例',
        items: [
          { text: 'Markdown 示例', link: '/markdown-examples' },
          { text: 'API 示例', link: '/api-examples' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/darrindeyoung791/HabitPulse' }
    ],

    footer: {
      message: '如无特殊说明，本站全部内容在 <a href="https://creativecommons.org/licenses/by-sa/4.0/deed.en" target="_blank" rel="noopener">CC-BY-SA 4.0</a> 协议之条款下提供',
      copyright: 'Copyright © 2026-present darrindeyoung791'
    }
  }
})
