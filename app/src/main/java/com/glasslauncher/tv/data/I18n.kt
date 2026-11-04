package com.glasslauncher.tv.data

/**
 * App language (System → Language). English is the source; the others cover the menu, the settings sections,
 * the first-run setup and the most used rows. Anything not translated yet stays in English.
 */
object I18n {
    @Volatile var lang: String = "en"

    /** Code → its own name, in the order the setting cycles through. */
    val LANGS = linkedMapOf(
        "en" to "English", "ar" to "العربية", "fa" to "فارسی", "tr" to "Türkçe", "fr" to "Français",
        "es" to "Español", "de" to "Deutsch", "ru" to "Русский", "pt" to "Português",
    )
    private val COLS = listOf("ar", "fa", "tr", "fr", "es", "de", "ru", "pt")

    fun tr(s: String): String {
        if (lang == "en" || s.isBlank()) return s
        val col = COLS.indexOf(lang).takeIf { it >= 0 } ?: return s
        TABLE[s]?.getOrNull(col)?.takeIf { it.isNotBlank() }?.let { return it }
        if (s.startsWith("  · ")) TABLE[s.removePrefix("  · ")]?.getOrNull(col)?.takeIf { it.isNotBlank() }?.let { return "  · $it" }
        return s
    }

    // English | Arabic | Persian | Turkish | French | Spanish | German | Russian | Portuguese
    private val TABLE: Map<String, List<String>> = """
Settings|الإعدادات|تنظیمات|Ayarlar|Réglages|Ajustes|Einstellungen|Настройки|Ajustes
Connections|الاتصالات|اتصال‌ها|Bağlantılar|Connexions|Conexiones|Verbindungen|Подключения|Conexões
Catalogs|الكتالوجات|کاتالوگ‌ها|Kataloglar|Catalogues|Catálogos|Kataloge|Каталоги|Catálogos
Continue Watching|متابعة المشاهدة|ادامه تماشا|İzlemeye devam et|Reprendre|Seguir viendo|Weiterschauen|Продолжить просмотр|Continuar assistindo
Library|المكتبة|کتابخانه|Kitaplık|Bibliothèque|Biblioteca|Mediathek|Библиотека|Biblioteca
Carousel|الكاروسيل|کاروسل|Karusel|Carrousel|Carrusel|Karussell|Карусель|Carrossel
Dock & icons|الشريط والأيقونات|داک و آیکون‌ها|Dock ve simgeler|Dock et icônes|Dock e iconos|Dock & Symbole|Док и значки|Dock e ícones
Menu & themes|القائمة والثيمات|منو و پوسته‌ها|Menü ve temalar|Menu et thèmes|Menú y temas|Menü & Designs|Меню и темы|Menu e temas
Clock & glass|الساعة والزجاج|ساعت و شیشه|Saat ve cam|Horloge et verre|Reloj y cristal|Uhr & Glas|Часы и стекло|Relógio e vidro
Screensaver|شاشة التوقف|محافظ صفحه|Ekran koruyucu|Économiseur d'écran|Salvapantallas|Bildschirmschoner|Заставка|Protetor de tela
Performance|الأداء|کارایی|Performans|Performances|Rendimiento|Leistung|Производительность|Desempenho
Manage from phone|الإدارة من الهاتف|مدیریت با گوشی|Telefondan yönet|Gérer depuis le téléphone|Gestionar desde el móvil|Vom Handy verwalten|Управление с телефона|Gerir pelo telemóvel
System|النظام|سیستم|Sistem|Système|Sistema|System|Система|Sistema
About|حول|درباره|Hakkında|À propos|Acerca de|Über|О программе|Sobre
Glassio settings|إعدادات Glassio|تنظیمات Glassio|Glassio ayarları|Réglages Glassio|Ajustes de Glassio|Glassio-Einstellungen|Настройки Glassio|Ajustes do Glassio
Android settings|إعدادات أندرويد|تنظیمات اندروید|Android ayarları|Réglages Android|Ajustes de Android|Android-Einstellungen|Настройки Android|Ajustes do Android
Themes|الثيمات|پوسته‌ها|Temalar|Thèmes|Temas|Designs|Темы|Temas
Camera|الكاميرا|دوربین|Kamera|Caméra|Cámara|Kamera|Камера|Câmera
Background|الخلفية|پس‌زمینه|Arka plan|Arrière-plan|Fondo|Hintergrund|Фон|Fundo
Wi-Fi|واي فاي|وای‌فای|Wi-Fi|Wi-Fi|Wi-Fi|WLAN|Wi-Fi|Wi-Fi
Apps|التطبيقات|برنامه‌ها|Uygulamalar|Apps|Apps|Apps|Приложения|Apps
Hello|مرحباً|سلام|Merhaba|Bonjour|Hola|Hallo|Привет|Olá
Let's connect and customise your Glassio|لنربط حساباتك ونخصص Glassio|بیایید Glassio را وصل و شخصی کنیم|Glassio'nu bağlayıp kişiselleştirelim|Connectons et personnalisons votre Glassio|Conectemos y personalicemos tu Glassio|Lass uns dein Glassio verbinden und anpassen|Давайте подключим и настроим Glassio|Vamos ligar e personalizar o seu Glassio
Install apps||||||Apps installieren||
Optional downloads. Confirm installation in Android, then set up the apps yourself.||||||Optionale Downloads. Installation in Android bestätigen, danach die Apps selbst einrichten.||
Download Aerial Views||||||Aerial Views herunterladen||
Open Aerial Views||||||Aerial Views öffnen||
Download SmartTube Glasio||||||SmartTube Glasio herunterladen||
Open SmartTube Glasio||||||SmartTube Glasio öffnen||
Download Nuvio Legacy||||||Nuvio Legacy herunterladen||
Open Nuvio Legacy||||||Nuvio Legacy öffnen||
Download FireStick Fixer||||||FireStick Fixer herunterladen||
Open FireStick Fixer||||||FireStick Fixer öffnen||
Get started|ابدأ|شروع|Başla|Commencer|Empezar|Los geht's|Начать|Começar
Restore a backup|استعادة نسخة احتياطية|بازیابی پشتیبان|Yedeği geri yükle|Restaurer une sauvegarde|Restaurar copia|Backup wiederherstellen|Восстановить копию|Restaurar cópia
Later|لاحقاً|بعداً|Sonra|Plus tard|Más tarde|Später|Позже|Mais tarde
Choose a look|اختر الشكل|ظاهر را انتخاب کنید|Bir görünüm seç|Choisissez un style|Elige un estilo|Wähle einen Look|Выберите вид|Escolha um visual
Connect|الربط|اتصال|Bağlan|Connecter|Conectar|Verbinden|Подключить|Ligar
Next|التالي|بعدی|İleri|Suivant|Siguiente|Weiter|Далее|Seguinte
Sign in with GitHub|تسجيل الدخول بـ GitHub|ورود با GitHub|GitHub ile giriş|Connexion avec GitHub|Iniciar sesión con GitHub|Mit GitHub anmelden|Вход через GitHub|Entrar com o GitHub
Synopsis lines|أسطر النص|خطوط توضیح|Özet satırları|Lignes du synopsis|Líneas de la sinopsis|Zeilen der Beschreibung|Строки описания|Linhas da sinopse
0 = the whole text|||0 = tüm metin|0 = tout le texte|0 = todo el texto|0 = der ganze Text|0 = весь текст|0 = todo o texto
You're all set|كل شي جاهز|همه چیز آماده است|Her şey hazır|Tout est prêt|Todo listo|Alles bereit|Всё готово|Tudo pronto
Start|ابدأ|شروع|Başla|Démarrer|Empezar|Start|Старт|Iniciar
Back|رجوع|بازگشت|Geri|Retour|Atrás|Zurück|Назад|Voltar
Done|تم|تمام|Bitti|Terminé|Listo|Fertig|Готово|Concluído
Saved|المحفوظة|ذخیره‌شده|Kaydedilenler|Enregistrés|Guardados|Gemerkt|Сохранённое|Guardados
Open apps|التطبيقات المفتوحة|برنامه‌های باز|Açık uygulamalar|Apps ouvertes|Apps abiertas|Offene Apps|Открытые приложения|Apps abertas
No apps are open|ماكو تطبيقات مفتوحة|هیچ برنامه‌ای باز نیست|Açık uygulama yok|Aucune app ouverte|No hay apps abiertas|Keine Apps offen|Нет открытых приложений|Nenhuma app aberta
Language|اللغة|زبان|Dil|Langue|Idioma|Sprache|Язык|Idioma
Theme|الثيم|پوسته|Tema|Thème|Tema|Design|Тема|Tema
Picture quality|جودة الصورة|کیفیت تصویر|Resim kalitesi|Qualité d'image|Calidad de imagen|Bildqualität|Качество изображения|Qualidade de imagem
Shuffle all catalogs|خلط كل الكتالوجات|ترکیب همه کاتالوگ‌ها|Tüm katalogları karıştır|Mélanger tous les catalogues|Mezclar todos los catálogos|Alle Kataloge mischen|Перемешать все каталоги|Misturar todos os catálogos
Surprise me|فاجئني|غافلگیرم کن|Beni şaşırt|Surprends-moi|Sorpréndeme|Überrasch mich|Удиви меня|Surpreende-me
White logo|شعار أبيض|لوگوی سفید|Beyaz logo|Logo blanc|Logo blanco|Weißes Logo|Белый логотип|Logótipo branco
Back up now|نسخ احتياطي الآن|پشتیبان‌گیری اکنون|Şimdi yedekle|Sauvegarder|Copiar ahora|Jetzt sichern|Создать копию|Fazer cópia agora
Restore backup|استعادة النسخة|بازیابی پشتیبان|Yedeği geri yükle|Restaurer|Restaurar copia|Wiederherstellen|Восстановить|Restaurar cópia
Hold Home|الضغط المطوّل على Home|نگه داشتن Home|Home'u basılı tut|Maintenir Home|Mantener Home|Home gedrückt halten|Удерживать Home|Manter Home
Glow|التوهج|درخشش|Parıltı|Halo|Brillo|Leuchten|Свечение|Brilho
Icon zoom on focus|تكبير الأيقونة عند التحديد|بزرگ‌نمایی آیکون|Seçilince büyüt|Zoom de l'icône|Zoom del icono|Symbol-Zoom|Увеличение значка|Zoom do ícone
Ask before removing|تأكيد قبل الحذف|پرسیدن قبل از حذف|Kaldırmadan önce sor|Demander avant de retirer|Preguntar antes de quitar|Vor dem Entfernen fragen|Спрашивать перед удалением|Perguntar antes de remover
Menu order|ترتيب القائمة|ترتیب منو|Menü sırası|Ordre du menu|Orden del menú|Menü-Reihenfolge|Порядок меню|Ordem do menu
Open Glassio when the TV starts|افتح Glassio عند تشغيل التلفزيون|باز کردن Glassio با روشن شدن تلویزیون|TV açılınca Glassio'yu aç|Ouvrir Glassio au démarrage|Abrir Glassio al encender|Glassio beim Start öffnen|Открывать Glassio при включении|Abrir o Glassio ao ligar
Run setup again|تشغيل الإعداد من جديد|اجرای دوباره راه‌اندازی|Kurulumu yeniden çalıştır|Relancer la configuration|Repetir la configuración|Einrichtung erneut|Повторить настройку|Repetir configuração
Check for updates|البحث عن تحديثات|بررسی به‌روزرسانی|Güncellemeleri denetle|Rechercher des mises à jour|Buscar actualizaciones|Nach Updates suchen|Проверить обновления|Procurar atualizações
CPU||||||CPU||
GPU||||||GPU||
RAM||||||RAM||
Storage||||||Speicher||
IP address||||||IP-Adresse||
Bar||||||Balken||
Card||||||Karte||
Card size||||||Kartengröße||
Glow size||||||Leuchtgröße||
Hide when complete||||||Ausblenden, wenn fertig||
New mix every||||||Neue Mischung alle||
Where it came from||||||Woher sie stammt||
+ button on the carousel||||||+-Taste im Karussell||
+ saves to Simkl too||||||+ speichert auch in Simkl||
1 · Open About (enable Developer options)||||||1 · Über öffnen (Entwickleroptionen einschalten)||
2 · Open Developer options||||||2 · Entwickleroptionen öffnen||
3 · Allow Glassio||||||3 · Glassio erlauben||
24-hour clock||||||24-Stunden-Uhr||
Add a remote button||||||Fernbedienungstaste hinzufügen||
Add add-on link||||||Add-on-Link hinzufügen||
Add add-ons from my phone (QR)||||||Add-ons vom Handy hinzufügen (QR)||
Add camera link||||||Kamera-Link hinzufügen||
Add video link||||||Video-Link hinzufügen||
Add-ons from my Nuvio account||||||Add-ons aus meinem Nuvio-Konto||
Android settings in the menu||||||Android-Einstellungen im Menü||
App at start-up||||||App beim Start||
Apply Apple TV defaults||||||Apple-TV-Standard anwenden||
Artwork||||||Bildmaterial||
Auto-hide clock and settings button||||||Uhr und Einstellungstaste automatisch ausblenden||
Auto-play||||||Automatisch abspielen||
Back on the home screen starts it||||||Zurück auf dem Startbildschirm startet ihn||
Backdrop gallery||||||Hintergrundgalerie||
Background in the menu||||||Hintergrund im Menü||
Bounce at the edges||||||An den Rändern federn||
Buttons||||||Tasten||
Camera in the menu||||||Kamera im Menü||
Catalogs from Stremio's add-ons||||||Kataloge aus Stremios Add-ons||
Catalogs from the Nuvio account's add-ons||||||Kataloge aus den Add-ons des Nuvio-Kontos||
Choose default home app||||||Standard-Start-App wählen||
City in the menu||||||Stadt im Menü||
Clear the list||||||Liste leeren||
Clock||||||Uhr||
Clock in the menu||||||Uhr im Menü||
Clock position||||||Position der Uhr||
Colour on focus||||||Farbe bei Fokus||
Connect Nuvio account||||||Nuvio-Konto verbinden||
Connect Simkl||||||Simkl verbinden||
Connect Stremio||||||Stremio verbinden||
Connect to Nuvio||||||Mit Nuvio verbinden||
Connect Trakt||||||Trakt verbinden||
Connect Trakt account||||||Trakt-Konto verbinden||
Continue Watching from Simkl||||||Weiterschauen aus Simkl||
Continue Watching from Stremio||||||Weiterschauen aus Stremio||
Continue Watching from the Nuvio account||||||Weiterschauen aus dem Nuvio-Konto||
Continue Watching from Trakt||||||Weiterschauen aus Trakt||
Continue Watching in the Library||||||Weiterschauen in der Mediathek||
Corner radius||||||Eckenradius||
Date||||||Datum||
Date in the menu||||||Datum im Menü||
DekuCS / Glassio on GitHub||||||DekuCS / Glassio auf GitHub||
Delete saved videos||||||Gespeicherte Videos löschen||
Dock background||||||Hintergrund des Docks||
Dock fits its apps||||||Dock passt sich den Apps an||
Dock position||||||Position des Docks||
Dock size||||||Größe des Docks||
Dots auto-hide||||||Punkte automatisch ausblenden||
Dots per group||||||Punkte pro Gruppe||
Dots style||||||Stil der Punkte||
Down from the dock opens the Library||||||Runter vom Dock öffnet die Mediathek||
Fade between videos||||||Überblenden zwischen Videos||
Fahrenheit||||||Fahrenheit||
Find cameras||||||Kameras suchen||
Find icon packs on Google Play||||||Icon-Pakete bei Google Play finden||
Genres line||||||Genre-Zeile||
Genres size||||||Größe der Genres||
Glass blur||||||Glas-Unschärfe||
Glass brightness||||||Glas-Helligkeit||
Glass detail||||||Glas-Detail||
Glass refraction||||||Glas-Brechung||
Glassio is the TV screensaver (ADB)||||||Glassio ist der Bildschirmschoner (ADB)||
Hide the dock||||||Dock ausblenden||
Home Assistant webhook||||||Home-Assistant-Webhook||
Home button opens Glassio||||||Home-Taste öffnet Glassio||
Icon pack||||||Icon-Paket||
Icon style||||||Icon-Stil||
Import icons from the icon folder||||||Icons aus dem Icon-Ordner importieren||
Keep the page on||||||Seite eingeschaltet lassen||
Keep videos on the TV||||||Videos auf dem TV behalten||
Last crash||||||Letzter Absturz||
Library in the menu||||||Mediathek im Menü||
Lift selected icon||||||Ausgewähltes Icon anheben||
Logo / title size||||||Größe von Logo / Titel||
Menu||||||Menü||
Menu look||||||Aussehen des Menüs||
Menu size||||||Größe des Menüs||
Mode||||||Modus||
My videos||||||Meine Videos||
Nuvio account||||||Nuvio-Konto||
Nuvio Legacy||||||Nuvio Legacy||
Nuvio library||||||Nuvio-Mediathek||
Open an app when the TV starts||||||Eine App starten, wenn der TV angeht||
Open on what I just watched||||||Öffnen bei dem, was ich zuletzt gesehen habe||
Open titles in||||||Titel öffnen in||
Picture cache||||||Bild-Cache||
Picture from a link||||||Bild aus einem Link||
Picture from the folder||||||Bild aus dem Ordner||
Pictures per title||||||Bilder pro Titel||
Place||||||Ort||
Play one video only||||||Nur ein Video abspielen||
Position||||||Position||
Poster size||||||Größe der Poster||
Posters in a row||||||Poster pro Reihe||
Prepare ahead||||||Vorher vorbereiten||
Preview now||||||Jetzt ansehen||
Progress bar||||||Fortschrittsbalken||
Quality||||||Qualität||
Rating, year, platform||||||Bewertung, Jahr, Plattform||
Rating, year, platform size||||||Größe von Bewertung, Jahr, Plattform||
Redirect an app button||||||App-Taste umleiten||
Refresh catalogs||||||Kataloge neu laden||
Reload carousel now||||||Karussell jetzt neu laden||
Reset pictures picked per title||||||Gewählte Bilder pro Titel zurücksetzen||
Save to the Nuvio library too||||||Auch in der Nuvio-Mediathek speichern||
Save to the Trakt watchlist too||||||Auch in der Trakt-Merkliste speichern||
Screensaver in the menu||||||Bildschirmschoner im Menü||
Select all catalogs||||||Alle Kataloge auswählen||
Select none||||||Keine auswählen||
Selected icon plate||||||Platte des ausgewählten Icons||
Send a test event||||||Testereignis senden||
Show Apps folder||||||Apps-Ordner anzeigen||
Show clock||||||Uhr anzeigen||
Show Continue Watching||||||Weiterschauen anzeigen||
Show date||||||Datum anzeigen||
Show phone apps too||||||Auch Handy-Apps anzeigen||
Shown at start for||||||Beim Start anzeigen für||
Shows for||||||Anzeigen für||
Shuffle||||||Mischen||
Sign in with email||||||Mit E-Mail anmelden||
Sign out of Trakt||||||Von Trakt abmelden||
Simkl account||||||Simkl-Konto||
Size||||||Größe||
Slide animation||||||Übergang||
Slide time||||||Anzeigedauer||
Slide transition||||||Übergang||
Slides at the start||||||Folien am Anfang||
Space between posters||||||Abstand zwischen Postern||
Space for saved videos||||||Platz für gespeicherte Videos||
Start after (on the home screen)||||||Startet nach (auf dem Startbildschirm)||
Stremio account||||||Stremio-Konto||
Style||||||Stil||
Synopsis||||||Beschreibung||
Synopsis language||||||Sprache der Beschreibung||
Synopsis only when the carousel is selected||||||Beschreibung nur, wenn das Karussell gewählt ist||
Synopsis size||||||Größe der Beschreibung||
Tell Home Assistant when a title opens||||||Home Assistant melden, wenn ein Titel geöffnet wird||
Themes in the menu||||||Designs im Menü||
Title style||||||Titel-Stil||
Titles per catalog||||||Titel pro Katalog||
Titles under posters||||||Titel unter den Postern||
TMDB token||||||TMDB-Token||
Total titles||||||Titel gesamt||
Trakt account||||||Trakt-Konto||
Trakt watchlist in the Library||||||Trakt-Merkliste in der Mediathek||
Transition speed||||||Geschwindigkeit des Übergangs||
Turn it on with ADB||||||Mit ADB einschalten||
Turn on the accessibility service||||||Bedienungshilfe einschalten||
TV info||||||TV-Infos||
TV info in the menu||||||TV-Infos im Menü||
TV screensaver starts after (ADB)||||||TV-Bildschirmschoner startet nach (ADB)||
Type||||||Typ||
Video from a link||||||Video aus einem Link||
Video from the folder||||||Video aus dem Ordner||
Weather||||||Wetter||
Weather city||||||Wetter-Stadt||
Weather in the menu||||||Wetter im Menü||
Wi-Fi in the menu||||||WLAN im Menü||
0 = every title, endless loop||||||0 = jeder Titel, Endlosschleife||
0 = flat glass (lightest)||||||0 = flaches Glas (am leichtesten)||
0 turns blur off||||||0 schaltet die Unschärfe aus||
A direct .mp4 / .mov link (Manage from phone can also save it to the TV)||||||Ein direkter .mp4-/.mov-Link („Vom Handy verwalten“ kann ihn auch auf den TV speichern)||
A very soft light around the selected app or poster (also in the Library)||||||Ein sehr weiches Licht um die gewählte App oder das Poster (auch in der Mediathek)||
After each new video||||||Nach jedem neuen Video||
After you leave this screen. The code changes each time it starts again||||||Nachdem du diesen Bildschirm verlässt. Der Code ändert sich bei jedem Neustart||
Apple style||||||Apple-Stil||
Apple style: the logo all in white||||||Apple-Stil: das Logo ganz in Weiß||
Apple TV: the next picture opens up from the right (from the left going back) · Slide: pushed in · Fade||||||Apple TV: das nächste Bild öffnet sich von rechts (von links beim Zurückgehen) · Schieben: hereingeschoben · Überblenden||
Applies after Glassio restarts||||||Gilt nach einem Neustart von Glassio||
Applies the visual preset. Keeps accounts, add-ons, app order, icons, weather city and your media files. Picture cache changes apply after restart.||||||Wendet die Voreinstellung an. Konten, Add-ons, App-Reihenfolge, Icons, Wetterstadt und deine Mediendateien bleiben. Änderungen am Bild-Cache gelten nach einem Neustart.||
Apps without a TV launcher entry||||||Apps ohne Eintrag im TV-Launcher||
At the top, with where each title came from||||||Oben, mit der Herkunft jedes Titels||
Auto uses the first installed: Nuvio Legacy, Nuvio (official), Nuvio Arc, Nuvio Re-Shaped||||||Auto nimmt das erste installierte: Nuvio Legacy, Nuvio (offiziell), Nuvio Arc, Nuvio Re-Shaped||
Back from Nuvio, the carousel shows that title first||||||Zurück aus Nuvio zeigt das Karussell diesen Titel zuerst||
Behind the apps row and the Apps folder||||||Hinter der App-Reihe und dem Apps-Ordner||
Between Play and Next: saves the title to your Library||||||Zwischen Play und Weiter: speichert den Titel in deiner Mediathek||
Between posters and between rows||||||Zwischen Postern und zwischen Reihen||
Carousel backdrops. 1280 enhanced: the light 1280 px picture, sharpened as it is scaled up||||||Karussell-Hintergründe. 1280 verbessert: das leichte 1280-px-Bild, beim Vergrößern geschärft||
Dock and app tiles (0 = square)||||||Dock und App-Kacheln (0 = quadratisch)||
Each title's own picture, or yours: a picture or a video, from a link or the folder below||||||Das eigene Bild jedes Titels oder deins: ein Bild oder Video, aus einem Link oder dem Ordner unten||
Every setting, catalog, Library, Continue Watching, account and token, saved to Download/Glassio on the TV (also from the phone page). Keep it private||||||Jede Einstellung, Katalog, Mediathek, Weiterschauen, Konto und Token, gespeichert in Download/Glassio auf dem TV (auch über die Handy-Seite). Gut aufbewahren||
Every video (Apple, Amazon or your link) is saved as it plays and starts at once next time||||||Jedes Video (Apple, Amazon oder dein Link) wird beim Abspielen gespeichert und startet beim nächsten Mal sofort||
Fade between slides (lower = faster)||||||Überblenden zwischen Folien (kleiner = schneller)||
Find Glassio and switch it on||||||Glassio suchen und einschalten||
From the dock's last row||||||Aus der letzten Reihe des Docks||
Full webhook URL from your Home Assistant||||||Vollständige Webhook-URL aus deinem Home Assistant||
Glassio or any app you pick opens on its own after the TV turns on. Needs the accessibility service above||||||Glassio oder eine gewählte App startet nach dem Einschalten des TVs von selbst. Braucht die Bedienungshilfe oben||
Hidden while you browse the apps||||||Ausgeblendet, während du die Apps durchstöberst||
Hidden while you browse; they appear when you go up to the settings button||||||Ausgeblendet beim Stöbern; sie erscheinen, wenn du hoch zur Einstellungstaste gehst||
How far and how strong it is||||||Wie weit und wie stark es ist||
How many titles in progress open the carousel||||||Wie viele angefangene Titel das Karussell öffnet||
How much the rim bends what is behind it (0 = flat)||||||Wie stark der Rand das Dahinterliegende bricht (0 = flach)||
How the plate behind every icon looks||||||Wie die Platte hinter jedem Icon aussieht||
In the Library||||||In der Mediathek||
It stays below the screen until you go down to it (the dots go down too)||||||Es bleibt unter dem Bildschirm, bis du runtergehst (die Punkte gehen mit)||
Like Apple TV: each title shows several of its pictures (needs the TMDB token)||||||Wie Apple TV: jeder Titel zeigt mehrere seiner Bilder (braucht den TMDB-Token)||
Like Apple TV: press Back on the home screen||||||Wie Apple TV: Zurück auf dem Startbildschirm drücken||
Like tvOS: icons stay white, the selected one shows its colours on a light plate||||||Wie tvOS: Icons bleiben weiß, das gewählte zeigt seine Farben auf einer hellen Platte||
Logo image, plain text, or nothing||||||Logo-Bild, reiner Text oder nichts||
Logos are evened out so wide and tall ones look the same size||||||Logos werden angeglichen, damit breite und hohe gleich groß wirken||
Needs a TMDB token. English is used when a title has no text in this language||||||Braucht einen TMDB-Token. Englisch wird genommen, wenn ein Titel keinen Text in dieser Sprache hat||
Needs the accessibility service below||||||Braucht die Bedienungshilfe unten||
Needs Trakt (Continue Watching)||||||Braucht Trakt (Weiterschauen)||
Neu & beliebt: vary nearby ranking positions. Manual mode: shuffle all titles||||||Neu & beliebt: variiert nahe Platzierungen. Manuell: mischt alle Titel||
Nuvio, Trakt, Simkl or Stremio, with a dot in its colour||||||Nuvio, Trakt, Simkl oder Stremio, mit einem Punkt in seiner Farbe||
Off: in the collections' order||||||Aus: in der Reihenfolge der Sammlungen||
One look for the dock, the icons and the menu||||||Ein Aussehen für Dock, Icons und Menü||
One that's already here isn't added twice||||||Was schon da ist, wird nicht doppelt hinzugefügt||
Open Nuvio > Add-ons > Manage from phone first||||||Erst Nuvio > Add-ons > Vom Handy verwalten öffnen||
Paste a manifest link||||||Einen Manifest-Link einfügen||
Play + Next: like Apple TV, Right or OK on the round button shows the next title, Left on Play the previous one||||||Play + Weiter: wie Apple TV, Rechts oder OK auf der runden Taste zeigt den nächsten Titel, Links auf Play den vorherigen||
Posters, backdrops and logos kept on the TV, so browsing back is instant. Applies after a restart||||||Poster, Hintergründe und Logos bleiben auf dem TV, damit Zurückblättern sofort geht. Gilt nach einem Neustart||
Press a button on the remote, then pick the app it opens in any app. Needs the accessibility service above||||||Drücke eine Taste der Fernbedienung, dann wähle die App, die sie in jeder App öffnet. Braucht die Bedienungshilfe oben||
Press a Fire TV button that opens its own app (YouTube, Netflix, …): pick that app, then the app that should open instead||||||Drücke eine Fire-TV-Taste, die ihre eigene App öffnet (YouTube, Netflix, …): wähle diese App, dann die, die stattdessen öffnen soll||
Pressing past the first or last icon nudges it, like tvOS||||||Über das erste oder letzte Icon hinaus drücken stupst es an, wie tvOS||
Projectivy / Android TV icon packs||||||Projectivy- / Android-TV-Icon-Pakete||
Projectivy / Blueprint / Nova packs installed on this TV||||||Auf diesem TV installierte Projectivy- / Blueprint- / Nova-Pakete||
RAM, CPU, GPU, storage, IP (the ones on in Menu, Library & themes)||||||RAM, CPU, GPU, Speicher, IP (die, die in Menü, Mediathek & Designs an sind)||
Refresh automatically. Neu & beliebt keeps the leading recommendations instead of skipping to random pages||||||Automatisch neu laden. Neu & beliebt behält die führenden Empfehlungen statt auf Zufallsseiten zu springen||
Reload to apply||||||Neu laden, um es anzuwenden||
Resolution of the picture the glass bends (higher = crisper glass, more memory)||||||Auflösung des Bildes, das das Glas bricht (höher = schärferes Glas, mehr Speicher)||
rtsp://user:password@ip:554/… , an HLS (.m3u8) or video link||||||rtsp://user:passwort@ip:554/… , ein HLS- (.m3u8) oder Video-Link||
Scan a QR code with your phone and approve||||||QR-Code mit dem Handy scannen und bestätigen||
Scan a QR code with your phone and approve (signed in to Stremio there)||||||QR-Code mit dem Handy scannen und bestätigen (dort bei Stremio angemeldet)||
Scan a QR code with your phone and approve, like signing in to Nuvio on a TV||||||QR-Code mit dem Handy scannen und bestätigen, wie die Anmeldung bei Nuvio auf einem TV||
Scan a QR code with your phone and enter a short code||||||QR-Code mit dem Handy scannen und einen kurzen Code eingeben||
Scan the code, paste Nuvio manifest links and your TMDB token||||||Code scannen, Nuvio-Manifest-Links und deinen TMDB-Token einfügen||
Searches your network (ONVIF: Hikvision, Dahua, Reolink, Tapo, Amcrest, Axis…)||||||Durchsucht dein Netzwerk (ONVIF: Hikvision, Dahua, Reolink, Tapo, Amcrest, Axis…)||
Seconds before the next title||||||Sekunden bis zum nächsten Titel||
Sends title, type and id as JSON||||||Sendet Titel, Typ und ID als JSON||
Several pictures per title||||||Mehrere Bilder pro Titel||
Show them only when the carousel is selected||||||Nur zeigen, wenn das Karussell gewählt ist||
Shows Continue · S1 · E3 · 42% under the title||||||Zeigt Weiterschauen · S1 · F3 · 42 % unter dem Titel||
Signed in||||||Angemeldet||
Slides change on their own||||||Folien wechseln von selbst||
Slides loaded before you get to them||||||Folien, die geladen sind, bevor du hinkommst||
Smaller makes the dock and its icons narrower||||||Kleiner macht Dock und Icons schmaler||
Smaller posters leave more room at the sides||||||Kleinere Poster lassen mehr Platz an den Seiten||
Start it, choose its videos and when it starts||||||Starten, Videos wählen und wann er startet||
Switch between title backdrops, your picture, your video and the screensaver||||||Wechselt zwischen Titel-Hintergründen, deinem Bild, deinem Video und dem Bildschirmschoner||
Temperature, conditions, high / low, humidity and wind (city in Clock & glass)||||||Temperatur, Lage, Hoch / Tief, Luftfeuchte und Wind (Stadt unter Uhr & Glas)||
The + button also adds the title on Trakt||||||Die +-Taste setzt den Titel auch auf Trakt||
The + button also saves the title to your Nuvio account (Legacy, official Nuvio and forks see it)||||||Die +-Taste speichert den Titel auch in deinem Nuvio-Konto (Legacy, offizielles Nuvio und Forks sehen es)||
The Apple TV-style menu at the settings button||||||Das Menü im Apple-TV-Stil an der Einstellungstaste||
The chips under the title||||||Die Chips unter dem Titel||
The folder with all your apps, in the dock. Hidden apps stay listed below||||||Der Ordner mit allen Apps, im Dock. Ausgeblendete Apps bleiben unten gelistet||
The line before each one||||||Die Zeile vor jedem||
The lines go away 4 s after the last one comes in||||||Die Zeilen gehen 4 s nach der letzten weg||
The menu, the settings sections, the setup and the main rows (the rest stays English for now). Reopen the screen to see it||||||Menü, Einstellungsbereiche, Einrichtung und alle Zeilen. Bildschirm neu öffnen, um es zu sehen||
The newest backup in Download/Glassio on the TV. Glassio restarts after||||||Die neueste Sicherung in Download/Glassio auf dem TV. Glassio startet danach neu||
The page scrolls up into it, like Apple TV; Up from its top row goes back||||||Die Seite scrollt hinein, wie Apple TV; Hoch aus der obersten Reihe geht zurück||
The selected icon rises a little, glass and all||||||Das gewählte Icon hebt sich etwas, mit Glas||
The TV's own idle time, in any app||||||Die eigene Leerlaufzeit des TVs, in jeder App||
The welcome screen: restore, catalogs, accounts and theme||||||Der Willkommensbildschirm: Sicherung, Kataloge, Konten und Design||
Then hidden until you go up to them||||||Danach ausgeblendet, bis du hochgehst||
Top bar, next to the settings button||||||Obere Leiste, neben der Einstellungstaste||
Under the clock, like Apple TV||||||Unter der Uhr, wie Apple TV||
Under the weather: IP, CPU, GPU, RAM, storage (CPU and GPU need TV access (ADB))||||||Unter dem Wetter: IP, CPU, GPU, RAM, Speicher (CPU und GPU brauchen TV-Zugriff (ADB))||
Use and temperature||||||Auslastung und Temperatur||
Use the API Read Access Token (long, starts eyJ…), not the short API Key||||||Nimm den API Read Access Token (lang, beginnt mit eyJ…), nicht den kurzen API Key||
Use, or its memory where the TV doesn't tell the use||||||Auslastung, oder ihr Speicher, wo der TV sie nicht nennt||
Videos without it play the nearest one||||||Videos ohne es spielen das nächstgelegene||
What holding the Home button does, in any app (needs the Home button service above)||||||Was Halten der Home-Taste in jeder App macht (braucht den Home-Tasten-Dienst oben)||
What plays||||||Was abgespielt wird||
What you paused in any app that uses Simkl||||||Was du in einer App mit Simkl pausiert hast||
What you paused in Stremio||||||Was du in Stremio pausiert hast||
What you watch in Nuvio Legacy, the official app and forks||||||Was du in Nuvio Legacy, der offiziellen App und Forks schaust||
Where the video was filmed||||||Wo das Video gedreht wurde||
Where titles you are watching appear||||||Wo Titel erscheinen, die du gerade schaust||
Which picture each title uses||||||Welches Bild jeder Titel nutzt||
With Colour on focus||||||Mit Farbe bei Fokus||
With fewer apps than a full row, the dock shrinks around them||||||Bei weniger Apps als einer vollen Reihe schrumpft das Dock darum||
With your own picture, video or screensaver background||||||Mit deinem eigenen Bild, Video oder Bildschirmschoner-Hintergrund||
Your links and the videos in the backgrounds folder, with a picture of each. OK plays only that one||||||Deine Links und die Videos im Hintergrund-Ordner, mit einem Bild von jedem. OK spielt nur das||
Your links below and the videos in the backgrounds folder||||||Deine Links unten und die Videos im Hintergrund-Ordner||
Your Stremio email and password (the password isn't kept)||||||Deine Stremio-E-Mail und dein Passwort (das Passwort wird nicht gespeichert)||
You can change it any time from the menu||||||Du kannst es jederzeit im Menü ändern||
Optional downloads. Confirm installation in Android, then set up the apps yourself.||||||Optionale Downloads. Bestätige die Installation in Android und richte die Apps dann selbst ein.||
Your catalogs, Continue Watching and Library. Sign in with your phone||||||Deine Kataloge, Weiterschauen und Mediathek. Melde dich mit dem Handy an||
The menu button opens the menu, the dock opens the Library, holding OK on an app shows options||||||Die Menü-Taste öffnet das Menü, das Dock öffnet die Mediathek, OK halten auf einer App zeigt Optionen||
Open||||||Öffnen||
Download||||||Herunterladen||
""".trim().lines().associate { l -> val p = l.split('|'); p[0] to p.drop(1) }
}
