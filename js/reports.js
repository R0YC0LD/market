import {
  collection,
  query,
  where,
  getDocs,
  Timestamp
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import { db } from "./firebase-init.js";
import { formatCurrency, formatDate, formatDateTime } from "./utils.js";

export async function fetchSalesInRange(startDate, endDate) {
  const q = query(
    collection(db, "sales"),
    where("createdAt", ">=", Timestamp.fromDate(startDate)),
    where("createdAt", "<=", Timestamp.fromDate(endDate))
  );
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
}

export function computeReport(sales) {
  const active = sales.filter((s) => !s.cancelled);
  const cancelled = sales.filter((s) => s.cancelled);

  let totalRevenue = 0;
  let nakitTotal = 0, nakitCount = 0;
  let kartTotal = 0, kartCount = 0;
  const productMap = new Map(); // name -> { qty, revenue }

  active.forEach((sale) => {
    totalRevenue += sale.total || 0;
    if (sale.paymentMethod === "nakit") { nakitTotal += sale.total || 0; nakitCount++; }
    else if (sale.paymentMethod === "kart") { kartTotal += sale.total || 0; kartCount++; }

    (sale.items || []).forEach((item) => {
      const cur = productMap.get(item.name) || { qty: 0, revenue: 0 };
      cur.qty += item.qty;
      cur.revenue += item.qty * item.price;
      productMap.set(item.name, cur);
    });
  });

  const productBreakdown = Array.from(productMap.entries())
    .map(([name, v]) => ({ name, qty: v.qty, revenue: v.revenue }))
    .sort((a, b) => b.revenue - a.revenue);

  return {
    saleCount: active.length,
    cancelledCount: cancelled.length,
    totalRevenue,
    nakitTotal, nakitCount,
    kartTotal, kartCount,
    productBreakdown
  };
}

export function generateTxtReport(report, periodLabel, startDate, endDate) {
  const lines = [];
  const sep = "-----------------------------------------";
  lines.push("ÇAKIR BÜFE SATIŞ RAPORU");
  lines.push(`Dönem        : ${periodLabel} (${formatDate(startDate)} - ${formatDate(endDate)})`);
  lines.push(`Oluşturulma  : ${formatDateTime(new Date())}`);
  lines.push(sep);
  lines.push("GENEL ÖZET");
  lines.push(sep);
  lines.push(pad("Toplam Satış Adedi", report.saleCount));
  lines.push(pad("Toplam Ciro", formatCurrency(report.totalRevenue)));
  lines.push(pad("Nakit", `${formatCurrency(report.nakitTotal)} (${report.nakitCount} satış)`));
  lines.push(pad("Kart", `${formatCurrency(report.kartTotal)} (${report.kartCount} satış)`));
  lines.push(pad("İptal Edilen Satış", report.cancelledCount));
  lines.push(sep);
  lines.push("ÜRÜN BAZLI SATIŞ DÖKÜMÜ (cirosu en yüksekten en aza)");
  lines.push(sep);
  if (!report.productBreakdown.length) {
    lines.push("Bu dönemde satış kaydı bulunmuyor.");
  } else {
    report.productBreakdown.forEach((p, idx) => {
      lines.push(`${idx + 1}. ${p.name}`);
      lines.push(`    Adet : ${p.qty}     Tutar : ${formatCurrency(p.revenue)}`);
    });
  }
  lines.push(sep);
  lines.push("Bu rapor Çakır Büfe Yönetim Sistemi tarafından otomatik oluşturulmuştur.");
  return lines.join("\n");
}

function pad(label, value) {
  const l = label.padEnd(20, " ");
  return `${l}: ${value}`;
}
