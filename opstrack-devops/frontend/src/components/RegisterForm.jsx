import { useState } from "react";

export function RegisterForm({ initialEmail, isSubmitting, error, validationErrors, onSubmit }) {
  const [form, setForm] = useState({ name: "", email: initialEmail, password: "" });

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit({ name: form.name.trim(), email: form.email.trim(), password: form.password });
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      {error && <div className="error-message auth-error" role="alert">{error}</div>}

      <label htmlFor="register-name">
        Ad
        <input autoComplete="name" id="register-name" name="name" onChange={updateField} required value={form.name} />
        {validationErrors.name && <span className="field-error">{validationErrors.name}</span>}
      </label>

      <label htmlFor="register-email">
        E-posta
        <input
          autoComplete="email"
          id="register-email"
          name="email"
          onChange={updateField}
          required
          type="email"
          value={form.email}
        />
        {validationErrors.email && <span className="field-error">{validationErrors.email}</span>}
      </label>

      <label htmlFor="register-password">
        Parola
        <input
          aria-describedby="password-hint"
          autoComplete="new-password"
          id="register-password"
          minLength="8"
          name="password"
          onChange={updateField}
          required
          type="password"
          value={form.password}
        />
        <span className="field-hint" id="password-hint">En az 8 karakter kullanın.</span>
        {validationErrors.password && <span className="field-error">{validationErrors.password}</span>}
      </label>

      <button className="primary-button" disabled={isSubmitting} type="submit">
        {isSubmitting ? "Hesap oluşturuluyor…" : "Hesap oluştur"}
      </button>
    </form>
  );
}
