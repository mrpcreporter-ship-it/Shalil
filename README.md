# Shalil — نرم افزار شلیل

A Java-based Android shop and sales app with Persian RTL interface and local SQLite storage.

## Included in this version
- Persian dashboard using the Teal Harmony color palette
- Product create, edit, delete, stock, barcode text, purchase price and selling price
- Sales invoice cart, total calculation, invoice saving and stock deduction
- Sales history
- Rial / toman display preference
- Offline local database

## Build
The GitHub Actions workflow **Build Shalil APK** builds the debug APK on pushes to `main` or when manually started.

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Not yet included
Camera barcode scanning, Bluetooth barcode scanner pairing, Bluetooth receipt printing (58/80 mm), import/export backup, and date-range profit reports are not implemented in this Java migration yet.
