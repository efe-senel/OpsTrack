const fallbackMessages = {
  400: "Gönderilen bilgiler geçersiz.",
  401: "Oturumunuz sona erdi. Lütfen tekrar giriş yapın.",
  403: "Bu işlem için yetkiniz bulunmuyor.",
  404: "İstenen kayıt bulunamadı.",
  409: "Bu bilgilerle mevcut bir kayıt bulunuyor.",
  500: "Sunucuda beklenmeyen bir hata oluştu.",
};

const unsafeMethods = new Set(["POST", "PUT", "PATCH", "DELETE"]);
let csrfToken = null;
let csrfRequest = null;

export class ApiError extends Error {
  constructor(message, { status = 0, details = null } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
    this.validationErrors = details?.validationErrors ?? {};
  }
}

async function parseResponse(response) {
  if (response.status === 204) return null;

  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return null;

  try {
    return await response.json();
  } catch {
    return null;
  }
}

async function performRequest(path, options) {
  let response;

  try {
    response = await fetch(path, {
      ...options,
      credentials: "include",
    });
  } catch {
    throw new ApiError("Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.");
  }

  const details = await parseResponse(response);
  if (!response.ok) {
    if (response.status === 401) clearCsrfToken();

    const validationMessage = details?.validationErrors
      ? Object.values(details.validationErrors).join(" ")
      : null;

    throw new ApiError(
      validationMessage ||
        details?.message ||
        fallbackMessages[response.status] ||
        "İşlem tamamlanamadı. Lütfen tekrar deneyin.",
      { status: response.status, details },
    );
  }

  return details;
}

export async function getCsrfToken({ force = false } = {}) {
  if (force) csrfToken = null;
  if (csrfToken) return csrfToken;

  if (!csrfRequest) {
    csrfRequest = performRequest("/api/v1/auth/csrf", { method: "GET" })
      .then((token) => {
        csrfToken = token;
        return token;
      })
      .finally(() => {
        csrfRequest = null;
      });
  }

  return csrfRequest;
}

export function clearCsrfToken() {
  csrfToken = null;
  csrfRequest = null;
}

export async function apiRequest(path, options = {}) {
  const method = (options.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);

  if (options.body != null && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const isUnsafe = unsafeMethods.has(method);
  if (isUnsafe) {
    const token = await getCsrfToken();
    headers.set(token.headerName, token.token);
  }

  try {
    return await performRequest(path, { ...options, method, headers });
  } catch (requestError) {
    if (!isUnsafe || requestError.status !== 403) throw requestError;

    const token = await getCsrfToken({ force: true });
    headers.set(token.headerName, token.token);
    return performRequest(path, { ...options, method, headers });
  }
}
