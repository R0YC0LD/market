// ============================================================
// FIREBASE YAPILANDIRMASI
// ============================================================
// Aşağıdaki bilgileri kendi Firebase projenizden almanız gerekiyor.
// Nasıl alınacağı README.md dosyasında adım adım anlatılmıştır.
//
// Firebase Console > Proje Ayarları > Genel > "Uygulamalarınız" bölümünden
// Web uygulaması eklediğinizde size verilen "firebaseConfig" nesnesini
// AŞAĞIDAKİ DEĞERLERİN ÜZERİNE YAPIŞTIRIN.
// ============================================================

export const firebaseConfig = {
  apiKey: "AIzaSyBXdvBEw5saI4JfHep9z55zpOt7kwHe64g",
  authDomain: "otokarnem-7b6b2.firebaseapp.com",
  projectId: "otokarnem-7b6b2",
  storageBucket: "otokarnem-7b6b2.firebasestorage.app",
  messagingSenderId: "631308252263",
  appId: "1:631308252263:web:6538c66da10a01607fd0c2",
  measurementId: "G-DCDL33HQ0M"
};

// Giriş ekranındaki "Kullanıcı Adı" alanı bu sabit e-postaya eşlenir.
// Firebase Authentication > Users kısmında BU e-posta ile bir kullanıcı
// oluşturmanız ve şifresini "çakırsuluova" olarak ayarlamanız gerekir.
// Detaylar README.md içindedir.
export const OWNER_LOGIN_USERNAME = "çakır";
export const OWNER_LOGIN_EMAIL = "cakir@cakirbufe.com";
