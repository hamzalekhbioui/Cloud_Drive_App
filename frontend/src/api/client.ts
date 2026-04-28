import axios from 'axios'

const client = axios.create({ baseURL: `${import.meta.env.VITE_API_BASE_URL ?? ''}/api` })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = error.config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('token')
      localStorage.removeItem('email')
      localStorage.removeItem('name')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client