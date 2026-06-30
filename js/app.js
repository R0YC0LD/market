import { autoLogin, onAuthChange } from "./auth.js";
import {
  initProductsListener,
  subscribeProducts,
  findProductByBarcode,
  isBarcodeTaken,
  addProduct,
  updateProduct,
  deleteProduct,
  adjustStock
} from "./products.js";
import {
  getCart,
  getCartTotal,
  clearCart,
  addToCart,
  setCartQty,
  removeFromCart,
  completeSale,
  refreshCartStockLimits
} from "./sales.js";
import { subscribeSalesInRange, cancelSale } from "./history.js";
import { fetchSalesInRange, computeReport, generateTxtReport } from "./reports.js";
import { startScanner, stopScanner } from "./scanner.js";
import {
  formatCurrency,
  formatTime,
  startOfDay,
  endOfDay,
  startOfWeek,
  startOfMonth,
  debounce,
  escapeHtml,
  resizeImageToBase64,
  downloadTextFile,
  showToast,
  showLoading,
  hideLoading,
  openModal,
  closeModal,
  confirmDialog,
  friendlyError
} from "./utils.js";

// ---------------- DOM REFERANSLARI ----------------
const loginView = document.getElementById("loginView");
const appView = document.getElementById("appView");
const connStatus = document.getElementById("connStatus");

const navBtns = document.querySelectorAll(".nav-btn");
const screens = document.querySelectorAll(".screen");

// Satış ekranı
const barcodeInput = document.getElementById("barcodeInput");
const cameraScanBtn = document.getElementById("cameraScanBtn");
const productSearchInput = document.getElementById("productSearchInput");
const productGrid = document.getElementById("productGrid");
const cartItemsEl = document.getElementById("cartItems");
const cartTotalEl = document.getElementById("cartTotal");
const clearCartBtn = document.getElementById("clearCartBtn");
const payNakitBtn = document.getElementById("payNakitBtn");
const payKartBtn = document.getElementById("payKartBtn");
const completeSaleBtn = document.getElementById("completeSaleBtn");

// Stok ekranı
const addProductBtn = document.getElementById("addProductBtn");
const stockSearchInput = document.getElementById("stockSearchInput");
const stockListEl = document.getElementById("stockList");

// Ürün modalı
const productModalTitle = document.getElementById("productModalTitle");
const productForm = document.getElementById("productForm");
const productIdInput = document.getElementById("productId");
const productNameInput = document.getElementById("productName");
const productPriceInput = document.getElementById("productPrice");
const productStockInput = document.getElementById("productStock");
const productBarcodeInput = document.getElementById("productBarcode");
const productPhotoInput = document.getElementById("productPhotoInput");
const productPhotoPreview = document.getElementById("productPhotoPreview");
const photoPlaceholder = document.getElementById("photoPlaceholder");
const productFormError = document.getElementById("productFormError");
const deleteProductBtn = document.getElementById("deleteProductBtn");
const productCameraScanBtn = document.getElementById("productCameraScanBtn");
const productSaveBtn = productForm.querySelector('button[type="submit"]');

// Stok düzelt modalı
const stockAdjustProductName = document.getElementById("stockAdjustProductName");
const stockAdjustAmount = document.getElementById("stockAdjustAmount");
const stockAdjustReason = document.getElementById("stockAdjustReason");
const stockMinusBtn = document.getElementById("stockMinusBtn");
const stockPlusBtn = document.getElementById("stockPlusBtn");
const confirmStockAdjustBtn = document.getElementById("confirmStockAdjustBtn");
const dirInBtn = document.getElementById("dirInBtn");
const dirOutBtn = document.getElementById("dirOutBtn");

// Geçmiş ekranı
const historyDateInput = document.getElementById("historyDate");
const historyListEl = document.getElementById("historyList");

// Rapor ekranı
const reportTabs = document.querySelectorAll(".report-tab");
const reportSummaryEl = document.getElementById("reportSummary");
const reportTableEl = document.getElementById("reportTable");
const downloadReportBtn = document.getElementById("downloadReportBtn");

// ---------------- DURUM (STATE) ----------------
let currentProducts = [];
let productSearchTerm = "";
let stockSearchTerm = "";
let selectedPaymentMethod = null;
let editingProductId = null;
let photoPendingDataUrl = "";
let adjustingProductId = null;
let adjustDirection = "in";
let scannerTarget = "cart"; // 'cart' | 'form'
let activePeriod = "daily";
let currentReport = null;
let currentReportRange = null;

