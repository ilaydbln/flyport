# FlightRez - Ucak Rezervasyon Sistemi

Spring Boot ile gelistirilmis demo ucak rezervasyon sistemi. Proje IntelliJ IDEA ile acilacak Maven projesi olarak hazirlandi.

## Ozellikler

- Ucus arama: nereden, nereye ve tarih filtresi
- Koltuk haritasi: business/economy koltuklar, bos/kilitli/dolu durumlari
- Rezervasyon: yolcu bilgisi ve mock odeme
- Iptal/iade: kalkis saatine gore iade orani
- Seat locking: ayni koltugun iki kisi tarafindan ayni anda alinmasini engelleyen kilit
- Rol sistemi: demo USER ve ADMIN kullanicilari

## Calistirma

1. IntelliJ IDEA ile bu klasoru acin: `/Users/ilaydabilen/Desktop/flightrez`
2. Maven dependency import islemini bekleyin.
3. `src/main/java/com/flightrez/FlightrezApplication.java` dosyasini run edin.
4. Tarayicida `http://localhost:8080` adresine gidin.

Alternatif olarak terminalden:

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" spring-boot:run
```

## Demo Kullanicilar

- `Demo User (USER)`: rezervasyon yapar, kendi rezervasyonlarini iptal eder.
- `Admin User (ADMIN)`: tum rezervasyonlari admin panelinde gorur.

## Mock Odeme

Kart numarasi 12 haneden uzun olmali ve `0000` ile bitmemeli. Ornek test karti:

```text
4111 1111 1111 1111
```

## Backend Notlari

Veriler uygulama icinde bellek uzerinde tutulur. `ReservationService` icinde `synchronized` transaction bolgesi kullanilir; koltuk kilitleme, odeme ve rezervasyon olusturma ayni kritik bolgede islenir. Bu yapi daha sonra JPA, H2/PostgreSQL ve Spring Security ile genisletilmeye hazirdir.
