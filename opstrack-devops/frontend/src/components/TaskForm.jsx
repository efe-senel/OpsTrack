import { useEffect, useState } from "react";

const emptyTask = { title: "", description: "", status: "OPEN" };

export function TaskForm({ task, isSaving, onCancel, onSubmit }) {
  const [form, setForm] = useState(emptyTask);

  useEffect(() => {
    setForm(
      task
        ? {
            title: task.title,
            description: task.description ?? "",
            status: task.status,
          }
        : emptyTask,
    );
  }, [task]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit({ ...form, title: form.title.trim(), description: form.description.trim() });
  }

  return (
    <form className="task-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <div>
          <p className="eyebrow">{task ? "Görevi güncelle" : "Yeni kayıt"}</p>
          <h2>{task ? "Görevi düzenle" : "Yeni görev oluştur"}</h2>
        </div>
        {task && (
          <button className="text-button" type="button" onClick={onCancel}>
            Vazgeç
          </button>
        )}
      </div>

      <label>
        Başlık
        <input
          autoFocus
          maxLength="120"
          name="title"
          onChange={updateField}
          placeholder="Örn. Dağıtım kontrol listesini hazırla"
          required
          value={form.title}
        />
      </label>

      <label>
        Açıklama
        <textarea
          maxLength="1000"
          name="description"
          onChange={updateField}
          placeholder="Görevin kapsamını ve önemli notları yazın"
          rows="4"
          value={form.description}
        />
        <span className="character-count">{form.description.length}/1000</span>
      </label>

      <label>
        Durum
        <select name="status" onChange={updateField} value={form.status}>
          <option value="OPEN">Açık</option>
          <option value="IN_PROGRESS">Devam ediyor</option>
          <option value="DONE">Tamamlandı</option>
        </select>
      </label>

      <button className="primary-button" disabled={isSaving} type="submit">
        {isSaving ? "Kaydediliyor…" : task ? "Değişiklikleri kaydet" : "Görev ekle"}
      </button>
    </form>
  );
}
