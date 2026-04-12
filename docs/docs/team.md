---
layout: page
---
<script setup>
import {
  VPTeamPage,
  VPTeamPageTitle,
  VPTeamMembers,
  VPTeamPageSection
} from 'vitepress/theme'

const humanMembers = [
  {
    avatar: 'https://github.com/darrindeyoung791.png',
    name: 'darrindeyoung791',
    title: '项目发起者、总设计、总规划、总测试、文档',
    links: [
      { icon: 'github', link: 'https://github.com/darrindeyoung791' },
    ]
  },
  {
    avatar: 'https://github.com/ying8502.png',
    name: 'ying8502',
    title: '功能规划、总测试、文档',
    links: [
      { icon: 'github', link: 'https://github.com/ying8502' },
    ]
  },
  {
    avatar: 'https://github.com/Frozentime3.png',
    name: 'Frozentime3',
    title: '总之做了一些工作',
    links: [
      { icon: 'github', link: 'https://github.com/Frozentime3' },
    ]
  },
]

const AIAgents = [
  {
    avatar: 'https://qwenlm.github.io/favicon.png',
    name: 'Qwen Code',
    title: '阿里旗下的命令行 AI 智能体',
    links: [
      { icon: 'github', link: 'https://github.com/QwenLM/qwen-code' },
      { 
        icon: {
          svg: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2A10,10 0 0,1 22,12M6,13H14L10.5,16.5L11.92,17.92L17.84,12L11.92,6.08L10.5,7.5L14,11H6V13Z"/></svg>'
        },
        link: 'https://qwen.ai/qwencode' 
      },
    ]
  },
  {
    avatar: 'images/icon/github-copilot.svg',
    name: 'GitHub Copilot',
    title: 'GitHub 自带的 AI 智能编程助手',
    links: [
      {
        icon: {
          svg: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2A10,10 0 0,1 22,12M6,13H14L10.5,16.5L11.92,17.92L17.84,12L11.92,6.08L10.5,7.5L14,11H6V13Z"/></svg>'
        },
        link: 'https://github.com/features/copilot'
      },
    ]
  },
]
</script>

<VPTeamPage>
  <VPTeamPageTitle>
    <template #title>
      HabitPulse 专业团队
    </template>
    <template #lead>
      HabitPulse 的诞生离不开以下团队成员的参与：
    </template>
  </VPTeamPageTitle>
  <VPTeamMembers size="medium" :members="humanMembers" />
  <VPTeamPageSection>
    <template #title>
      AI Agents
    </template>
    <template #lead>
      不那么冷的冷知识，HabitPulse 的诞生其实更离不开这些 AI 工具：
    </template>
    <template #members>
      <VPTeamMembers size="small" :members="AIAgents" />
    </template>
  </VPTeamPageSection>
</VPTeamPage>