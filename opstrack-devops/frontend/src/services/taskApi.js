const TASKS_ENDPOINT = "/api/v1/tasks";

const fallbackMessages = {
  400: "Gönderilen görev bilgileri geçersiz.",
  404: "Görev bulunamadı.",
  500: "Sunucuda beklenmeyen bir hata oluştu.",
};

async function request(path = "", options = {}) {
  let response;

  try {
    response = await fetch(`${TASKS_ENDPOINT}${path}`, {
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
      ...options,
    });
  } catch {
    throw new Error("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
  }

  if (!response.ok) {
    let details;
    try {
      details = await response.json();
    } catch {
      details = null;
    }

    const validationMessage = details?.validationErrors
      ? Object.values(details.validationErrors).join(" ")
      : null;
    throw new Error(
      validationMessage ||
        fallbackMessages[response.status] ||
        "İşlem tamamlanamadı. Lütfen tekrar deneyin.",
    );
  }

  return response.status === 204 ? null : response.json();
}

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
