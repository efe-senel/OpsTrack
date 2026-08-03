import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthScreen } from "./components/AuthScreen";
import { TaskCard } from "./components/TaskCard";
import { TaskForm } from "./components/TaskForm";
import { authApi } from "./services/authApi";
import { taskApi } from "./services/taskApi";

export default function App() {
  const [authStatus, setAuthStatus] = useState("checking");
  const [user, setUser] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [busyAction, setBusyAction] = useState(null);
  const [error, setError] = useState("");

  const returnToLogin = useCallback(() => {
    setAuthStatus("anonymous");
    setUser(null);
    setTasks([]);
    setEditingTask(null);
    setBusyAction(null);
    setError("");
  }, []);

  const handleTaskError = useCallback((requestError) => {
    if (requestError.status === 401) {
      returnToLogin();
      return;
    }
    setError(requestError.message);
  }, [returnToLogin]);

  const loadTasks = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      setTasks(await taskApi.list());
    } catch (requestError) {
      handleTaskError(requestError);
    } finally {
      setIsLoading(false);
    }
  }, [handleTaskError]);

  useEffect(() => {
    let isActive = true;

    async function restoreSession() {
      try {
        const currentUser = await authApi.me();
        if (!isActive) return;
        setUser(currentUser);
        setAuthStatus("authenticated");
        await loadTasks();
      } catch (requestError) {
        if (!isActive) return;
        if (requestError.status !== 401) setError(requestError.message);
        setAuthStatus("anonymous");
      }
    }

    restoreSession();
    return () => {
      isActive = false;
    };
  }, [loadTasks]);

  const completedCount = useMemo(
    () => tasks.filter((task) => task.status === "DONE").length,
    [tasks],
  );

  async function login(credentials) {
    setError("");
    const loggedInUser = await authApi.login(credentials);
    setUser(loggedInUser);
    setAuthStatus("authenticated");
    await loadTasks();
  }

  async function register(credentials) {
    setError("");
    await authApi.register(credentials);
  }

  async function logout() {
    setIsLoggingOut(true);
    setError("");
    try {
      await authApi.logout();
      returnToLogin();
    } catch (requestError) {
      if (requestError.status === 401) returnToLogin();
      else setError(requestError.message);
    } finally {
      setIsLoggingOut(false);
    }
  }

  async function saveTask(payload) {
    setIsSaving(true);
    setError("");
    try {
      if (editingTask) {
        const updated = await taskApi.update(editingTask.id, payload);
        setTasks((current) => current.map((task) => (task.id === updated.id ? updated : task)));
        setEditingTask(null);
      } else {
        const created = await taskApi.create(payload);
        setTasks((current) => [...current, created]);
      }
    } catch (requestError) {
      handleTaskError(requestError);
    } finally {
      setIsSaving(false);
    }
  }

  async function changeStatus(task, status) {
    setBusyAction({ id: task.id, type: "status" });
    setError("");
    try {
      const updated = await taskApi.update(task.id, {
        title: task.title,
        description: task.description,
        status,
      });
      setTasks((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    } catch (requestError) {
      handleTaskError(requestError);
    } finally {
      setBusyAction(null);
    }
  }

  async function deleteTask(task) {
    if (!window.confirm(`“${task.title}” görevi kalıcı olarak silinsin mi?`)) return;

    setBusyAction({ id: task.id, type: "delete" });
    setError("");
    try {
      await taskApi.remove(task.id);
      setTasks((current) => current.filter((item) => item.id !== task.id));
      if (editingTask?.id === task.id) setEditingTask(null);
    } catch (requestError) {
      handleTaskError(requestError);
    } finally {
      setBusyAction(null);
    }
  }

  if (authStatus === "checking") {
    return (
      <main className="session-loading" aria-live="polite">
        <span className="brand-mark">O</span>
        <span className="spinner" />
        <p>Oturum kontrol ediliyor…</p>
      </main>
    );
  }

  if (authStatus === "anonymous") {
    return <AuthScreen onLogin={login} onRegister={register} sessionError={error} />;
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="header-topline">
          <a className="brand" href="/" aria-label="OpsTrack ana sayfa">
            <span className="brand-mark">O</span>
            <span>OpsTrack</span>
          </a>
          <div className="user-menu">
            <span><strong>{user.name}</strong><small>{user.email}</small></span>
            <button className="header-button" disabled={isLoggingOut} onClick={logout} type="button">
              {isLoggingOut ? "Çıkış yapılıyor…" : "Çıkış yap"}
            </button>
          </div>
        </div>
        <div className="header-copy">
          <p className="eyebrow">Operasyon merkezi</p>
          <h1>Ekibin işlerini tek yerde takip et.</h1>
          <p>Görevleri oluşturun, ilerlemeyi güncelleyin ve odağınızı koruyun.</p>
        </div>
        <div className="summary">
          <div><strong>{tasks.length}</strong><span>Toplam görev</span></div>
          <div><strong>{completedCount}</strong><span>Tamamlanan</span></div>
        </div>
      </header>

      <main className="workspace">
        <aside>
          <TaskForm
            isSaving={isSaving}
            onCancel={() => setEditingTask(null)}
            onSubmit={saveTask}
            task={editingTask}
          />
        </aside>

        <section className="task-section" aria-live="polite">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Güncel görünüm</p>
              <h2>Görevler</h2>
            </div>
            <button className="text-button" disabled={isLoading} onClick={loadTasks}>
              Yenile
            </button>
          </div>

          {error && (
            <div className="error-message" role="alert">
              <span>{error}</span>
              <button onClick={() => setError("")} aria-label="Hata mesajını kapat">×</button>
            </div>
          )}

          {isLoading ? (
            <div className="state-panel"><span className="spinner" />Görevler yükleniyor…</div>
          ) : tasks.length === 0 ? (
            <div className="state-panel empty-state">
              <span className="empty-icon">✓</span>
              <h3>Henüz görev yok</h3>
              <p>İlk görevinizi soldaki formdan oluşturarak başlayın.</p>
            </div>
          ) : (
            <div className="task-list">
              {tasks.map((task) => (
                <TaskCard
                  busyAction={busyAction}
                  key={task.id}
                  onDelete={deleteTask}
                  onEdit={setEditingTask}
                  onStatusChange={changeStatus}
                  task={task}
                />
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
