import axios from 'axios'

export type BackendHealthResponse = {
  status: string
  timestamp?: string
}

export function pingBackend() {
  return axios.get<BackendHealthResponse>('/health', { timeout: 4000 })
}

