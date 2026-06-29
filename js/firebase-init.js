// Firebase modüler SDK - CDN üzerinden yüklenir (build aracı gerekmez).
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getAuth,
  setPersistence,
  browserLocalPersistence
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";
import {
  getAnalytics,
  isSupported as analyticsIsSupported
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-analytics.js";
import { firebaseConfig } from "./firebase-config.js";

export const firebaseApp = initializeApp(firebaseConfig);
export const auth = getAuth(firebaseApp);

// Analytics isteğe bağlıdır: reklam engelleyici veya desteklenmeyen ortamlarda
// sessizce devre dışı kalır, uygulamanın çalışmasını engellemez.
export let analytics = null;
analyticsIsSupported()
  .then((supported) => { if (supported) analytics = getAnalytics(firebaseApp); })
  .catch(() => {});

// Çevrimdışı önbellek: bağlantı kesilse de uygulama son bilinen verilerle çalışmaya devam eder,
// birden fazla sekme/pencere açık olsa bile sorunsuz çalışır.
export const db = initializeFirestore(firebaseApp, {
  localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() })
});

setPersistence(auth, browserLocalPersistence).catch(() => {});
