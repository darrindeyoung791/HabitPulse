import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  srcDir: "docs",
  base: '/HabitPulse/',
  cleanUrls: true,

  head: [
    ['link', { rel: 'icon', href: '/HabitPulse/images/HabitPulse_round_icon.svg' }]
  ],

  title: "HabitPulse Docs",
  description: "A VitePress Site for HabitPulse Users",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: {
      light: '/images/HabitPulse_round_icon.svg',
      dark: '/images/HabitPulse_round_icon.svg',
      link: '/'
    },

    search: {
      provider: 'local',
      options: {
        detailedView: true,
        translations: {
          button: {
            buttonText: '搜索文档',
            buttonAriaLabel: '搜索文档'
          },
          modal: {
            noResultsText: '无法找到相关结果',
            resetButtonTitle: '清除查询条件',
            footer: {
              selectText: '选择',
              navigateText: '切换',
              closeText: '关闭'
            }
          }
        }
      }
    },

    lastUpdated: {
      text: '最后更新于',
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
      { text: '下载', link: '/download' },
      { text: '教程', link: '/tutorial/first-time' },
      { text: '团队', link: '/team' },
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
        text: '教程',
        items: [
          { text: '帮助与反馈', link: '/tutorial/help-and-feedback'},
          { text: '初次上手', link: '/tutorial/first-time'},
          { text: '新建与编辑习惯', link: '/tutorial/add-and-edit-habit'},
          { text: '删除与排序习惯', link: '/tutorial/delete-and-sort-habit'},
          { text: '打卡习惯', link: '/tutorial/checkin'},
          { text: '联系人', link: '/tutorial/contacts'},
          { text: '记录', link: '/tutorial/records'},
          { text: '搜索与筛选', link: '/tutorial/search-and-filter'},
        ]
      },
      {
        text: '关于',
        items: [
          { text: '开发团队', link: '/team' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/darrindeyoung791/HabitPulse' }
    ],

    footer: {
      message: '如无特殊说明，本站全部内容在 <a href="https://creativecommons.org/licenses/by-sa/4.0/deed.en" target="_blank" rel="noopener">CC-BY-SA 4.0</a> 协议之条款下提供',
      copyright: 'Copyright © 2026-present darrindeyoung791'
    },

    returnToTopLabel: '回到顶部',

    docFooter: {
      prev: '上一篇',
      next: '下一篇'
    },

    outline: {
      label: '本文目录'
    },

    sidebarMenuLabel: '菜单',

    darkModeSwitchLabel: '主题',
    
    lightModeSwitchTitle: '切换到浅色模式',
    
    darkModeSwitchTitle: '切换到深色模式',
  }
})
