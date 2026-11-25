# Shalil — نرم افزار شلیل

A fresh Java Android shop-management project. The interface is Persian-first and right-to-left, with the Teal Harmony color palette.

## Implemented
- Local SQLite database; no account or internet connection required for normal use
- Product create, edit, delete, search, stock, barcode value, buying price and selling price
- Camera barcode scanning using the ZXing scanner flow
- Invoice cart, totals, saved invoice history, and stock deduction
- Daily, 7-day, and 30-day sales totals
- Rial/Toman display setting
- SQLite database backup and restore through Android's document picker
- Bluetooth paired-device selection and basic ESC/POS receipt output
- 58 mm / 80 mm paper-width preference

## Build APK
GitHub Actions runs on pushes to main. Open the Actions tab, choose the latest **Build Shalil APK** run, and download the **Shalil-debug-apk** artifact.

## Notes
Bluetooth printer compatibility varies by model and firmware. Test the selected printer with a sample receipt before using it for customer invoices. Camera scanning requires camera permission. Backups contain the local store database; keep them private.
