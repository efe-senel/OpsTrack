const statusLabels = {
  OPEN: "Açık",
  IN_PROGRESS: "Devam ediyor",
  DONE: "Tamamlandı",
};

export function TaskCard({ task, busyAction, onDelete, onEdit, onStatusChange }) {
  const isBusy = busyAction?.id === task.id;

  return (
    <article className="task-card">
      <div className="card-topline">
        <span className={`status-badge status-${task.status.toLowerCase()}`}>
          {statusLabels[task.status]}
        </span>
        <span className="task-date">
          {new Intl.DateTimeFormat("tr-TR", {
            day: "numeric",
            month: "short",
            year: "numeric",
          }).format(new Date(task.createdAt))}
        </span>
      </div>

      <h3>{task.title}</h3>
      <p className={task.description ? "" : "muted"}>
        {task.description || "Bu görev için açıklama eklenmemiş."}
      </p>

      <div className="card-actions">
        <label className="status-control">
          <span>Durum</span>
          <select
            aria-label={`${task.title} durumunu değiştir`}
            disabled={isBusy}
            onChange={(event) => onStatusChange(task, event.target.value)}
            value={task.status}
          >
            <option value="OPEN">Açık</option>
            <option value="IN_PROGRESS">Devam ediyor</option>
            <option value="DONE">Tamamlandı</option>
          </select>
        </label>
        <div className="action-buttons">
          <button className="secondary-button" disabled={isBusy} onClick={() => onEdit(task)}>
            Düzenle
          </button>
          <button className="danger-button" disabled={isBusy} onClick={() => onDelete(task)}>
            {isBusy && busyAction.type === "delete" ? "Siliniyor…" : "Sil"}
          </button>
        </div>
      </div>
    </article>
  );
}
