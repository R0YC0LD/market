// Kamera ile barkod okuma - html5-qrcode kütüphanesi (index.html'de <script> ile yüklenir).
// Fiziksel barkod okuyucusu olmayan büfeler için telefon kamerasıyla tarama imkânı sağlar.

let scannerInstance = null;
let isRunning = false;

export async function startScanner(elementId, onDetected, onError) {
  if (typeof window.Html5Qrcode === "undefined") {
    onError?.(new Error("Kamera tarayıcı kütüphanesi yüklenemedi. İnternet bağlantınızı kontrol edin."));
    return;
  }
  if (isRunning) await stopScanner();

  scannerInstance = new window.Html5Qrcode(elementId);
  try {
    await scannerInstance.start(
      { facingMode: "environment" },
      { fps: 10, qrbox: { width: 230, height: 150 } },
      (decodedText) => {
        onDetected(decodedText);
      },
      () => { /* kare bazlı okuma hataları yok sayılır */ }
    );
    isRunning = true;
  } catch (err) {
    isRunning = false;
    onError?.(new Error("Kameraya erişilemedi. Kamera izni verildiğinden emin olun."));
  }
}

export async function stopScanner() {
  if (scannerInstance && isRunning) {
    try {
      await scannerInstance.stop();
      scannerInstance.clear();
    } catch (e) {
      // Tarayıcı zaten durmuş olabilir, güvenle yok sayılır.
    }
  }
  isRunning = false;
  scannerInstance = null;
}
