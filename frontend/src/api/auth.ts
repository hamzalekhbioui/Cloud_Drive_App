import client from './client'

export interface AuthResponse {
  token: string
  email: string
  name: string
}

export const register = (name: string, email: string, password: string) =>
  client.post<AuthResponse>('/auth/register', { name, email, password })

export const login = (email: string, password: string) =>
  client.post<AuthResponse>('/auth/login', { email, password })