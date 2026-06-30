import {
  signInAnonymously,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { auth } from "./firebase-init.js";

export function autoLogin() {
  return signInAnonymously(auth);
}

export function onAuthChange(callback) {
  return onAuthStateChanged(auth, callback);
}
