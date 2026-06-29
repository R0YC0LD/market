// ============================================================
// GENEL YARDIMCI FONKSİYONLAR
// ============================================================

export function formatCurrency(amount) {
  const n = Number(amount) || 0;
  return n.toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " ₺";
}

export function formatDate(date) {
  return date.toLocaleDateString("tr-TR");
}

export function formatDateTime(date) {
  return date.toLocaleDateString("tr-TR") + " " + date.toLocaleTimeString("tr-TR", { hour: "2-digit", minute: "2-digit" });
}

export function formatTime(date) {
  return date.toLocaleTimeString("tr-TR", { hour: "2-digit", minute: "2-digit" });
}

export function startOfDay(d) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

export function endOfDay(d) {
  const x = new Date(d);
  x.setHours(23, 59, 59, 999);
  return x;
}

export function startOfWeek(d) {
  const x = startOfDay(d);
  const day = x.getDay(); // 0=Pazar
  const diff = (day === 0 ? -6 : 1 - day); // Pazartesi başlangıç
  x.setDate(x.getDate() + diff);
  return x;
}

export function startOfMonth(d) {
  const x = startOfDay(d);
  x.setDate(1);
  return x;
}

export function debounce(fn, delay = 250) {
  let t = null;
  return (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), delay);
  };
}

export function escapeHtml(str) {
  return String(str ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[c]));
}

// Seçilen görseli yeniden boyutlandırıp sıkıştırarak base64 (dataURL) döndürür.
// Firebase Storage gerektirmeden ürün fotoğrafını doğrudan Firestore belgesinde saklamamızı sağlar.
export function resizeImageToBase64(file, maxSize = 360, quality = 0.7) {
  return new Promise((resolve, reject) => {
    if (!file) return resolve("");
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Dosya okunamadı"));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error("Görsel yüklenemedi"));
      img.onload = () => {
        let { width, height } = img;
        if (width > height) {
          if (width > maxSize) { height = Math.round(height * (maxSize / width)); width = maxSize; }
        } else {
          if (height > maxSize) { width = Math.round(width * (maxSize / height)); height = maxSize; }
        }
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, width, height);
        resolve(canvas.toDataURL("image/jpeg", quality));
      };
      img.src = reader.result;
    };
    reader.readAsDataURL(file);
  });
}

export function downloadTextFile(filename, content) {
  const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// ---------------- TOAST ----------------
export function showToast(message, type = "default", duration = 2600) {
  const container = document.getElementById("toastContainer");
  if (!container) return;
  const el = document.createElement("div");
  el.className = "toast" + (type !== "default" ? " " + type : "");
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), duration);
}

// ---------------- LOADING OVERLAY ----------------
let loadingCount = 0;
export function showLoading() {
  loadingCount++;
  document.getElementById("loadingOverlay")?.classList.remove("hidden");
}
export function hideLoading() {
  loadingCount = Math.max(0, loadingCount - 1);
  if (loadingCount === 0) document.getElementById("loadingOverlay")?.classList.add("hidden");
}

// ---------------- MODAL ----------------
export function openModal(id) {
  document.getElementById(id)?.classList.remove("hidden");
}
export function closeModal(id) {
  document.getElementById(id)?.classList.add("hidden");
}

// ---------------- ONAY DİYALOĞU ----------------
export function confirmDialog(message) {
  return new Promise((resolve) => {
    const text = document.getElementById("confirmModalText");
    text.textContent = message;
    openModal("confirmModal");
    const okBtn = document.getElementById("confirmModalOk");
    const cancelBtn = document.getElementById("confirmModalCancel");
    const cleanup = (result) => {
      closeModal("confirmModal");
      okBtn.removeEventListener("click", onOk);
      cancelBtn.removeEventListener("click", onCancel);
      resolve(result);
    };
    const onOk = () => cleanup(true);
    const onCancel = () => cleanup(false);
    okBtn.addEventListener("click", onOk);
    cancelBtn.addEventListener("click", onCancel);
  });
}

// Genel hata mesajlarını kullanıcının anlayacağı Türkçe metne çevirir.
export function friendlyError(err) {
  const code = err?.code || "";
  const map = {
    "auth/invalid-credential": "Kullanıcı adı veya şifre hatalı.",
    "auth/invalid-login-credentials": "Kullanıcı adı veya şifre hatalı.",
    "auth/wrong-password": "Kullanıcı adı veya şifre hatalı.",
    "auth/user-not-found": "Kullanıcı adı veya şifre hatalı.",
    "auth/too-many-requests": "Çok fazla hatalı deneme yapıldı. Lütfen biraz sonra tekrar deneyin.",
    "auth/network-request-failed": "İnternet bağlantısı sağlanamadı.",
    "permission-denied": "Bu işlem için yetkiniz yok. Lütfen tekrar giriş yapın.",
    "unavailable": "Sunucuya şu anda ulaşılamıyor. Bağlantınızı kontrol edin.",
    "auth/api-key-not-valid.-please-pass-a-valid-api-key.": "Sistem henüz kurulmamış: js/firebase-config.js dosyasındaki Firebase bilgileri eksik veya hatalı. README.md dosyasındaki kurulum adımlarını uygulayın.",
    "auth/configuration-not-found": "Sistem henüz kurulmamış: Firebase Authentication'da Email/Password girişi etkinleştirilmemiş. README.md dosyasındaki kurulum adımlarını uygulayın."
  };
  if (map[code]) return map[code];
  if (code.startsWith("auth/")) return "Giriş yapılamadı. Lütfen bilgileri kontrol edip tekrar deneyin.";
  return err?.message || "Beklenmeyen bir hata oluştu.";
}