let unsubProducts = null;
let unsubHistory = null;

// ================================================================
// YARDIMCI
// ================================================================
function normalizeSearch(str) {
  return String(str || "").toLocaleLowerCase("tr-TR").trim();
}

function toLocalDateInputValue(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function parseLocalDateInputValue(str) {
  const [y, m, d] = str.split("-").map(Number);
  return new Date(y, m - 1, d);
}

// ================================================================
// OTOMATİK GİRİŞ
// ================================================================
onAuthChange((user) => {
  if (user) {
    loginView.classList.add("hidden");
    appView.classList.remove("hidden");
    historyDateInput.value = toLocalDateInputValue(new Date());
    if (!unsubProducts) {
      unsubProducts = initProductsListener((err) => showToast(friendlyError(err), "error"));
    }
    setView("satis");
  } else {
    autoLogin().catch(() => showToast("Bağlantı kurulamadı, sayfayı yenileyin.", "error"));
  }
});

subscribeProducts((products) => {
  currentProducts = products;
  renderProductGrid();
  renderStockList();
  refreshCartStockLimits();
  renderCart();
});

// ================================================================
// GEZİNME (NAVIGASYON)
// ================================================================
function setView(viewName) {
  screens.forEach((s) => s.classList.add("hidden"));
  document.getElementById("view-" + viewName).classList.remove("hidden");
  navBtns.forEach((b) => b.classList.toggle("active", b.dataset.view === viewName));

  if (viewName === "stok") renderStockList();
  if (viewName === "gecmis" && !unsubHistory) setupHistoryListener(historyDateInput.value);
  if (viewName === "rapor") loadReport();
}

navBtns.forEach((btn) => btn.addEventListener("click", () => setView(btn.dataset.view)));

// ================================================================
// BAĞLANTI DURUMU
// ================================================================
function updateConnStatus() {
  if (navigator.onLine) {
    connStatus.classList.remove("offline");
    connStatus.title = "Bağlı";
  } else {
    connStatus.classList.add("offline");
    connStatus.title = "Çevrimdışı - değişiklikler bağlantı gelince gönderilecek";
  }
}
window.addEventListener("online", updateConnStatus);
window.addEventListener("offline", updateConnStatus);
updateConnStatus();

// ================================================================
// SATIŞ EKRANI
// ================================================================
function renderProductGrid() {
  const term = normalizeSearch(productSearchTerm);
  const list = currentProducts.filter((p) => !term || normalizeSearch(p.name).includes(term));

  if (!list.length) {
    productGrid.innerHTML = `<p class="empty-hint">${currentProducts.length ? "Ürün bulunamadı." : "Henüz ürün eklenmemiş. Stok ekranından ürün ekleyin."}</p>`;
    return;
  }

  productGrid.innerHTML = list.map((p) => `
    <button type="button" class="product-card" data-id="${p.id}" ${p.stock <= 0 ? "disabled" : ""}>
      ${p.photo ? `<img class="pc-img" src="${p.photo}" alt="">` : `<div class="pc-img">🛒</div>`}
      <span class="pc-name">${escapeHtml(p.name)}</span>
      <span class="pc-price">${formatCurrency(p.price)}</span>
      <span class="pc-stock ${p.stock <= 5 ? "low" : ""}">Stok: ${p.stock}</span>
    </button>
  `).join("");
}

productSearchInput.addEventListener("input", debounce((e) => {
  productSearchTerm = e.target.value;
  renderProductGrid();
}, 200));

productGrid.addEventListener("click", (e) => {
  const btn = e.target.closest(".product-card");
  if (!btn || btn.disabled) return;
  const product = currentProducts.find((p) => p.id === btn.dataset.id);
  if (!product) return;
  try {
    addToCart(product, 1);
    renderCart();
    showToast(`${product.name} sepete eklendi`, "success", 1200);
  } catch (err) {
    showToast(err.message, "error");
  }
});

function handleBarcodeScanned(code) {
  const product = findProductByBarcode(code);
  if (!product) {
    showToast(`Barkod tanınmadı: ${code}`, "error");
    return;
  }
  try {
    addToCart(product, 1);
    renderCart();
    showToast(`${product.name} eklendi`, "success", 1200);
    if (navigator.vibrate) navigator.vibrate(35);
  } catch (err) {
    showToast(err.message, "error");
  }
}

barcodeInput.addEventListener("keydown", (e) => {
  if (e.key !== "Enter") return;
  e.preventDefault();
  const code = barcodeInput.value.trim();
  barcodeInput.value = "";
  if (code) handleBarcodeScanned(code);
});

function renderCart() {
  const items = getCart();
  if (!items.length) {
    cartItemsEl.innerHTML = `<p class="empty-hint">Sepet boş. Barkod okutun veya üründen seçin.</p>`;
  } else {
    cartItemsEl.innerHTML = items.map((i) => `
      <div class="cart-item" data-id="${i.productId}">
        <div class="ci-info">
          <div class="ci-name">${escapeHtml(i.name)}</div>
          <div class="ci-price">${formatCurrency(i.price)} / adet</div>
        </div>
        <div class="ci-qty">
          <button type="button" class="ci-dec">−</button>
          <span>${i.qty}</span>
          <button type="button" class="ci-inc">+</button>
        </div>
        <div class="ci-total">${formatCurrency(i.price * i.qty)}</div>
        <button type="button" class="ci-remove" title="Kaldır">🗑</button>
      </div>
    `).join("");
  }
  cartTotalEl.textContent = formatCurrency(getCartTotal());
  updateCompleteSaleBtnState();
}

cartItemsEl.addEventListener("click", (e) => {
  const row = e.target.closest(".cart-item");
  if (!row) return;
  const id = row.dataset.id;
  const item = getCart().find((i) => i.productId === id);
  if (!item) return;

  if (e.target.closest(".ci-inc")) {
    try { setCartQty(id, item.qty + 1); } catch (err) { showToast(err.message, "error"); }
    renderCart();
  } else if (e.target.closest(".ci-dec")) {
    setCartQty(id, item.qty - 1);
    renderCart();
  } else if (e.target.closest(".ci-remove")) {
    removeFromCart(id);
    renderCart();
  }
});

clearCartBtn.addEventListener("click", async () => {
  if (!getCart().length) return;
  const ok = await confirmDialog("Sepeti boşaltmak istediğinize emin misiniz?");
  if (!ok) return;
  clearCart();
  selectedPaymentMethod = null;
  payNakitBtn.classList.remove("selected");
  payKartBtn.classList.remove("selected");
  renderCart();
});

[payNakitBtn, payKartBtn].forEach((btn) => btn.addEventListener("click", () => {
  selectedPaymentMethod = btn.dataset.method;
  payNakitBtn.classList.toggle("selected", selectedPaymentMethod === "nakit");
  payKartBtn.classList.toggle("selected", selectedPaymentMethod === "kart");
  updateCompleteSaleBtnState();
}));

function updateCompleteSaleBtnState() {
  completeSaleBtn.disabled = !(getCart().length && selectedPaymentMethod);
}

completeSaleBtn.addEventListener("click", async () => {
  if (!getCart().length || !selectedPaymentMethod) return;
  completeSaleBtn.disabled = true;
  showLoading();
  const total = getCartTotal();
  try {
    await completeSale(selectedPaymentMethod);
    showToast(`Satış tamamlandı: ${formatCurrency(total)}`, "success");
    selectedPaymentMethod = null;
    payNakitBtn.classList.remove("selected");
    payKartBtn.classList.remove("selected");
    renderCart();
    barcodeInput.focus();
  } catch (err) {
    showToast(friendlyError(err), "error");
  } finally {
    hideLoading();
    updateCompleteSaleBtnState();
  }
});

// ================================================================
// KAMERA İLE BARKOD TARAMA
// ================================================================
async function openScanner(target) {
  scannerTarget = target;
  openModal("scannerModal");
  await startScanner("scannerView", onScannerDetected, (err) => {
    showToast(err.message, "error");
    closeModal("scannerModal");
  });
}

async function onScannerDetected(code) {
  await stopScanner();
  closeModal("scannerModal");
  if (scannerTarget === "form") {
    productBarcodeInput.value = code;
    if (isBarcodeTaken(code, editingProductId)) {
      showToast(`⚠️ Bu barkod (${code}) zaten başka bir ürüne kayıtlı.`, "error", 3200);
    } else {
      showToast(`✅ Barkod tanındı: ${code}`, "success", 1800);
      if (navigator.vibrate) navigator.vibrate(35);
      productNameInput.focus();
    }
  } else {
    handleBarcodeScanned(code);
  }
}

cameraScanBtn.addEventListener("click", () => openScanner("cart"));
productCameraScanBtn.addEventListener("click", () => openScanner("form"));

document.querySelectorAll(".modal-close").forEach((btn) => {
  btn.addEventListener("click", async () => {
    const id = btn.dataset.close;
    if (id === "scannerModal") await stopScanner();
    closeModal(id);
  });
});

// ================================================================
// STOK EKRANI
// ================================================================
function renderStockList() {
  const term = normalizeSearch(stockSearchTerm);
  const list = currentProducts.filter((p) =>
    !term || normalizeSearch(p.name).includes(term) || String(p.barcode).includes(stockSearchTerm.trim())
  );

  if (!list.length) {
    stockListEl.innerHTML = `<p class="empty-hint">${currentProducts.length ? "Ürün bulunamadı." : "Henüz ürün eklenmemiş."}</p>`;
    return;
  }

  stockListEl.innerHTML = list.map((p) => `
    <div class="stock-row" data-id="${p.id}">
      ${p.photo ? `<img class="sr-img" src="${p.photo}" alt="">` : `<div class="sr-img">🛒</div>`}
      <div class="sr-info">
        <div class="sr-name">${escapeHtml(p.name)}</div>
        <div class="sr-meta">${formatCurrency(p.price)} · Barkod: ${escapeHtml(p.barcode)}</div>
      </div>
      <span class="sr-stock-badge ${p.stock <= 5 ? "low" : ""}">${p.stock} adet</span>
      <div class="sr-actions">
        <button type="button" class="sr-edit" title="Düzenle">✏️</button>
        <button type="button" class="sr-adjust" title="Stok Düzelt">±</button>
      </div>
    </div>
  `).join("");
}

stockSearchInput.addEventListener("input", debounce((e) => {
  stockSearchTerm = e.target.value;
  renderStockList();
}, 200));

stockListEl.addEventListener("click", (e) => {
  const row = e.target.closest(".stock-row");
  if (!row) return;
  const product = currentProducts.find((p) => p.id === row.dataset.id);
  if (!product) return;

  if (e.target.closest(".sr-adjust")) {
    openStockAdjustModal(product);
  } else {
    openProductModalForEdit(product);
  }
});

addProductBtn.addEventListener("click", () => openProductModalForAdd());

// ---------------- ÜRÜN MODALI ----------------
function showFormError(msg) {
  productFormError.textContent = msg;
  productFormError.classList.remove("hidden");
}

function openProductModalForAdd() {
  editingProductId = null;
  photoPendingDataUrl = "";
  productForm.reset();
  productIdInput.value = "";
  productModalTitle.textContent = "Yeni Ürün";
  deleteProductBtn.classList.add("hidden");
  productPhotoPreview.src = "";
  productPhotoPreview.classList.add("hidden");
  photoPlaceholder.classList.remove("hidden");
  productFormError.classList.add("hidden");
  openModal("productModal");
  // Yeni ürün eklenirken barkodu otomatik tanıması için kamera hemen açılır.
  // Kamera/izin yoksa kullanıcı barkodu elle yazabilir (tarayıcı X ile kapatılabilir).
  openScanner("form");
}

function openProductModalForEdit(product) {
  editingProductId = product.id;
  photoPendingDataUrl = product.photo || "";
  productIdInput.value = product.id;
  productModalTitle.textContent = "Ürünü Düzenle";
  deleteProductBtn.classList.remove("hidden");
  productNameInput.value = product.name;
  productPriceInput.value = product.price;
  productStockInput.value = product.stock;
  productBarcodeInput.value = product.barcode;
  if (photoPendingDataUrl) {
    productPhotoPreview.src = photoPendingDataUrl;
    productPhotoPreview.classList.remove("hidden");
    photoPlaceholder.classList.add("hidden");
  } else {
    productPhotoPreview.classList.add("hidden");
    photoPlaceholder.classList.remove("hidden");
  }
  productFormError.classList.add("hidden");
  openModal("productModal");
}

productPhotoInput.addEventListener("change", async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  try {
    photoPendingDataUrl = await resizeImageToBase64(file);
    productPhotoPreview.src = photoPendingDataUrl;
    productPhotoPreview.classList.remove("hidden");
    photoPlaceholder.classList.add("hidden");
  } catch (err) {
    showToast("Fotoğraf yüklenemedi.", "error");
  }
});

productForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  productFormError.classList.add("hidden");

  const name = productNameInput.value.trim();
  const price = parseFloat(productPriceInput.value);
  const stock = parseInt(productStockInput.value, 10);
  const barcode = productBarcodeInput.value.trim();

  if (!name) return showFormError("Ürün adı gerekli.");
  if (isNaN(price) || price < 0) return showFormError("Geçerli bir fiyat girin.");
  if (isNaN(stock) || stock < 0) return showFormError("Geçerli bir stok adedi girin.");
  if (!barcode) return showFormError("Barkod gerekli.");
  if (isBarcodeTaken(barcode, editingProductId)) return showFormError("Bu barkod başka bir ürüne ait.");

  productSaveBtn.disabled = true;
  showLoading();
  try {
    if (editingProductId) {
      await updateProduct(editingProductId, { name, price, stock, barcode, photo: photoPendingDataUrl });
      showToast("Ürün güncellendi.", "success");
    } else {
      await addProduct({ name, price, stock, barcode, photo: photoPendingDataUrl });
      showToast("Ürün eklendi.", "success");
    }
    closeModal("productModal");
  } catch (err) {
    showFormError(friendlyError(err));
  } finally {
    productSaveBtn.disabled = false;
    hideLoading();
  }
});

deleteProductBtn.addEventListener("click", async () => {
  if (!editingProductId) return;
  const ok = await confirmDialog("Bu ürünü silmek istediğinize emin misiniz? Bu işlem geri alınamaz.");
  if (!ok) return;
  showLoading();
  try {
    await deleteProduct(editingProductId);
    showToast("Ürün silindi.", "success");
    closeModal("productModal");
  } catch (err) {
    showToast(friendlyError(err), "error");
  } finally {
    hideLoading();
  }
});

