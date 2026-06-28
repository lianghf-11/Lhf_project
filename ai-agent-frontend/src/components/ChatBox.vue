<template>
  <div class="chat-page">
    <header class="top-bar">
      <button class="back-btn" @click="$router.push('/')">&#8592; 返回</button>
      <div class="title-group">
        <span class="title">{{ title }}</span>
        <span class="chat-id">{{ chatId?.slice(0, 8) }}...</span>
      </div>
      <button class="new-btn" @click="newChat">新对话</button>
    </header>
    <div class="messages" ref="msgBox">
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon">{{ icon }}</div>
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </div>
      <div v-for="(m, i) in messages" :key="i"
           :class="['msg-row', m.role === 'user' ? 'msg-right' : 'msg-left']">
        <div v-if="m.role === 'ai'" class="avatar ai-avatar">AI</div>
        <div class="bubble-wrapper">
          <div :class="['bubble', m.role]" v-html="renderText(m.text)"></div>
        </div>
        <div v-if="m.role === 'user'" class="avatar user-avatar">我</div>
      </div>
      <div v-if="loading" class="msg-row msg-left">
        <div class="avatar ai-avatar">AI</div>
        <div class="bubble-wrapper">
          <div class="bubble ai typing-dots"><span></span><span></span><span></span></div>
        </div>
      </div>
    </div>
    <div class="input-area">
      <div class="input-wrapper">
        <input v-model="input" @keydown.enter="send" placeholder="输入消息..." ref="inputRef" />
        <button class="send-btn" @click="send" :disabled="loading || !input.trim()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({ chatId: String, title: String, icon: String, subtitle: String })
const emits = defineEmits(['send', 'newChat'])
const router = useRouter()

const messages = ref([])
const input = ref('')
const loading = ref(false)
const msgBox = ref(null)
const inputRef = ref(null)

function scrollDown() {
  nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })
}

function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true
  scrollDown()
  emits('send', text)
}

function addAi(text) {
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'ai') {
    last.text += text
  } else {
    messages.value.push({ role: 'ai', text })
  }
  scrollDown()
}

function setLoading(v) { loading.value = v }
function newChat() { messages.value = []; emits('newChat') }

function renderText(text) {
  const div = document.createElement('div')
  div.textContent = text
  let html = div.innerHTML
  // markdown link [text](api/files/...) — image 后缀则直接展示
  html = html.replace(/\[([^\]]+)\]\((\/?api\/files\/[^)]+)\)/g, (m, label, url) => {
    const href = url.startsWith('/') ? url : '/' + url
    if (/\.(jpg|jpeg|png|gif|webp|bmp)(\?.*)?$/i.test(url)) {
      return `<br/><img src="${href}" alt="${label}" class="chat-img" loading="lazy"/><br/>`
    }
    return `<a href="${href}" target="_blank" class="file-link">${label}</a>`
  })
  return html
}

defineExpose({ addAi, setLoading })
</script>

<style scoped>
.chat-page {
  display: flex; flex-direction: column; height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}
.top-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: rgba(255,255,255,.85);
  backdrop-filter: blur(10px); border-bottom: 1px solid #e8e8e8;
  position: sticky; top: 0; z-index: 10;
}
.title-group { text-align: center; }
.title-group .title { display: block; font-size: 16px; font-weight: 600; color: #333; }
.title-group .chat-id { font-size: 11px; color: #aaa; }
.back-btn, .new-btn {
  background: none; border: 1px solid #ddd; padding: 6px 12px;
  border-radius: 6px; cursor: pointer; font-size: 13px; color: #666;
}
.back-btn:hover, .new-btn:hover { background: #f0f0f0; }

.messages { flex: 1; overflow-y: auto; padding: 16px; }
.welcome { text-align: center; padding: 60px 20px; }
.welcome-icon { font-size: 56px; margin-bottom: 12px; }
.welcome h2 { margin: 0 0 8px; font-size: 22px; color: #333; }
.welcome p { color: #999; font-size: 14px; }

.msg-row { display: flex; align-items: flex-end; margin-bottom: 16px; gap: 8px; }
.msg-right { flex-direction: row-reverse; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0; color: #fff;
}
.ai-avatar { background: linear-gradient(135deg, #667eea, #764ba2); }
.user-avatar { background: linear-gradient(135deg, #1677ff, #69b1ff); }

.bubble-wrapper { max-width: 72%; }
.bubble {
  padding: 12px 16px; border-radius: 16px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word; font-size: 14px;
}
.bubble.ai { background: #fff; color: #333; border-bottom-left-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.bubble.user { background: linear-gradient(135deg, #1677ff, #4096ff); color: #fff; border-bottom-right-radius: 4px; }

.typing-dots { display: flex; gap: 4px; padding: 14px 18px; }
.typing-dots span { width: 7px; height: 7px; border-radius: 50%; background: #bbb; animation: bounce 1.2s infinite; }
.typing-dots span:nth-child(2) { animation-delay: .2s; }
.typing-dots span:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%,60%,100% { transform: translateY(0) } 30% { transform: translateY(-6px) } }

.input-area { padding: 12px 16px 20px; background: rgba(255,255,255,.6); }
.input-wrapper {
  display: flex; align-items: center; background: #fff;
  border-radius: 24px; padding: 4px 4px 4px 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06); border: 1px solid #eee;
}
.input-wrapper input {
  flex: 1; border: none; outline: none; padding: 10px 0;
  font-size: 14px; background: transparent;
}
.send-btn {
  width: 38px; height: 38px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, #1677ff, #4096ff); color: #fff;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: transform .2s;
}
.send-btn:hover { transform: scale(1.05); }
.send-btn:disabled { opacity: .4; transform: none; }
:deep(.file-link) {
  display: inline-block; margin: 8px 0; padding: 6px 14px;
  background: #1677ff; color: #fff !important; border-radius: 6px; text-decoration: none; font-size: 13px;
}
:deep(.file-link:hover) { background: #4096ff; }
:deep(.chat-img) { max-width: 260px; border-radius: 8px; margin: 8px 0; display: block; }
</style>
