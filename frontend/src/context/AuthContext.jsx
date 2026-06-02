import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import apiClient, { setAuthToken } from '../api/api';

const AuthContext = createContext(null);
const AUTH_TOKEN_KEY = 'wallet_auth_token';
const AUTH_USER_KEY = 'wallet_auth_user';

export function AuthProvider({ children }) {
  const [authUser, setAuthUser] = useState(() => {
    try {
      const saved = localStorage.getItem(AUTH_USER_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY));
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (token) {
      setAuthToken(token);
    }
  }, [token]);

  const login = async (credentials) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/auth/login', credentials);
      const auth = response.data;
      const authToken = `${auth.tokenType} ${auth.token}`;
      localStorage.setItem(AUTH_TOKEN_KEY, authToken);
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(auth.user));
      setToken(authToken);
      setAuthUser(auth.user);
    } catch (e) {
      setError(e.response?.data?.message || 'Login failed');
      throw e;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (payload) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/auth/register', payload);
      const auth = response.data;
      const authToken = `${auth.tokenType} ${auth.token}`;
      localStorage.setItem(AUTH_TOKEN_KEY, authToken);
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(auth.user));
      setToken(authToken);
      setAuthUser(auth.user);
    } catch (e) {
      setError(e.response?.data?.message || 'Registration failed');
      throw e;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    setAuthUser(null);
    setToken(null);
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    setAuthToken(null);
  };

  const value = useMemo(
    () => ({ authUser, login, logout, register, isLoading, error, setError }),
    [authUser, isLoading, error]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
