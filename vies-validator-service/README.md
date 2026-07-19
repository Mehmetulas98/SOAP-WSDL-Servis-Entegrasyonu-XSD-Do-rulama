# VIES VAT Doğrulama Servisi

Satıcı ve alıcının KDV numaralarını resmi VIES servisi üzerinden SOAP ile doğrulayan Spring Boot uygulaması.

---

## Kullanılan Teknolojiler ve Tasarım Kararları

### Spring Boot 4.1.0 / Java 17

Java 17 ve Spring Boot 4.1 kullandım. Bu seçimleri yapmamın sebebi modern ve kurumsal mimarilerde mikro dönüşüm ve sıfırdan
yapılan mikroservis mimarilerinde bu ikili çoklukla kullanılmaktadır.

### jaxb2-maven-plugin — WSDL'den POJO Üretimi

VIES WSDL'i elle yazmak yerine compile aşamasında POJO sınıflarına dönüştürdüm. İleride servis ile ilgi
bir değişiklik olduğunda mvn compile ile proje aynı şekilde çalışmaya devam eder. Ayrıca servisin olası
değişimlerde bakım maliyetini de düşürür.

### XSD Doğrulama

WSDL içindeki `<wsdl:types>` bloğu ayrı bir `checkVatTypes.xsd` dosyasına çıkarttım.
`Jaxb2Marshaller.setSchemas()` ile bu şema marshaller'a bağladım.
Böylece VIES'ten dönen SOAP yanıtı, unmarshalling öncesinde şemaya göre doğrulanır.
`ValidationEventHandler` uyarıları loglar fakat işlemi durdurmaz.

### Spring Retry — Fault Tolerance

VIES servisi ara sıra `MS_UNAVAILABLE`, `TIMEOUT`, `MS_MAX_CONCURRENT_REQ` gibi geçici hatalar döndürürmekte.
Bu durumlarda isteği başarısızlık olarak işaretlemek yerine otomatik yeniden deneme yapılır. Bu konu için Resillence4j
kullanılabilirdi ama Spring Boot 4.1.0 da bazı konfigurasyon sorunları nedeniyle kullanmamaya karar verdim. Bunun yerine
Spring Boot ile yeni gelen Spring Retry mekanizmasını kullandım. Ayrıca hataları sınıflandırmak için 2 tane exception sınıfı
oluşturdum. Bir tanesi RetryableViesException yani geçici yeniden denenmesi gereken hata durumları, ViesServiceException ise yeniden
denenmesi gereksiz hatalar için. Ayrıca servisten 200 dönmesine rağmen SOAP fault kodu ile cevap dönüyor ve programın akışını bozuyor.
Hatalı body unmarshal edilmeye çalışıldığı için unmarshallingfailureexception'a yol açıyordu. Çözüm olarak exception fırlattım bu durumda
ve retry mekanizmasına geri besledim.
Exception bazında Retry yapılıp yapılmayacağı `@Retryable(retryFor = RetryableViesException.class)` ile belirlenir.
Hatalı input gibi hatalarda gereksiz yere retry yapmaya gerek kalmaz.

### Bucket4j — Rate Limiting

API'ye gelen istek sayısını kontrol altında tutmak için kullandım. Servisin kilitlenmesini ve gereksiz isteklerden korunmak için
uyguladım. Örnek olarak 60 saniyede 2 istek olarak gerçekleştirdim. Bu konfigurasyon kullanılacak yer ve duruma göre değiştirilebilir.

---

## Hata Yönetimi

| Durum                              | HTTP Kodu |
| ---------------------------------- | --------- |
| Geçersiz istek alanları            | 400       |
| VIES'ten `INVALID_INPUT` hatası    | 400       |
| VIES servisine ulaşılamıyor        | 503       |
| Geçici hata, tüm denemeler tükendi | 503       |
| İstek limiti aşıldı (rate limit)   | 429       |

---

## Nasıl Çalıştırılır

### 1) Yerel Ortamda

**Gereksinim:** Java JDK 17, Maven

```
Git reposu clonelanır. Proje dizininde aşağıdaki komutlar çalıştırılır.
mvn clean package          # derleme ve test yapmak için
mvn spring-boot:run        # projeyi çalıştırmak için
```

### 2) Docker ile (Tercih Edilen)

**Gereksinim:** Docker

```
Git reposu clonelanır. Proje dizininde aşağıdaki komutlar çalıştırılır.
docker compose up -d --build   # imajı derler ve arka planda başlatır
docker compose logs -f         # log akışını izler
```

Servis ayağa kalktıktan sonra sağlık durumunu actuator vasıtasıyla http://localhost:8080/actuator/health adresinden
bakılabilir. Garafana ve prometheus entegrasyonu
yapılarak projenin çalıştığı makine bilgileri vs gibi ek bilgiler de izlenebilir.

---

## Örnek Servis Çağrısı

**Endpoint:** `POST http://localhost:8080/api/invoices/validate`
Örnek çağırım request bilgisi aşağıdaki gibidir.

```json
{
  "invoiceNumber": "INV-2026-0001",
  "sellerCountryCode": "DE",
  "sellerVatNumber": "129273398",
  "buyerCountryCode": "FR",
  "buyerVatNumber": "10402571889"
}
```

**Başarılı yanıt**

```json
{
  "invoiceNumber": "INV-2026-0001",
  "invoiceIssuable": true,
  "message": "Fatura kesilebilir.",
  "sellerVat": {
    "countryCode": "DE",
    "vatNumber": "129273398",
    "valid": true,
    "name": "BUNDESZENTRALAMT FUER STEUERN",
    "address": "..."
  },
  "buyerVat": {
    "countryCode": "FR",
    "vatNumber": "10402571889",
    "valid": true,
    "name": "...",
    "address": "..."
  }
}
```

**KDV numarası geçersiz :**

```json
{
  "invoiceNumber": "INV-2026-0001",
  "invoiceIssuable": false,
  "message": "Fatura kesilemiyor: alıcı KDV numarası VIES'te kayıtlı değil.",
  "sellerVat": { "valid": true, ... },
  "buyerVat":  { "valid": false, ... }
}
```

**Rate limit aşıldı :**

```json
{
  "status": 429,
  "title": "İstek limiti aşıldı",
  "detail": "60 saniye içinde en fazla 2 istek gönderilebilir."
}
```

---

## Testleri Çalıştırma

```
Proje dinizinde "mvn test" komutu çalıştırılır.
```

3 tane test sınıfı var temel olarak.
ViesClientServiceTest(Retry ve çağrı yönetimi), InvoiceValidationServiceTest(iş mantığı testleri),
InvoiceControllerTest(rest katmanı için testler)

---

## Curl ile test Etme

```
curl -X POST http://localhost:8080/api/invoices/validate \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNumber": "INV-2026-0001",
    "sellerCountryCode": "DE",
    "sellerVatNumber": "129273398",
    "buyerCountryCode": "FR",
    "buyerVatNumber": "10402571889"
  }'
```

```
curl -X POST http://localhost:8080/api/invoices/validate \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceNumber": "INV-2026-0002",
    "sellerCountryCode": "XX",
    "sellerVatNumber": "129273398",
    "buyerCountryCode": "DE",
    "buyerVatNumber": "129273398"
  }'
```

## Test Verisi

Geçerli VIES numaraları için örnek dataları bu linkten bulunabilir.
https://viesapi.eu/test-api/
