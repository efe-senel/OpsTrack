import { apiRequest, clearCsrfToken, getCsrfToken } from "./apiClient";

const AUTH_ENDPOINT = "/api/v1/auth";

export const authApi = {
  getCsrfToken,
  register: (credentials) =>
    apiRequest(`${AUTH_ENDPOINT}/register`, {
      method: "POST",
      body: JSON.stringify(credentials),
    }),
  login: (credentials) =>
    apiRequest(`${AUTH_ENDPOINT}/login`, {
      method: "POST",
      body: JSON.stringify(credentials),
    }),
  me: () => apiRequest(`${AUTH_ENDPOINT}/me`),
  logout: async () => {
    const response = await apiRequest(`${AUTH_ENDPOINT}/logout`, { method: "POST" });
    clearCsrfToken();
    return response;
  },
};
