<template>
  <div class="feature-card">
    <div class="feature-icon">{{ icon }}</div>
    <h3 class="feature-title">{{ title }}</h3>
    <p class="feature-description">{{ description }}</p>
    <ul v-if="points" class="feature-points">
      <li v-for="point in points" :key="point">{{ point }}</li>
    </ul>
  </div>
</template>

<script setup>
defineProps({
  icon: String,
  title: String,
  description: String,
  points: Array
})
</script>

<style scoped>
.feature-card {
  padding: 24px 24px 24px 24px;
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  border: 1px solid var(--vp-c-brand-1);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 20px;
}

.feature-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 10px;
  color: var(--vp-c-text-1);
}

.feature-description {
  color: var(--vp-c-text-2);
  margin-bottom: 16px;
  line-height: 1.5;
}

.feature-points {
  padding-left: 20px;
  margin: 0;
  color: var(--vp-c-text-2);
  font-size: 14px;
  line-height: 1.8;
}

.feature-points li {
  margin-bottom: 4px;
}

.feature-points li:last-child {
  margin-bottom: 0;
}
</style>