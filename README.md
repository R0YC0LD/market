# Çakır Büfe – Yönetim Sistemi

Çakır Büfe için hazırlanmış, telefon/tablet/bilgisayardan kullanılabilen, web tabanlı stok ve satış takip sistemi. Veriler Firebase (Google) üzerinde saklanır, site GitHub Pages üzerinden ücretsiz olarak yayınlanır.

> **Önemli not:** Bu sistem gerçek ödeme tahsilatı yapmaz (kredi kartı çekmez, banka ile konuşmaz). Kart ödemesi POS cihazından, nakit ödeme elden alınır; sistem sadece "bu satışta hangi yöntemle ödendi" bilgisini kayda geçirir ve stoğu otomatik düşer. Bu, sizinle yapılan konuşmadaki talebe uygundur.

---

## 1. Özellikler

- 📱 Telefon, tablet ve bilgisayarda otomatik uyumlu (responsive) arayüz
- 🔐 Kullanıcı adı/şifre ile giriş (Kullanıcı adı: `çakır`)
- 📦 Ürün yönetimi: ad, fotoğraf, fiyat, stok adedi, barkod
- 🔍 Barkod okutma: fiziksel barkod okuyucu **veya telefon kamerası** ile tarama
- ⌨️ Barkod okuyucu yoksa: ürün adıyla arama veya barkod numarasını elle yazma
- 🛒 Sepetli satış ekranı, ödeme yöntemi seçimi (Nakit / Kart)
- 📉 Satış yapılınca stok otomatik düşer (çoklu cihazda eşzamanlı, güvenli)
- ↩️ Satış iptali: iptal edilen satışın stoğu otomatik geri yüklenir
- ➕➖ Stok girişi/çıkışı (yeni sevkiyat, fire/bozulma, sayım düzeltmesi vb.)
- 🧾 Satış geçmişi (tarihe göre görüntüleme ve iptal)
- 📊 Günlük / haftalık / aylık satış raporu (ürün bazlı kırılım, en çok satan)
- ⬇️ Raporu **.txt** dosyası olarak indirme
- 🔄 Birden fazla cihazdan eşzamanlı kullanım (gerçek zamanlı senkronizasyon)

---

## 2. Kurulum – Firebase

Veriler Firebase'de (Google'ın ücretsiz bulut veritabanı hizmeti) saklanır. Aşağıdaki adımları **bir kere** yapmanız yeterli.

### 2.1. Firebase projesi oluşturma