// ---------------- STOK DÜZELT MODALI ----------------
function updateDirectionButtons() {
  dirInBtn.classList.toggle("selected", adjustDirection === "in");
  dirOutBtn.classList.toggle("selected", adjustDirection === "out");
}

function openStockAdjustModal(product) {
  adjustingProductId = product.id;
  adjustDirection = "in";
  stockAdjustProductName.textContent = `${product.name} — Mevcut stok: ${product.stock} adet`;
  stockAdjustAmount.value = 1;
  updateDirectionButtons();
  openModal("stockAdjustModal");
}

[dirInBtn, dirOutBtn].forEach((btn) => btn.addEventListener("click", () => {
  adjustDirection = btn.dataset.dir;
  updateDirectionButtons();
}));

stockMinusBtn.addEventListener("click", () => {
  stockAdjustAmount.value = Math.max(1, (parseInt(stockAdjustAmount.value, 10) || 1) - 1);
});
stockPlusBtn.addEventListener("click", () => {
  stockAdjustAmount.value = (parseInt(stockAdjustAmount.value, 10) || 1) + 1;
});

confirmStockAdjustBtn.addEventListener("click", async () => {
  if (!adjustingProductId) return;
  const amount = parseInt(stockAdjustAmount.value, 10);
  if (isNaN(amount) || amount <= 0) {
    showToast("Geçerli bir adet girin.", "error");
    return;
  }
  const delta = adjustDirection === "in" ? amount : -amount;
  showLoading();
  try {
    await adjustStock(adjustingProductId, delta, stockAdjustReason.value);
    showToast("Stok güncellendi.", "success");
    closeModal("stockAdjustModal");
  } catch (err) {
    showToast(friendlyError(err), "error");
  } finally {
    hideLoading();
  }
});

