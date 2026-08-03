import { useState } from "react";
import { LoginForm } from "./LoginForm";
import { RegisterForm } from "./RegisterForm";

export function AuthScreen({ onLogin, onRegister, sessionError }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [validationErrors, setValidationErrors] = useState({});
  const [notice, setNotice] = useState("");

  function switchMode(nextMode) {
    setMode(nextMode);
    setError("");
    setValidationErrors({});
    setNotice("");
  }

  async function submitLogin(credentials) {
    setIsSubmitting(true);
    setError("");
    setValidationErrors({});
    setEmail(credentials.email);
    try {
      await onLogin(credentials);
    } catch (requestError) {
      const fieldErrors = requestError.validationErrors ?? {};
      setError(Object.keys(fieldErrors).length ? "" : requestError.message);
      setValidationErrors(fieldErrors);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function submitRegister(credentials) {
    setIsSubmitting(true);
    setError("");
    setValidationErrors({});
    setEmail(credentials.email);
    try {
      await onRegister(credentials);
      setMode("login");
      setNotice("Hesabınız oluşturuldu. Şimdi parolanızla giriş yapabilirsiniz.");
    } catch (requestError) {
      const fieldErrors = requestError.validationErrors ?? {};
      setError(Object.keys(fieldErrors).length ? "" : requestError.message);
      setValidationErrors(fieldErrors);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-shell">
      <section className="auth-intro">
        <a className="brand" href="/" aria-label="OpsTrack ana sayfa">
          <span className="brand-mark">O</span>
          <span>OpsTrack</span>
        </a>
        <div>
          <p className="eyebrow">Operasyon merkezi</p>
          <h1>Ekibin işlerini tek yerde takip et.</h1>
          <p>Görevleri oluşturun, ilerlemeyi güncelleyin ve odağınızı koruyun.</p>
        </div>
      </section>

      <main className="auth-main">
        <section className="auth-card" aria-labelledby="auth-title">
          <p className="eyebrow">OpsTrack'e hoş geldiniz</p>
          <h2 id="auth-title">{mode === "login" ? "Hesabınıza giriş yapın" : "Yeni hesap oluşturun"}</h2>
          <p className="auth-copy">
            {mode === "login" ? "Görev alanınıza devam etmek için bilgilerinizi girin." : "Ekibinizin görevlerini takip etmeye başlayın."}
          </p>

          {sessionError && !error && <div className="error-message auth-error" role="alert">{sessionError}</div>}
          {notice && <div className="success-message" role="status">{notice}</div>}

          {mode === "login" ? (
            <LoginForm
              error={error}
              initialEmail={email}
              isSubmitting={isSubmitting}
              onSubmit={submitLogin}
              validationErrors={validationErrors}
            />
          ) : (
            <RegisterForm
              error={error}
              initialEmail={email}
              isSubmitting={isSubmitting}
              onSubmit={submitRegister}
              validationErrors={validationErrors}
            />
          )}

          <p className="auth-switch">
            {mode === "login" ? "Hesabınız yok mu?" : "Zaten hesabınız var mı?"}
            <button className="text-button" disabled={isSubmitting} onClick={() => switchMode(mode === "login" ? "register" : "login")} type="button">
              {mode === "login" ? "Kayıt olun" : "Giriş yapın"}
            </button>
          </p>
        </section>
      </main>
    </div>
  );
}
