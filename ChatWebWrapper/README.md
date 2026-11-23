# Chat Web (pembungkus WhatsApp Web)
## Build otomatis (tanpa Android Studio)
1. Buat repo GitHub baru, unggah seluruh isi folder ini (termasuk folder .github).
2. Buka tab Actions > Build APK (jalan otomatis saat push) > tunggu hijau.
3. Unduh artifact "ChatWeb-debug-apk" > ekstrak > install app-debug.apk di HP.
## Build lokal
Android Studio: Open folder ini > Build > Build APK(s).
Atau: gradle assembleDebug  (hasil: app/build/outputs/apk/debug/app-debug.apk)
## Login
Di HP yang sama pilih "Tautkan dengan nomor telepon" (bukan QR).
