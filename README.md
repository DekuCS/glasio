# Glassio – Deku Edition

Persönlich konfigurierter Android-TV-/Fire-TV-Launcher mit Glas-Design, Nuvio-Anbindung und aktuellen Film-, Serien- und Anime-Empfehlungen.

Dieser Fork basiert auf [Glassio von mohammadmrh12](https://github.com/mohammadmrh12/glassio), lizenziert unter **GPL-3.0**. Die ursprünglichen Copyright-Hinweise und die [Lizenz](LICENSE) bleiben erhalten.

## Download und Installation

Die fertige APK liegt unter [Releases](https://github.com/DekuCS/glassio/releases/latest). Eine universelle APK unterstützt Fire TV, Android TV und Google TV ab Android 8. Fehlende Dock-Apps werden übersprungen und lassen sich später separat installieren.

1. APK auf den Fire TV übertragen und installieren. Falls Android danach fragt, die Installation für die verwendete Übertragungs-App erlauben.
2. Glassio öffnen. Die vorkonfigurierten Werte sind sofort aktiv; im Setup können eigene Konten verbunden werden. „Later“ behält das vorhandene Design.
Im Setup gibt es eine optionale Seite **Apps installieren**: Nuvio Legacy und auf Fire TV zusätzlich FireStick Fixer. Die Buttons laden die aktuelle APK aus dem jeweiligen [Nuvio-Legacy-Projekt](https://github.com/iqui27/nuvio-native-legacy) bzw. [FireStick-Fixer-Projekt](https://github.com/Hornet34/firestick-fixer). Daneben stehen **Aerial Views 1.8.5** (fest) und **SmartTube Glasio** (jeweils das neueste Universal-APK aus dem [SmartTube-Glasio-Projekt](https://github.com/DekuCS/SmartTube-Glasio)) bereit. Bereits installierte Apps werden geöffnet. Android bestätigt die Installation; die Launcher-Auswahl erfolgt danach in FireStick Fixer. Zurück/Weiter bleibt auch ohne Download möglich.

3. In **Einstellungen → Verbindungen** eigene Nuvio-/Trakt-/Stremio-/Simkl-Konten verbinden. Persönliche Konten, Verlauf, private Add-on-URLs, Wetterort und ADB-Schlüssel sind nicht in der APK enthalten.
4. Home-Tasten-Übernahme und systemweiter Bildschirmschoner werden auf jedem Gerät separat eingerichtet. Die APK aktiviert keine fremde ADB-Verbindung und entfernt keinen vorhandenen Launcher.

## Installationsdefaults

Die vollständige Liste steht in [config/install-defaults.json](config/install-defaults.json). Sie wurde aus der eingerichteten Fire-TV-Konfiguration übernommen. Die Kotlin-Defaults in `AppleTvDesign.kt` gelten für Neuinstallationen, fehlende Einstellungen und die explizite Preset-Aktion. Updates erhalten bereits gespeicherte Änderungen.

- Individueller Theme-Mix: Dock und Menü **Glass is glass**, Icons **Liquid**, matte Einstellungsfläche. Drei auswählbare Themes: Glass still, Glass is glass, Liquid.
- Dock: Nuvio Legacy, SmartTube Glasio, Spotify, Glassio-Einstellungen. Kurzes OK auf Einstellungen öffnet Glassio; langes OK die nativen TV-Einstellungen.
- Karussell: **Neu & beliebt**, ausgewogene Film-/Serien-/Anime-Auswahl, Fade mit 800 ms, deutsche Beschreibungen, Continue Watching separat unter dem Dock. Neue TVs starten mit öffentlichen Cinemeta-Katalogen; persönliche Add-ons kommen über die eigenen Konten dazu. Ein TMDB-Fallback ergänzt leere Anime-Feeds.
- Bild-/Glaswerte: 480 px Glasdetail, 24 Blur, 0 Refraction, 1024 MB Bildcache, 1 vorbereitetes Bild, 2/3 Seitenauflösung. Leistungs-Presets bleiben unabhängig auswählbar.
- Bildschirmschoner: Apple Aerial, 1080p H.264, nach 180 Sekunden, Cache 4 GB, Übergänge und eingeblendete Informationen.
- Sprache Deutsch; Uhr und Datum sichtbar. Wetterort wird pro Gerät gewählt.

## Updates aus diesem Fork

QR-Code unter **Über** und die Update-Prüfung zeigen auf **DekuCS/glassio**. Die App prüft `releases/latest` nach dem Start und anschließend alle zwölf Stunden; „Nach Updates suchen“ prüft sofort. Heruntergeladen werden APK-Assets aus Releases dieses Forks. Installiert wird erst über die bestehende Update-Aktion. Die Signatur muss der vorherigen Installation entsprechen.

## Selbst bauen

Benötigt werden JDK 17 und Android SDK 36. Die Gradle-Wrapper-Dateien liegen im Repository.

In der nicht versionierten `local.properties` den SDK-Pfad und benötigte App-Konfiguration hinterlegen: `TRAKT_CLIENT_ID`, `NUVIO_ANON_KEY`, `TMDB_READ_ACCESS_TOKEN`, optional `SIMKL_CLIENT_ID`. Die veröffentlichte APK wird lokal mit der vorhandenen App-Konfiguration gebaut. CI enthält keine persönlichen Schlüssel und veröffentlicht keine automatisch signierten Updates.

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease
```

Die APK entsteht in `app/build/outputs/apk/release/`. Für kompatible Folge-Updates wird derselbe lokale Signierschlüssel in `release-signing/glassio.jks` benötigt. Er ist von Git ausgeschlossen; privat sichern. Optional können `GLASSIO_KEYSTORE_PASSWORD`, `GLASSIO_KEY_ALIAS` und `GLASSIO_KEY_PASSWORD` gesetzt werden. Ohne Distributionsschlüssel verwendet ein lokaler Build den lokalen Android-Debugschlüssel; solche Builds sind nicht automatisch kompatibel mit der veröffentlichten APK.

Für eine neue Veröffentlichung `versionCode` und `versionName` erhöhen, prüfen und die signierte APK als Release-Asset im eigenen Fork veröffentlichen. APKs aus CI dienen der Buildprüfung und nicht als signaturkompatibles Release.

## Prüfung

Die Defaults-, Migrations-, Katalog-, Dock- und Update-Quellen-Tests laufen mit `testDebugUnitTest`. Bestehende Nuvio-, Stremio-, Trakt- und Simkl-Integrationen, Bibliothek, Verlauf und Fernbedienungsnavigation bleiben erhalten. Ein zweiter physischer Fire TV wurde nicht getestet.
