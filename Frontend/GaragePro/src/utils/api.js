export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const isJwtExpired = (token) => {
  if (!token) return true;
  try {
    const parts = token.split('.');
    if (parts.length < 2) return true;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64));
    if (payload.exp && Date.now() >= payload.exp * 1000) {
      return true;
    }
    return false;
  } catch {
    return false;
  }
};

export const fetchWithAuth = async (endpoint, options = {}) => {
  const token = localStorage.getItem('token');

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Only wipe session and redirect if the token is truly missing or expired
    const isAuthEndpoint = endpoint.includes('/auth/login') || endpoint.includes('/auth/register');
    if (!isAuthEndpoint && (!token || isJwtExpired(token))) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/';
    }
  }

  return response;
};

export const api = {
  get: async (endpoint) => {
    const res = await fetchWithAuth(endpoint, { method: 'GET' });
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      throw new Error(data?.message || data?.error || `Request failed with status ${res.status}`);
    }
    return data;
  },

  post: async (endpoint, body) => {
    const res = await fetchWithAuth(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
    });
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      throw new Error(data?.message || data?.error || `Request failed with status ${res.status}`);
    }
    return data;
  },

  put: async (endpoint, body) => {
    const res = await fetchWithAuth(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      throw new Error(data?.message || data?.error || `Request failed with status ${res.status}`);
    }
    return data;
  },

  delete: async (endpoint) => {
    const res = await fetchWithAuth(endpoint, { method: 'DELETE' });
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      throw new Error(data?.message || data?.error || `Request failed with status ${res.status}`);
    }
    return data;
  },
};
