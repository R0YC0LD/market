import {
  collection,
  doc,
  query,
  where,
  orderBy,
  onSnapshot,
  runTransaction,
  Timestamp,
  serverTimestamp
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { db } from "./firebase-init.js";

// Belirtilen tarih aralığındaki satışları gerçek zamanlı dinler.
export function subscribeSalesInRange(startDate, endDate, callback, onError) {
  const q = query(
    collection(db, "sales"),
    where("createdAt", ">=", Timestamp.fromDate(startDate)),
    where("createdAt", "<=", Timestamp.fromDate(endDate)),
    orderBy("createdAt", "desc")
  );
  return onSnapshot(
    q,
    (snap) => callback(snap.docs.map((d) => ({ id: d.id, ...d.data() }))),
    (err) => {
      console.error("Satış geçmişi hatası:", err);
      if (onError) onError(err);
    }
  );
}

// Satışı iptal eder: ürün stoklarını geri yükler, satışı "cancelled" olarak işaretler.
export async function cancelSale(saleId) {
  await runTransaction(db, async (tx) => {
    const saleRef = doc(db, "sales", saleId);
    const saleSnap = await tx.get(saleRef);
    if (!saleSnap.exists()) throw new Error("Satış bulunamadı.");
    const sale = saleSnap.data();
    if (sale.cancelled) throw new Error("Bu satış zaten iptal edilmiş.");

    const productRefs = sale.items.map((i) => doc(db, "products", i.productId));
    const productSnaps = await Promise.all(productRefs.map((ref) => tx.get(ref)));

    productSnaps.forEach((snap, idx) => {
      if (!snap.exists()) return; // Ürün silinmiş olabilir, stoğu geri yükleyecek bir yer yok.
      const ref = productRefs[idx];
      const currentStock = snap.data().stock || 0;
      tx.update(ref, { stock: currentStock + sale.items[idx].qty });
    });

    tx.update(saleRef, { cancelled: true, cancelledAt: serverTimestamp() });
  });
}