1. [https://console.firebase.google.com](https://console.firebase.google.com) adresine gidip Google hesabınızla giriş yapın.
2. **"Proje ekle" / "Add project"** butonuna basın.
3. Proje adı olarak örneğin `cakir-bufe` yazın, devam edin (Google Analytics'i kapatabilirsiniz, gerekli değil).
4. Proje oluşturulduktan sonra ana panele gelirsiniz.

### 2.2. Web uygulaması yapılandırması

✅ Bu projede `js/firebase-config.js` dosyası **zaten dolduruldu** (`otokarnem-7b6b2` adlı Firebase projesinin bilgileriyle). Yeni bir Firebase projesi kullanmak isterseniz, Firebase Console'da **"</>"** (Web) simgesiyle yeni bir web uygulaması ekleyip size verilen `firebaseConfig` nesnesini `js/firebase-config.js` içine yapıştırmanız yeterlidir.

Sistem ayrıca **Firebase Analytics** ile otomatik olarak entegre edilmiştir (`js/firebase-init.js` içinde) — tarayıcı/reklam engelleyici desteklemediği durumlarda sessizce devre dışı kalır, uygulamanın çalışmasını etkilemez.

### 2.3. Authentication (giriş) kurulumu

Sistemdeki "Kullanıcı Adı: çakır / Şifre: çakırsuluova" girişi, arka planda Firebase Authentication üzerinden gerçek ve güvenli bir e-posta/şifre girişine bağlanır. Bunun çalışması için **bir kullanıcı oluşturmanız** gerekir:

1. Sol menüden **Build > Authentication**'a girin, **"Get started"** deyin.
2. **Sign-in method** sekmesinden **"Email/Password"** sağlayıcısını etkinleştirin (Enable yapıp kaydedin).
3. **Users** sekmesine geçin, **"Add user"** butonuna basın.
4. E-posta olarak tam olarak şunu yazın: `cakir@cakirbufe.com`
5. Şifre olarak tam olarak şunu yazın: `çakırsuluova`
6. **"Add user"** ile kaydedin.

> Not: `js/firebase-config.js` içindeki `OWNER_LOGIN_EMAIL` değeri ile buradaki e-posta birebir aynı olmalı. Değiştirmediyseniz zaten `cakir@cakirbufe.com` olarak ayarlıdır.

### 2.4. Firestore Database kurulumu

1. Sol menüden **Build > Firestore Database**'e girin, **"Create database"** deyin.
2. Konum (location) seçin (örn. `eur3 (europe-west)`), **"Production mode"** (üretim modu) ile devam edin.
3. Veritabanı oluşturulduktan sonra üstteki **"Rules"** sekmesine geçin.
4. Bu projedeki **`firestore-kurallari.txt`** dosyasını açın, içindeki "KURAL METNİ" bölümünü kopyalayıp oradaki kutuya **yapıştırın** ve **"Publish"** butonuna basın.

Bu kurallar sayesinde verilere sadece giriş yapmış kullanıcı (büfe sahibi) ulaşabilir; site herkese açık olsa da başkası verilerinizi okuyup değiştiremez.

### 2.5. Storage kuralları (isteğe bağlı)

Sistem ürün fotoğraflarını doğrudan Firestore'da sakladığı için **Firebase Storage kurmanız gerekmez**. İleride Storage'ı etkinleştirirseniz, **`storage-kurallari.txt`** dosyasındaki kuralları Build > Storage > Rules sekmesine aynı şekilde yapıştırabilirsiniz. Detaylar dosyanın içinde anlatılmıştır.

---

## 3. GitHub Pages'e Yayınlama

1. [github.com](https://github.com) üzerinde yeni bir repo (depo) oluşturun (örn. `cakir-bufe`). **Public** olmalı (GitHub Pages ücretsiz kullanım için).
2. Bu `market sistemi` klasörünün **içindeki tüm dosya ve klasörleri** (index.html, css, js, README.md, firestore-kurallari.txt, storage-kurallari.txt vb.) GitHub'daki repo sayfasına sürükleyip bırakın ("Add file > Upload files").
3. "Commit changes" ile onaylayın.
4. Repo içinde **Settings > Pages**'e gidin.
5. "Branch" kısmından `main` (veya `master`) dalını ve `/ (root)` klasörünü seçip **Save** deyin.
6. Birkaç dakika içinde size bir adres verilecek (örn. `https://kullaniciadiniz.github.io/cakir-bufe/`). Bu adres, sitenizin canlı adresidir.

> Telefonunuzun ana ekranına kısayol eklemek isterseniz: siteyi telefon tarayıcısında açıp "Ana ekrana ekle" seçeneğini kullanabilirsiniz, böylece bir uygulama gibi açılır.

---

## 4. Giriş Bilgileri

| Alan | Değer |
|---|---|
| Kullanıcı Adı | `çakır` |
| Şifre | `çakırsuluova` |

Bu bilgiler sadece **2.3. Authentication kurulumu** adımı doğru yapıldıktan sonra çalışır.

---

## 5. Kullanım Kılavuzu

### Satış Ekranı
- Üstteki kutuya barkod okuyucu ile barkod okutun (otomatik sepete eklenir) veya 📷 simgesine basıp telefon kamerasıyla barkodu tarayın.
- Barkod okuyucunuz yoksa, ürün arama kutusundan ürün adını yazıp aşağıdaki listeden ürüne dokunun.
- Sepetteki adetleri +/- ile değiştirebilir, ürünü silebilirsiniz.
- Ödeme yöntemini (Nakit/Kart) seçip **"Satışı Tamamla"** butonuna basın. Stok otomatik olarak düşer.

### Stok Ekranı
- **"+ Yeni Ürün"** ile ürün adı, fotoğrafı, fiyatı, stok adedi ve barkodunu kaydedin.
- Bir ürüne dokunarak bilgilerini düzenleyebilir veya silebilirsiniz.
- **"±"** butonuyla stok girişi (yeni sevkiyat) veya çıkışı (fire/bozulma, sayım düzeltmesi) yapabilirsiniz.
- Kırmızı/az gösterilen stok rozeti, ürünün azaldığını (5 adet ve altı) gösterir.

### Geçmiş Ekranı
- Tarih seçerek o günün satışlarını görebilirsiniz.
- Hatalı girilen bir satışı **"İptal Et"** ile geri alabilirsiniz; ürünlerin stoğu otomatik olarak geri yüklenir.

### Rapor Ekranı
- Günlük / Haftalık / Aylık sekmelerinden dönem seçin.
- Toplam ciro, satış adedi, nakit/kart dağılımı ve ürün bazlı satış dökümünü görün.
- **"TXT Olarak İndir"** ile raporu dosya olarak bilgisayarınıza/telefonunuza kaydedin.

---

## 6. Sık Sorulan Sorular

**Giriş yapamıyorum, "Kullanıcı adı veya şifre hatalı" diyor.**
Firebase Authentication kısmında `cakir@cakirbufe.com` e-postalı kullanıcıyı doğru şifre (`çakırsuluova`) ile oluşturduğunuzdan emin olun (Bölüm 2.3).

**Ürün eklerken/satış yaparken "yetkiniz yok" hatası alıyorum.**
`firestore-kurallari.txt` içindeki kuralları Firebase Console'a yapıştırıp **Publish** yapmadıysanız bu hatayı alırsınız (Bölüm 2.4).

**Kamera ile barkod tarama çalışmıyor.**
Tarayıcının kamera izni istediğinde "İzin Ver" demeniz gerekir. Site `http://` (şifresiz) değil, `https://` (GitHub Pages otomatik sağlar) adresinden açılmalıdır; aksi halde kamera izni tarayıcı tarafından engellenir.

**Aynı anda iki telefondan/tabletten kullanabilir miyiz?**
Evet. Tüm cihazlar aynı Firebase veritabanını kullandığı için stok ve satışlar anlık olarak senkronize olur.

---

## 7. Güvenlik Notları

- `js/firebase-config.js` içindeki bilgiler herkesin görebileceği şekilde sitede durur; bu **normaldir**, Firebase web uygulamalarında beklenen bir durumdur. Asıl güvenlik, Firebase Console'a yapıştırdığınız **Firestore Rules** (`firestore-kurallari.txt`) ile sağlanır (sadece giriş yapan kullanıcı veriye erişebilir).
- Giriş şifresini kimseyle paylaşmayın. Şifreyi değiştirmek isterseniz Firebase Console > Authentication > Users kısmından ilgili kullanıcının şifresini sıfırlayabilirsiniz.
- Ürün fotoğrafları, dosya boyutunu küçük tutmak için otomatik olarak sıkıştırılıp veritabanına kaydedilir; ayrıca bir depolama (Storage) servisi kurmanıza gerek yoktur.

---

## 8. Dosya Yapısı

```
market sistemi/
├── index.html              Tüm ekranların HTML yapısı
├── firestore-kurallari.txt Firestore güvenlik kuralları (Console'a yapıştırılacak)
├── storage-kurallari.txt   Storage güvenlik kuralları (isteğe bağlı)
├── README.md               Bu kılavuz
├── css/
│   └── style.css           Tüm görsel tasarım
└── js/
    ├── firebase-config.js  Firebase bağlantı bilgileri (dolduruldu)
    ├── firebase-init.js    Firebase başlatma + Analytics
    ├── auth.js              Giriş/çıkış işlemleri
    ├── products.js          Ürün ekleme/düzenleme/stok
    ├── sales.js              Sepet ve satış tamamlama
    ├── history.js            Satış geçmişi ve iptal
    ├── reports.js            Rapor hesaplama ve TXT oluşturma
    ├── scanner.js            Kamera ile barkod tarama
    ├── utils.js              Ortak yardımcı fonksiyonlar
    └── app.js                Tüm ekranları birbirine bağlayan ana dosya
```

---

## 9. Ek/Çıkarım Talepleri

Eklenmesi veya çıkarılması istenen bir özellik olursa (örn. çoklu kullanıcı, KDV hesaplama, tedarikçi takibi, e-posta ile günlük rapor gönderimi vb.) belirtmeniz yeterli; sistem bu yapı üzerine genişletilebilir şekilde tasarlanmıştır.
