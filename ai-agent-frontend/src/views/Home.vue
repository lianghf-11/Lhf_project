<template>
  <div class="home">
    <h1>AI 智能平台</h1>
    <div class="cards">
      <router-link to="/love-app" class="card">
        <span class="icon">💕</span>
        <h2>AI 恋爱大师</h2>
        <p>智能恋爱顾问，帮你解决感情难题</p>
      </router-link>
      <router-link to="/manus" class="card">
        <span class="icon">🤖</span>
        <h2>AI 超级智能体</h2>
        <p>全能助手，调工具执行复杂任务</p>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.home { text-align: center; padding: 60px 20px; }
h1 { margin-bottom: 40px; }
.cards { display: flex; gap: 24px; justify-content: center; flex-wrap: wrap; }
.card {
  display: block; width: 280px; padding: 30px 20px;
  border-radius: 12px; background: #fff; box-shadow: 0 2px 12px rgba(0,0,0,.1);
  text-decoration: none; color: #333; transition: transform .2s;
}
.card:hover { transform: translateY(-4px); }
.icon { font-size: 48px; display: block; margin-bottom: 12px; }
h2 { margin: 0 0 8px; }
p { margin: 0; color: #666; }
</style>
