# School Registry (Android, Kotlin + Compose + Room, offline)
## Build & run
1. Install Android Studio (Koala or newer). File > Open > this folder; let Gradle sync (it creates the Gradle wrapper).
2. Choose an emulator or a USB-debugging phone, press Run.
## Installable APK
Build > Build Bundle(s)/APK(s) > Build APK(s). File: app/build/outputs/apk/debug/app-debug.apk (copy to phone, allow "install unknown apps").
For a signed release: Build > Generate Signed App Bundle / APK > APK.
## User guide
- Register: Dashboard > Add. Fill required (*) fields, add photo, Save. Admission no. is automatic.
- Search/Filter/Sort: View. Type in the search box; use Class/Gender/Sort menus. Tap a card for the profile.
- Edit: Profile > Edit > Update. Delete asks for confirmation.
- Print slip: Profile > Slip PDF/Print; in the chooser pick a PDF viewer, then Save or Print there.
- Reports: class counts, class/all-student PDF lists, CSV export (opens in Excel).
- Backup/Restore: Settings (gear) > Backup saves a .json file (with photos); Restore adds records from it without deleting existing ones.
- Demo data: six sample students are added on first launch; remove in Settings.
