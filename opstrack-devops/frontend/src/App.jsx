import { useCallback, useEffect, useMemo, useState } from "react";
import { TaskCard } from "./components/TaskCard";
import { TaskForm } from "./components/TaskForm";
import { taskApi } from "./services/taskApi";

export default function App() {
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [busyAction, setBusyAction] = useState(null);
  const [error, setError] = useState("");

  const loadTasks = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      setTasks(await taskApi.list());
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const completedCount = useMemo(
    () => tasks.filter((task) => task.status === "DONE").length,
    [tasks],
  );

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
      setError(requestError.message);
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
      setError(requestError.message);
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
      setError(requestError.message);
    } finally {
      setBusyAction(null);
    }
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="brand" href="/" aria-label="OpsTrack ana sayfa">
          <span className="brand-mark">O</span>
          <span>OpsTrack</span>
        </a>
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
