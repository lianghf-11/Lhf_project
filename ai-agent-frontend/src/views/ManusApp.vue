<template>
  <ChatBox ref="chat" :chatId="chatId" title="AI 超级智能体" icon="🤖"
           subtitle="全能助手，调工具执行复杂任务"
           @send="onSend" @newChat="newChat" />
</template>

<script setup>
import { ref } from 'vue'
import ChatBox from '../components/ChatBox.vue'

const chat = ref(null)
const chatId = ref(crypto.randomUUID())

function newChat() { chatId.value = crypto.randomUUID() }

function onSend(message) {
  fetch(`/api/ai/manus/chat?message=${encodeURIComponent(message)}`)
    .then(res => {
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buf = ''
      function pump() {
        reader.read().then(({ done, value }) => {
          if (done) { chat.value.setLoading(false); return }
          buf += decoder.decode(value, { stream: true })
          const parts = buf.split('\n\n')
          buf = parts.pop()
          parts.forEach(p => {
            const text = p.split('\n')
              .filter(line => line.startsWith('data:'))
              .map(line => line.slice(5))
              .join('\n').trim()
            if (text) chat.value.addAi(text)
          })
          pump()
        })
      }
      pump()
    })
    .catch(e => { chat.value.setLoading(false); console.error(e) })
}
</script>
