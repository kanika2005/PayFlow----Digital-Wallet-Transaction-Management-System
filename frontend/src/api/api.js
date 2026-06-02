import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export function setAuthToken(token) {
  if (token) {
    apiClient.defaults.headers.common.Authorization = token;
  } else {
    delete apiClient.defaults.headers.common.Authorization;
  }
}

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('wallet_auth_token');
  if (token) {
    config.headers.Authorization = token;
  }
  return config;
});

export default apiClient;
