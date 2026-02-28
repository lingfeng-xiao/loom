<template>
  <div class="home">
    <h1>Welcome to Loom</h1>
    <p>这是一个前后端分离的项目示例</p>
    <button @click="fetchHello">获取后端数据</button>
    <div v-if="message">
      <h2>后端响应:</h2>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      message: ''
    }
  },
  methods: {
    async fetchHello() {
      try {
        const response = await fetch('/api/hello')
        const data = await response.json()
        this.message = data.message
      } catch (error) {
        console.error('Error fetching data:', error)
        this.message = '获取数据失败'
      }
    }
  }
}
</script>

<style scoped>
.home {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  color: #42b983;
}

button {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  margin: 20px 0;
}

button:hover {
  background-color: #35495e;
}
</style>