import {
  collection,
  doc,
  runTransaction,
  serverTimestamp
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { db } from "./firebase-init.js";
import { getCachedProducts } from "./products.js";

let cart = []; // [{ productId, name, price, qty, stock }]

export function getCart() {
  return cart;
}

export function getCartTotal() {
  return cart.reduce((sum, item) => sum + item.price * item.qty, 0);
}

export function clearCart() {
  cart = [];
}

export function addToCart(product, qty = 1) {
  const existing = cart.find((i) => i.productId === product.id);
  const currentQty = existing ? existing.qty : 0;
  if (currentQty + qty > product.stock) {
    throw new Error(`Yetersiz stok. "${product.name}" için en fazla ${product.stock} adet eklenebilir.`);
  }
  if (existing) {
    existing.qty += qty;
  } else {
    cart.push({ productId: product.id, name: product.name, price: product.price, qty, stock: product.stock });
  }
}

export function setCartQty(productId, qty) {
  const item = cart.find((i) => i.productId === productId);
  if (!item) return;
  if (qty <= 0) {
    cart = cart.filter((i) => i.productId !== productId);
    return;
  }
  if (qty > item.stock) {
    throw new Error(`Yetersiz stok. En fazla ${item.stock} adet eklenebilir.`);
  }
  item.qty = qty;
}

export function removeFromCart(productId) {
  cart = cart.filter((i) => i.productId !== productId);
}

// Satışı tamamlar: her ürünün stoğunu atomik olarak düşer ve satış kaydı oluşturur.
export async function completeSale(paymentMethod) {
  if (!cart.length) throw new Error("Sepet boş.");
  if (paymentMethod !== "nakit" && paymentMethod !== "kart") {
    throw new Error("Ödeme yöntemi seçilmedi.");
  }

  const items = cart.map((i) => ({ ...i }));
  const total = items.reduce((s, i) => s + i.price * i.qty, 0);
  const saleRef = doc(collection(db, "sales"));

  await runTransaction(db, async (tx) => {
    const productRefs = items.map((i) => doc(db, "products", i.productId));
    const snaps = await Promise.all(productRefs.map((ref) => tx.get(ref)));

    snaps.forEach((snap, idx) => {
      if (!snap.exists()) throw new Error(`"${items[idx].name}" artık mevcut değil.`);
      const currentStock = snap.data().stock || 0;
      if (currentStock < items[idx].qty) {
        throw new Error(`"${items[idx].name}" için stok yetersiz (kalan: ${currentStock}).`);
      }
    });

    snaps.forEach((snap, idx) => {
      const ref = productRefs[idx];
      const currentStock = snap.data().stock || 0;
      tx.update(ref, { stock: currentStock - items[idx].qty });
    });

    tx.set(saleRef, {
      items,
      total,
      paymentMethod,
      cancelled: false,
      createdAt: serverTimestamp()
    });
  });

  clearCart();
  return saleRef.id;
}

export function refreshCartStockLimits() {
  const products = getCachedProducts();
  cart.forEach((item) => {
    const p = products.find((x) => x.id === item.productId);
    if (p) item.stock = p.stock;
  });
}
