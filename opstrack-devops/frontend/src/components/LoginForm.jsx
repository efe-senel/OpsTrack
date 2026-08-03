import { useEffect, useState } from "react";

export function LoginForm({ initialEmail, isSubmitting, error, validationErrors, onSubmit }) {
  const [form, setForm] = useState({ email: initialEmail, password: "" });

  useEffect(() => {
    setForm((current) => ({ ...current, email: initialEmail }));
  }, [initialEmail]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit({ email: form.email.trim(), password: form.password });
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      {error && <div className="error-message auth-error" role="alert">{error}</div>}

      <label htmlFor="login-email">
        E-posta
        <input
          autoComplete="email"
          id="login-email"
          name="email"
          onChange={updateField}
          required
          type="email"
          value={form.email}
        />
        {validationErrors.email && <span className="field-error">{validationErrors.email}</span>}
      </label>

      <label htmlFor="login-password">
        Parola
        <input
          autoComplete="current-password"
          id="login-password"
          name="password"
          onChange={updateField}
          required
          type="password"
          value={form.password}
        />
        {validationErrors.password && <span className="field-error">{validationErrors.password}</span>}
      </label>

      <button className="primary-button" disabled={isSubmitting} type="submit">
        {isSubmitting ? "Giriş yapılıyor…" : "Giriş yap"}
      </button>
    </form>
  );
}