// ================================================================
// GEÇMİŞ EKRANI
// ================================================================
function setupHistoryListener(dateStr) {
  if (unsubHistory) { unsubHistory(); unsubHistory = null; }
  const day = parseLocalDateInputValue(dateStr);
  unsubHistory = subscribeSalesInRange(
    startOfDay(day),
    endOfDay(day),
    renderHistory,
    (err) => showToast(friendlyError(err), "error")
  );
}

historyDateInput.addEventListener("change", () => setupHistoryListener(historyDateInput.value));

function renderHistory(sales) {
  if (!sales.length) {
    historyListEl.innerHTML = `<p class="empty-hint">Bu tarihte satış kaydı yok.</p>`;
    return;
  }
  historyListEl.innerHTML = sales.map((sale) => {
    const time = sale.createdAt?.toDate ? formatTime(sale.createdAt.toDate()) : "--:--";
    const itemsText = (sale.items || []).map((i) => `${i.qty}x ${i.name}`).join(", ");
    const methodLabel = sale.paymentMethod === "nakit" ? "💵 Nakit" : "💳 Kart";
    return `
      <div class="history-card ${sale.cancelled ? "cancelled" : ""}" data-id="${sale.id}">
        <div class="hc-top">
          <span class="hc-time">${time}</span>
          <span class="hc-method">${methodLabel}</span>
        </div>
        <div class="hc-items">${escapeHtml(itemsText)}</div>
        <div class="hc-bottom">
          <span class="hc-total">${formatCurrency(sale.total)}</span>
          ${sale.cancelled
            ? '<span class="hc-cancel-badge">İptal Edildi</span>'
            : '<button type="button" class="hc-cancel-btn">İptal Et</button>'}
        </div>
      </div>
    `;
  }).join("");
}

