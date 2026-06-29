import {
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { auth } from "./firebase-init.js";
import { OWNER_LOGIN_USERNAME, OWNER_LOGIN_EMAIL } from "./firebase-config.js";

function normalize(str) {
  return String(str || "").trim().toLocaleLowerCase("tr-TR");
}

// Kullanıcı "çakır" yazsın ama Türkçe karakter girmese ("cakir") de giriş yapabilsin.
function usernameMatches(input) {
  const n = normalize(input);
  const target = normalize(OWNER_LOGIN_USERNAME);
  const targetAscii = target.replace("ç", "c").replace("ı", "i");
  return n === target || n === targetAscii;
}

export async function login(username, password) {
  if (!usernameMatches(username)) {
    const err = new Error("Kullanıcı adı veya şifre hatalı.");
    err.code = "auth/invalid-credential";
    throw err;
  }
  const pass = String(password || "").trim();
  await signInWithEmailAndPassword(auth, OWNER_LOGIN_EMAIL, pass);
}

export function logout() {
  return signOut(auth);
}

export function onAuthChange(callback) {
  return onAuthStateChanged(auth, callback);
}
