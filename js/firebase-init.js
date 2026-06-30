// Firebase modüler SDK - CDN üzerinden yüklenir (build aracı gerekmez).
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
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

export let analytics = null;
analyticsIsSupported()
  .then((supported) => { if (supported) analytics = getAnalytics(firebaseApp); })
  .catch(() => {});

export const db = initializeFirestore(firebaseApp, {
  localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() })
});
