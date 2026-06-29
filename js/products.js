import {
  collection,
  doc,
  addDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  query,
  orderBy,
  serverTimestamp,
  runTransaction
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { db } from "./firebase-init.js";

const productsCol = collection(db, "products");

let cachedProducts = [];
const listeners = [];

export function subscribeProducts(callback) {
  listeners.push(callback);
  if (cachedProducts.length) callback(cachedProducts);
}

export function getCachedProducts() {
  return cachedProducts;
}

export function initProductsListener(onError) {
  const q = query(productsCol, orderBy("name"));
  return onSnapshot(
    q,
    (snap) => {
      cachedProducts = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      listeners.forEach((cb) => cb(cachedProducts));
    },
    (err) => {
      console.error("Ürün dinleyici hatası:", err);
      if (onError) onError(err);
    }
  );
}

export function findProductByBarcode(barcode) {
  const b = String(barcode || "").trim();
  if (!b) return null;
  return cachedProducts.find((p) => p.barcode === b) || null;
}

export function isBarcodeTaken(barcode, excludeId = null) {
  const b = String(barcode || "").trim();
  return cachedProducts.some((p) => p.barcode === b && p.id !== excludeId);
}

export async function addProduct({ name, price, stock, barcode, photo }) {
  await addDoc(productsCol, {
    name: name.trim(),
    price: Number(price),
    stock: Math.round(Number(stock)),
    barcode: String(barcode).trim(),
    photo: photo || "",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });
}

export async function updateProduct(id, { name, price, stock, barcode, photo }) {
  const data = {
    name: name.trim(),
    price: Number(price),
    stock: Math.round(Number(stock)),
    barcode: String(barcode).trim(),
    updatedAt: serverTimestamp()
  };
  if (photo !== undefined) data.photo = photo;
  await updateDoc(doc(db, "products", id), data);
}

export async function deleteProduct(id) {
  await deleteDoc(doc(db, "products", id));
}

// Stok girişi/çıkışı: pozitif delta = giriş, negatif delta = çıkış (fire/bozulma vb.)
export async function adjustStock(id, delta, reason) {
  await runTransaction(db, async (tx) => {
    const ref = doc(db, "products", id);
    const snap = await tx.get(ref);
    if (!snap.exists()) throw new Error("Ürün bulunamadı.");
    const current = snap.data().stock || 0;
    const next = current + delta;
    if (next < 0) throw new Error("Stok 0'ın altına düşemez.");
    tx.update(ref, { stock: next, updatedAt: serverTimestamp() });
  });

  try {
    await addDoc(collection(db, "stockLogs"), {
      productId: id,
      delta,
      reason: reason || "Belirtilmedi",
      createdAt: serverTimestamp()
    });
  } catch (e) {
    console.warn("Stok hareketi kaydedilemedi:", e);
  }
}
