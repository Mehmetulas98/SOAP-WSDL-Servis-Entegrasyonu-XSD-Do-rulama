 

Merhaba,
Melasoft Java Backend Developer pozisyonu için değerlendirme sorularımıza vermiş olduğunuz yanıtlar için teşekkür ederiz.
Başvurunuz değerlendirilmiş olup, sizi sürecimizin bir sonraki aşaması olan teknik task’a davet etmekten memnuniyet duyuyoruz.
Bu e-postaya ekli dosyada teknik task’a ait tüm gereksinimler ve teslimat talimatları yer almaktadır. Lütfen çalışmaya başlamadan önce dokümanı dikkatlice inceleyiniz.
Teslim Süresi: Bu e-postanın size ulaştığı andan itibaren 48 saat içerisinde tamamlamış olduğunuz çalışmayı bu e-postayı yanıtlayarak tarafımıza iletmenizi rica ederiz.
Değerlendirme sonrasında uygun bulunan adaylarla teknik mülakat planlanacaktır.
Süreç boyunca teknik task ile ilgili açıklamaya ihtiyaç duyduğunuz bir konu olursa bu e-posta üzerinden bizimle iletişime geçebilirsiniz.
İlginiz ve ayırdığınız zaman için teşekkür eder, çalışmanızda başarılar dileriz.
Saygılarımızla,
Melasoft HR

...

[İleti kısaltıldı]  Tüm iletiyi görüntüle
Bir ek
•  Gmail tarafından tarandı
# SOAP/WSDL Servis Entegrasyonu + XSD Doğrulama

**Pozisyon:** Java Developer (Mid / Senior)
**Süre:** 2 gün.
**Teslim:** Public Git repo (GitHub/GitLab) linki + `README.md`.
 
---

## Kısaca ne yapacaksın

Bir fatura kesilmeden önce, faturadaki tarafların **KDV numaralarını** resmi bir
AB servisinden (**VIES**) doğrulayan küçük bir Java uygulaması yaz. Servisle
**SOAP** üzerinden konuşacaksın ve veriyi servisin **XSD** şemasına göre
doğrulayacaksın.

Bu iş, günlük işimizde yaptığımız fatura entegrasyonlarının küçük bir örneği.
 
---

## Kullanacağın servis — VIES

VIES, bir AB KDV numarasının geçerli olup olmadığını söyler. Herkese açık,
kayıt/anahtar gerektirmez.

**Servis adresleri:**
```
WSDL     : https://ec.europa.eu/taxation_customs/vies/checkVatService.wsdl
Endpoint : https://ec.europa.eu/taxation_customs/vies/services/checkVatService
```

- Operasyon: `checkVat` — girdi: `countryCode` + `vatNumber`;
  cevap: `valid` (boolean), `name`, `address`, `requestDate`.
- XSD şeması WSDL dosyasının içinde gömülüdür (ayrı `.xsd` dosyası yok).
- Test için geçerli bir numara: `countryCode=DE`, `vatNumber=129273398`.

**Örnek istek (SOAP):**
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:urn="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
  <soapenv:Body>
    <urn:checkVat>
      <urn:countryCode>DE</urn:countryCode>
      <urn:vatNumber>129273398</urn:vatNumber>
    </urn:checkVat>
  </soapenv:Body>
</soapenv:Envelope>
```

**Örnek cevap:**
```xml
<checkVatResponse>
  <countryCode>DE</countryCode>
  <vatNumber>129273398</vatNumber>
  <valid>true</valid>
  <name>...</name>
  <address>...</address>
</checkVatResponse>
```
 
---

## Akış (adım adım)

1. Basit bir **fatura modeli** tut: fatura no + satıcı KDV no + alıcı KDV no.
   (İstersen kalem/tutar da ekle, zorunlu değil.)
2. Fatura kesilmeden önce, **iki KDV numarasını da** VIES `checkVat` ile sorgula.
3. Servisin cevabını **XSD'ye göre doğrula**, sonra sonucu oku (`valid` vb.).
4. Sonuca göre karar ver:
    - İki numara da geçerli → "fatura kesilebilir" (+ dönen ünvan/adresi rapora yaz).
    - Numaralardan biri geçersiz → "fatura kesilemez: <hangi taraf> KDV no geçersiz".
5. Sonucu ekrana/log'a anlamlı biçimde yaz. (İstersen küçük bir REST endpoint
   veya CLI ile dışarı aç — zorunlu değil.)

---

## Dikkat edilecek durumlar

Servis iki farklı "olumsuz" durum döndürür — **bunları ayırman beklenir:**

- **KDV no formatı doğru ama kayıtlı değil** → normal cevap gelir, içinde
  `valid=false`. (Bu bir hata değil, iş sonucudur.)
- **Geçersiz girdi** (ör. hatalı ülke kodu) → servis **SOAP Fault** döndürür
  (`INVALID_INPUT`). (Bu teknik bir hatadır, ayrı ele alınmalı.)

Ayrıca servis ara sıra yavaşlar veya `MS_UNAVAILABLE` gibi geçici hata
döndürebilir — timeout / tekrar deneme gibi durumları düşün.
 
---

## İstenenler

1. **SOAP entegrasyonu** — servise istek at, cevabı kullan. Endpoint, timeout
   gibi ayarlar kodda sabit (hard-coded) olmasın, config'ten gelsin.
2. **XSD doğrulama** — girdiyi ve/veya servis cevabını servisin XSD şemasına
   göre doğrula.
3. **Hata yönetimi** — yukarıdaki "Dikkat edilecek durumlar"ı ele al: fault,
   timeout, geçici erişilemezlik, geçersiz girdi.
4. **Test** — doğrulama akışı + hata durumları için test yaz.
5. **README** — nasıl build/çalıştırılır, verdiğin tasarım kararları ve nedenleri.

---

## Teknik

- Java 17+, Maven veya Gradle. `README`'deki komutlarla build + test çalışsın.
- Spring Boot serbest.
- Kendi seçtiğin kütüphane/yaklaşımı kullanabilirsin — seçimini README'de gerekçelendir.
- Eksik/belirsiz bir nokta olursa: bir varsayım yap, README'de belirt, devam et.

 