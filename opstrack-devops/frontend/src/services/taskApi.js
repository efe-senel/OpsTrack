import { apiRequest } from "./apiClient";

const TASKS_ENDPOINT = "/api/v1/tasks";

const request = (path = "", options = {}) => apiRequest(`${TASKS_ENDPOINT}${path}`, options);

export const taskApi = {
  list: () => request(),
  create: (task) =>
    request("", {
      method: "POST",
      body: JSON.stringify(task),
    }),
  update: (id, task) =>
    request(`/${id}`, {
      method: "PUT",
      body: JSON.stringify(task),
    }),
  remove: (id) => request(`/${id}`, { method: "DELETE" }),
};
