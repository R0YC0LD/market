# FUSE9 – Google Play Console yayın rehberi

Bu rehber, FUSE9'u Play Console'a eklemek için her formda ne seçeceğini sırayla anlatır.
Menü adları Play Console'un Türkçe arayüzüne göre yazıldı. Google bazen etiketleri değiştiriyor;
birebir aynı değilse en yakın seçeneği seç.

## 0. Elindeki dosyalar

| Dosya | Ne işe yarar |
|---|---|
| `app-release.aab` | Play'e yüklenecek, upload anahtarıyla imzalı paket (paket adı `com.r0yc0ld.fuse9`, sürüm 1.0.0, versionCode 1) |
| `fuse9-upload.jks` + `fuse9-upload-anahtar-bilgisi.txt` | Upload anahtarı ve şifresi. **Repoda yok; mutlaka güvenli bir yere yedekle.** |
| `play/listing/icon-512.png` | Uygulama simgesi (512×512) |
| `play/listing/<dil>/feature-graphic.png` | Öne çıkan grafik (1024×500) |
| `play/listing/<dil>/1…5-*.png` | Telefon ekran görüntüleri (1080×1920) |
| `play/listing/<dil>/title.txt`, `short_description.txt`, `full_description.txt` | Mağaza metinleri |
| `play/listing/release-notes.txt` | Sürüm notları (iki dilde, Play'in istediği etiketlerle) |
| `play/privacy-policy.html` | Gizlilik politikası sayfası |

`<dil>` = `tr-TR` (Türkçe) veya `en-US` (İngilizce).

## 1. Gizlilik politikasını yayına koy (önce bunu yap)

Play her uygulama için herkese açık bir gizlilik politikası URL'si istiyor.

1. `play/privacy-policy.html` iletişim adresi olarak `by599296@gmail.com` içeriyor; düzenlemen gerekmiyor.
2. Sayfayı herkese açık bir adrese koy. Seçenekler:
   - **GitHub Pages:** repo ayarlarında Pages'i aç; dosyayı yayınlanan dala koy.
     Adres `https://<kullanıcı-adın>.github.io/<repo>/…/privacy-policy.html` biçiminde olur.
   - **Google Sites:** yeni site aç, metni yapıştır, yayınla.
3. Adresi tarayıcıda gizli pencerede açıp göründüğünü kontrol et.

## 2. Uygulamayı oluştur

Ana Sayfa → **Uygulama oluştur**:

- Uygulama adı: `FUSE9`
- Varsayılan dil: **Türkçe – tr-TR**
- Uygulama mı, oyun mu: **Oyun**
- Ücretsiz mi, ücretli mi: **Ücretsiz**
- Beyanlar: Geliştirici Program Politikaları ve ABD ihracat yasaları kutularını işaretle → **Uygulama oluştur**

## 3. "Uygulamanızı ayarlayın" görevleri

Kontrol panelinde bu görevler listelenir. Her birini aşağıdaki cevaplarla doldur.

### Gizlilik politikası
1. adımdaki URL'yi yapıştır.

### Uygulama erişimi
**Tüm işlevler özel erişim gerektirmeden kullanılabilir.** (Giriş ekranı yok.)

### Reklamlar
**Hayır, uygulamamda reklam yok.**

### İçerik derecelendirmesi
- E-posta: `by599296@gmail.com`
- Kategori: **Oyun** (bulmaca)
- Şiddet, korku, cinsellik, kumar, uyuşturucu, küfür: hepsine **Hayır**
- Kullanıcılar arası iletişim veya içerik paylaşımı: **Hayır**
- Konum paylaşımı: **Hayır**
- Dijital satın alma: **Hayır**
- Beklenen sonuç: en düşük yaş derecesi (PEGI 3 / Everyone / USK 0 gibi)

### Hedef kitle ve içerik
- Hedef yaş grupları: **13–15, 16–17, 18 ve üzeri**.
  13 yaş altını seçmek uygulamayı "Aileler" politikasına sokar ve ek şartlar getirir. İlk sürüm için seçme.
- Uygulama çocukların ilgisini çekebilir mi: **Hayır**

### Veri güvenliği
- Uygulamanız gerekli kullanıcı verisi türlerinden herhangi birini topluyor veya paylaşıyor mu? **Hayır**
- Bu yüzden şifreleme ve silme sorularını cevaplaman gerekmez.
- Neden doğru: FUSE9'un internet izni yok. Kayıt, istatistik ve ayarlar yalnızca cihazda tutuluyor.

### Devlet uygulaması
**Hayır**

### Finansal özellikler
**Uygulamam finansal özellik sunmuyor**

### Sağlık
**Uygulamam sağlık özelliği sunmuyor**

### Haber uygulaması
Sorulursa **Hayır**

### Uygulama kategorisi ve iletişim bilgileri
Ayarla → Mağaza ayarları:
- Uygulama/oyun: **Oyun**
- Kategori: **Bulmaca**
- Etiketler (isteğe bağlı): Bulmaca, Mantık, Sudoku
- E-posta: `by599296@gmail.com` (mağazada herkese açık görünür). Web sitesi ve telefon isteğe bağlı.

### Ana mağaza girişi (Türkçe)
Büyüt → Mağaza girişi → **Ana mağaza girişi**:
- Uygulama adı: `tr-TR/title.txt`
- Kısa açıklama: `tr-TR/short_description.txt`
- Tam açıklama: `tr-TR/full_description.txt`
- Uygulama simgesi: `icon-512.png`
- Öne çıkan grafik: `tr-TR/feature-graphic.png`
- Telefon ekran görüntüleri: `tr-TR/1-first-board.png` … `5-rules.png` (5 adet, bu sırayla)
- Tablet görüntüleri zorunlu değil, boş bırakabilirsin.
- **Kaydet**

### İngilizce çeviri
Aynı sayfada **Çevirileri yönet → Kendi çevirilerimi ekle → English (United States) – en-US**.
`en-US/` klasöründeki metin ve görselleri yükle.

## 4. Kapalı test sürümü

> **Önemli:** 13 Kasım 2023'ten sonra açılmış **kişisel** geliştirici hesaplarında, Üretim
> (herkese açık) sürüme geçmeden önce en az **12 test kullanıcısıyla 14 gün kesintisiz kapalı
> test** zorunlu. Diğer iki uygulaman da "Kapalı test" aşamasında, yani bu kural muhtemelen
> sana da uygulanıyor. Bu yüzden ilk adım kapalı test.

1. Test edin ve yayınlayın → Test → **Kapalı test** → kanal oluştur (ör. "alpha") veya varsayılan kanalı kullan.
2. **Test kullanıcıları**: e-posta listesi oluştur veya Google Grubu ekle. En az 12 kişi olsun; tercihen 14 gün dolana kadar ekstra birkaç kişi daha ekle, çünkü kimi kişiler düşebiliyor. Geri bildirim e-postası: `by599296@gmail.com`.
3. **Ülkeler/bölgeler**: hepsi (veya istediklerin).
4. **Yeni sürüm oluştur**:
   - Play Uygulama İmzalama sorulursa **Google tarafından oluşturulan anahtarı kullan**'ı kabul et. Uygulamayı Google imzalar; senin `fuse9-upload.jks` dosyan yalnızca yükleme anahtarıdır.
   - App bundle yükle: `app-release.aab`
   - Sürüm adı: `1.0.0 (1)`
   - Sürüm notları: `release-notes.txt` içindeki metni olduğu gibi yapıştır
   - **Sonraki** → uyarıları oku → **Kaydet** → **İncelemeye gönder**
5. Yayın özeti sayfasında değişiklikleri incelemeye gönder. İnceleme birkaç saatten birkaç güne kadar sürebilir.
6. Onaydan sonra test kanalının **katılım bağlantısını** test kullanıcılarına gönder. Her biri bağlantıdan katılıp uygulamayı Play'den indirmeli.

## 5. Üretim (herkese açık) yayını

1. 12+ test kullanıcısı 14 gün boyunca katılı kaldıktan sonra Kontrol paneli → **Üretim erişimine başvur**.
2. Formda test sürecini kısaca anlat: kaç kişi test etti, hangi geri bildirimleri aldın, ne düzelttin.
3. Onay gelince: Üretim → **Yeni sürüm oluştur** → kapalı testteki sürümü **kitaplıktan ekle** → sürüm notları → **İncelemeye gönder**.
4. İstersen kademeli yayın kullan (ör. önce %20), sonra %100'e çıkar.

## 6. "Android geliştirici doğrulaması" sekmesi

Hesabında bu sekme duruyor. Google 2026'dan itibaren uygulamaları doğrulanmış geliştirici
kimliğine bağlıyor. O sekmede bekleyen bir görev varsa (kimlik/paket adı kaydı), FUSE9'u
göndermeden önce tamamla. Paket adı `com.r0yc0ld.fuse9`.

## 7. Sonraki güncellemeler

1. `fuse9/app/build.gradle.kts` içinde `versionCode`'u 1 artır (2, 3, …). `versionName`'i de güncelle (1.0.1 gibi).
2. Upload anahtarını kullanmak için `fuse9/keystore.properties` dosyasını oluştur (örneği `keystore.properties.example`).
3. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
4. Play Console'da ilgili kanalda yeni sürüm oluşturup yükle.

## 8. Anahtar güvenliği

- `fuse9-upload.jks` dosyasını ve şifresini iki ayrı güvenli yerde yedekle (ör. şifre yöneticisi + çevrimdışı disk).
- Anahtar kaybolursa uygulama kaybolmaz: Play Uygulama İmzalama sayesinde Play Console → Uygulama bütünlüğü üzerinden **yükleme anahtarı sıfırlama** isteği gönderebilirsin.
- Bu dosyaları asla repoya koyma; `.gitignore` zaten engelliyor.