historyListEl.addEventListener("click", async (e) => {
  const btn = e.target.closest(".hc-cancel-btn");
  if (!btn) return;
  const card = e.target.closest(".history-card");
  const saleId = card.dataset.id;
  const ok = await confirmDialog("Bu satışı iptal etmek istediğinize emin misiniz? Ürün stokları geri yüklenecek.");
  if (!ok) return;
  showLoading();
  try {
    await cancelSale(saleId);
    showToast("Satış iptal edildi.", "success");
  } catch (err) {
    showToast(friendlyError(err), "error");
  } finally {
    hideLoading();
  }
});

// ================================================================
// RAPOR EKRANI
// ================================================================
reportTabs.forEach((btn) => btn.addEventListener("click", () => {
  reportTabs.forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
  activePeriod = btn.dataset.period;
  loadReport();
}));

async function loadReport() {
  const now = new Date();
  let start, label;
  if (activePeriod === "weekly") { start = startOfWeek(now); label = "Haftalık"; }
  else if (activePeriod === "monthly") { start = startOfMonth(now); label = "Aylık"; }
  else { start = startOfDay(now); label = "Günlük"; }
  const end = endOfDay(now);

  showLoading();
  try {
    const sales = await fetchSalesInRange(start, end);
    currentReport = computeReport(sales);
    currentReportRange = { start, end, label };
    renderReportUI(currentReport);
  } catch (err) {
    showToast(friendlyError(err), "error");
  } finally {
    hideLoading();
  }
}

function renderReportUI(report) {
  reportSummaryEl.innerHTML = `
    <div class="rs-card"><div class="rs-label">Toplam Ciro</div><div class="rs-value">${formatCurrency(report.totalRevenue)}</div></div>
    <div class="rs-card"><div class="rs-label">Toplam Satış</div><div class="rs-value">${report.saleCount}</div></div>
    <div class="rs-card"><div class="rs-label">💵 Nakit</div><div class="rs-value">${formatCurrency(report.nakitTotal)}</div></div>
    <div class="rs-card"><div class="rs-label">💳 Kart</div><div class="rs-value">${formatCurrency(report.kartTotal)}</div></div>
    <div class="rs-card full"><div class="rs-label">İptal Edilen Satış</div><div class="rs-value">${report.cancelledCount}</div></div>
  `;

  if (!report.productBreakdown.length) {
    reportTableEl.innerHTML = `<p class="empty-hint">Bu dönemde satış kaydı bulunmuyor.</p>`;
    return;
  }

  reportTableEl.innerHTML = `
    <table class="report-table">
      <thead><tr><th>Ürün</th><th class="num">Adet</th><th class="num">Tutar</th></tr></thead>
      <tbody>
        ${report.productBreakdown.map((p, idx) => `
          <tr><td>${idx === 0 ? "🏆 " : ""}${escapeHtml(p.name)}</td><td class="num">${p.qty}</td><td class="num">${formatCurrency(p.revenue)}</td></tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

downloadReportBtn.addEventListener("click", () => {
  if (!currentReport || !currentReportRange) {
    showToast("Önce rapor yüklenmesini bekleyin.", "error");
    return;
  }
  const txt = generateTxtReport(currentReport, currentReportRange.label, currentReportRange.start, currentReportRange.end);
  const periodSlug = { daily: "gunluk", weekly: "haftalik", monthly: "aylik" }[activePeriod];
  const dateStr = toLocalDateInputValue(new Date());
  downloadTextFile(`cakir-bufe-rapor-${periodSlug}-${dateStr}.txt`, txt);
  showToast("Rapor indirildi.", "success");
});
