# Clairjour — SUIVI.md

## 🧭 Ligne directrice
**État** (2026-11-06, **v1.2.0 prête, non poussée**, versionCode 5) : refonte v1.2 terminée — APK release 26 → 13,2 Mo (AGP 9, Vico retiré, ARM seul), barre du bas et interrupteurs aux couleurs Clairjour, Accueil épuré, Stats avec calendrier + frise de jalons, Journal par semaine avec recherche sans accents, page Bouclier réordonnée, pastille « bouclier coupé » ; 5 bugs corrigés en route (bouton « Respire » et « + » invisibles, flash d'état vide, cascade de jalons, fausse pastille). 81 tests OK, captures + README à jour. Prochaine priorité : **vérif sur le Redmi, puis push + release v1.2.0**.

## ✅ Fait (v0.1 + v0.2)
### v0.1 initial
- [x] Branding Clairjour + mockups + design doc
- [x] Scaffold Android Studio complet, Gradle 8.10.2 + AGP 8.7.3 + Kotlin 2.0.21
- [x] Room DB v1 (5 entités) + DataStore + WorkManager + Glance + Vico + i18n FR/EN
- [x] Écrans Onboarding, Home, Journal, Stats, Settings, AddictionEdit
- [x] Notifications + widget + backup JSON local + animation jalons
- [x] Icône adaptive + keystore release + build debug/release signé (v0.1.0, 11 juillet)

### v0.2 refonte — Sécurité (Phase 1)
- [x] **P1.1** Chiffrement backup JSON AES-256/GCM + PBKDF2 210k iters (`data/backup/BackupCrypto.kt`)
- [x] **P1.2** SQLCipher pour DB Room, clé stockée dans EncryptedSharedPreferences (`security/DatabasePassphraseProvider.kt`, `data/db/ClairjourDatabase.kt`)
- [x] **P1.3** `allowBackup=false` + `data_extraction_rules` restrictif (exclusion DB, SharedPrefs sécurisés, DataStore)
- [x] **P1.4** Import backup transactionnel (`withTransaction`), validation version, plafond 10 Mo / 50k rows par table
- [x] **P1.5** Notifications `VISIBILITY_SECRET` + PublicVersion neutre (`ReminderWorker`)
- [x] **P1.6** Retrait permission `RECEIVE_BOOT_COMPLETED` inutile + `usesCleartextTraffic=false` + `network_security_config.xml`
- [x] **P1.7** Signing config lit env vars `CLAIRJOUR_STORE_PASSWORD/KEY_PASSWORD` en priorité + doc keystore rotation

### v0.2 refonte — UX critique (Phase 2)
- [x] **P2.8** Suppression : bouton poubelle TopBar `AddictionEditScreen` (mode édition) + `SwipeToDismissBox` sur items `JournalScreen` avec `AlertDialog` confirmation
- [x] **P2.9** Accessibilité TalkBack : `contentDescription` partout, semantics sur `CounterBlock` (Home) et `StepIndicator` (Onboarding)
- [x] **P2.10** Undo rechute : `Snackbar` avec action "Annuler" 5s, `RelapseRepository.undoRelapse()` + snapshot
- [x] **P2.11** i18n déclencheurs journal : `stringArrayResource` avec clés EN persistées et labels FR/EN affichés
- [x] **P2.12** Onboarding flexible : DatePicker "sobre depuis", champs coût/unités marqués "(facultatif)" + bouton "Configurer plus tard"
- [x] **P2.13** Touch targets ≥ 48dp (chips humeur JournalScreen)

### v0.2 refonte — Features (Phase 3)
- [x] **P3.14** Panic button + `CrisisScreen` : FAB "Respire" sur Home, cercle de respiration animé 4s, contacts 3114 + Drogues Info Service (dial direct), raisons personnelles affichées
- [x] **P3.15** Raisons personnelles ("why") : nouveau champ `personalReasons: List<String>` sur `AddictionEntity` (DB v2), UI liste éditable dans `AddictionEditScreen`, affichage dans `CrisisScreen`, propagées dans backup
- [x] **P3.16** Documenté comme reste v0.3 (auto-backup) — trade-off sécurité vs praticité à trancher
- [x] **P3.17** Stats enrichies : streak actuel vs record, moyennes humeur 7j/30j, top 3 déclencheurs (30 derniers jours), état vide parlant sur graphe humeur

### v0.2 refonte — Polish (Phase 4)
- [x] **P4.18** Filtres journal : FilterChips humeur 1-5 + cravings + bouton "Effacer les filtres"
- [x] **P4.19** États vides parlants (StatsScreen graphe humeur)
- [x] **P4.20** Feedback save : `Snackbar` "Domaine/Entrée enregistrée" sur `AddictionEditScreen` et `JournalEditorScreen`
- [x] **P4.21** Prefix `€ ` sur champ coût (AddictionEditScreen + Onboarding)
- [x] **P4.22** `BuildConfig.VERSION_NAME` dynamique (SettingsScreen)
- [x] **P4.23** Vico beta gardé (2.0.0 GA pas encore sortie — surveiller)
- [x] **P4.24** Pledge : `AlertDialog` pédagogique avant l'engagement (explication "qu'est-ce que l'engagement du jour")

### Build final
- [x] `./gradlew assembleDebug` **SUCCESSFUL** (2026-07-21, ~46 Mo APK avec SQLCipher)
- [x] `./gradlew testDebugUnitTest` **SUCCESSFUL** (2026-07-21)
- [x] Fix jalons re-affichés après rechute (2026-08-02) — `markAllSeenFor` remplace `clearFor` dans `reportRelapse` ; undo utilise `insertAll` REPLACE. `compileDebugKotlin` + `testDebugUnitTest` OK.

## 🛡️ v1.1 — Bouclier Instagram (plan)
Principe : `AccessibilityService` qui lit l'écran d'Instagram (app officielle intacte) et réagit par retour auto, clic forcé ou cache opaque. Règles toutes modulables, désactivation freinée par 10 s d'attente.
- [x] **B1** Cœur pur testable `shield/` : `ScreenNode` (arbre léger), `InstagramSelectors` (IDs candidats centralisés), `InstagramScreenClassifier`, `ShieldEngine` (sessions reel + origine, cooldown anti-boucle)
- [x] **B2** Règles (`ShieldRule`) : onglet Reels, reels DM = un seul, reels ouverts ailleurs (fil/notifs), reels depuis profil, grille Explorer cachée, recherche = Comptes forcé, fil d'accueil caché, stories
- [x] **B3** Service Android : capture arbre, debounce, actions (BACK, clic, overlay `TYPE_ACCESSIBILITY_OVERLAY`), lecture règles via DataStore
- [x] **B4** UI : écran « Bouclier Instagram » (statut service, interrupteurs, dialogue 10 s pour désactiver, avertissement Play Store avant activation, mode diagnostic en debug)
- [x] **B5** Tests unitaires classifieur + moteur
- [x] **B6** Calibration des sélecteurs sur téléphone (adb + logcat `ClairjourShield`) et tests réels des 8 règles
- [x] **B7** Build debug + tests + commit/push
- [x] **B8** (2026-11-03) Reel touché dans une story = traité comme un reel reçu en message (un seul, swipe → retour) — règle `DIRECT_SINGLE_REEL` renommée « reels partagés avec toi »
- [x] **B9** Nouveau cache Explorer (`CoverOverlay`) : suit le thème système, icône bouclier, motivation aléatoire (`motivations.json`), bouton « Chercher un compte » qui touche la barre de recherche d'Instagram
- [x] **B10** Désactivation immédiate (dialogue 10 s supprimé) ; avertissement Play Store affiché une seule fois, ensuite « Activer » ouvre directement la page du service
- [x] **B11** Statut à 3 états (`ShieldServiceState` : actif / coupé par le système / désactivé) via la liste des services réellement liés + écoute en direct ; carte Xiaomi (démarrage auto, batterie « Pas de restrictions », verrouillage dans les récents) ; `ShieldWatchdogWorker` (15 min) notifie si le bouclier qui tournait a été coupé
- [x] **B12** (2026-11-03) Cache en noir sans citation, décalé de 12 dp sous la barre de recherche ; textes de la page réécrits (état « arrêté par le téléphone » + bouton « Relancer ») ; carte « Garder le bouclier allumé » sur toutes les marques (démarrage auto Xiaomi/Oppo/Realme/OnePlus/Vivo/Huawei/Honor, batterie)
- [x] **B13** (2026-11-03) Service d'accessibilité dans un processus séparé et léger `:shield` (⚠️ ne survit PAS au nettoyage des récentes : `SwipeUpClean` tue tous les processus de l'app — vérifié le 2026-11-03 à 23:51) ; réglages du bouclier déplacés dans `ShieldSettingsStore` (DataStore multi-processus, `files/datastore/shield_settings.json`, migration auto depuis `clairjour_prefs`) ; `ClairjourApplication` ne démarre ni la DB ni WorkManager dans `:shield`
- [x] **B14** (2026-11-04) L'app lit l'autorisation « Démarrage automatique » de HyperOS (app op privé MIUI 10008, par réflexion) : bouton mis en avant + avertissement rouge si refusée, ✓ si autorisée
- [x] **B15** (2026-11-04) Vérifié sur le Redmi : avec démarrage auto autorisé, `SwipeUpClean` tue les 2 processus et Android relance `:shield` 1 s après
- [x] **B16** (2026-11-04) Captures `docs/screenshots/` (émulateur Pixel_7, données fictives) + README + release v1.1.0 (APK signé)
- [x] **B17** Corrections vues pendant les captures : nom du domaine en anglais à l'onboarding (« Tobacco ») → libellé traduit ; devise « ¤ » quand la langue de l'app n'a pas de pays → euro forcé ; texte illisible dans la carte « Bouclier actif » sur le Redmi (intents MIUI `AutoStartManagementActivity` / `HiddenAppsConfigActivity` à confirmer sur HyperOS)
- [x] **B18** (2026-11-06) « Un seul reel à la fois » étendu au fil d'accueil et à « J'aime » (Votre activité) : le reel s'ouvre (pause possible), swiper → retour. Vérifié sur le Redmi (fil + J'aime)
- [x] **B19** Corrections vues en mode diagnostic : fil non reconnu quand Instagram ne marque aucun onglet sélectionné ; faux swipe quand le nom d'auteur disparaît un instant (clé de reel = auteur uniquement)
- [x] **B20** (2026-11-06) Appui long ×2 sur un reel en collaboration faisait sortir du reel : clé = premier nom d'utilisateur uniquement (« mediapart et anttonrouget » vs « Reel de mediapart. ») ; capture README du Bouclier refaite

## 🎨 v1.2 — Refonte visuelle + optimisation (plan)
- [x] **R1** (2026-11-06) Optimisation : AGP 9.1.1 + Gradle 9.3.1 + Kotlin 2.1.20 + KSP 2.0.1 + Room 2.7.0 + BOM 2025.01 (même combo qu'AlcoLimit, Kotlin 2.2 non retenu), Vico **retiré** (jamais utilisé, courbe en `Canvas`), abiFilters ARM → **APK release 26,0 → 13,2 Mo**, debug 48,5 → 34,7 Mo ; baseline profile vérifié dans l'APK ; durée d'analyse du Bouclier loguée en debug (« evaluation took N ms », à lire sur le Redmi en R8) ; compteur à la minute reporté à R3
- [x] **R2** (2026-11-06) Thème : `surfaceContainer*` définis en clair (ivoire) et sombre (bleu nuit), pastille de la barre du bas = `surfaceVariant` + icône `primary`, `ClairjourSwitch` commun (éteint neutre) sur les 4 écrans. Vérifié sur émulateur clair + sombre
- [x] **R3** (2026-11-06) Accueil : compteur « + X h YY min » rafraîchi au début de chaque minute (au lieu de chaque seconde), barre de jalon continue (`gapSize = 0.dp`, sans point final) + pourcentage localisé. Bugs corrigés au passage : bouton « Respire » caché sous la barre du bas (padding de la barre appliqué au `Scaffold` de l'Accueil), état vide « Que veux-tu suivre ? » affiché pendant le chargement de la base (`state.loading` ignoré), « dans 1 jours » → plurals `home_next_milestone`. Vérifié sur émulateur
- [x] **R4** (2026-11-06) Stats : cartes Série (+ record) et Économisé (+ par jour), frise de jalons défilante (atteints ✓, prochain avec jours restants, ouverte sur le prochain), calendrier mensuel ‹ › (`domain/SobrietyCalendar.kt`, 6 tests), humeur + moyennes dans une carte, déclencheurs dans une carte. Vérifié sur émulateur (47 j, 517 €, nov./oct.)
- [x] **R5** (2026-11-06) Journal : recherche en mémoire insensible à la casse et aux accents (`domain/JournalBrowsing.kt`, la recherche SQL `LIKE` supprimée), entrées regroupées par semaine (« Cette semaine », « Semaine dernière », « Semaine du … »), bande de couleur d'humeur (rouge/doré/vert sauge) + déclencheurs sur la carte, barre de recherche arrondie avec effacement. Bugs corrigés : bouton « + » sous la barre du bas (même cause que l'Accueil), fond rouge + poubelle visible à travers les cartes au repos (dessiné seulement pendant le glissement). 6 nouveaux tests (75 au total)
- [x] **R6** (2026-11-06) Bouclier : statut → interrupteurs → section « Fiabilité » en bas ; carte « Garder le bouclier allumé » repliée avec « ✓ fait » quand `ShieldKeepAlive.isDone` (batterie sans restriction + démarrage auto non refusé), dépliée sinon ; 4 tests. Repli à vérifier sur le Redmi (émulateur : batterie non réglée → dépliée, attendu)
- [x] **R7** (2026-11-06) Pastille rouge (`BadgedBox`) sur l'onglet Paramètres quand `ShieldKeepAlive.needsAttention` (bouclier voulu + `STOPPED_BY_SYSTEM`), état relu à chaque retour dans l'app + écoute accessibilité ; test unitaire. ⚠️ Non simulable sur émulateur (le système relance le service aussitôt ; `am force-stop` retire le service des réglages → état OFF) → à vérifier sur le Redmi
- [x] **Bug jalons** (2026-11-06) Reculer la date « Sobre depuis » d'un domaine existant déclenchait une cascade de célébrations (1 jour, 3 jours…) : le remplissage « déjà vus » n'existait qu'à la création → `markPastMilestonesSeen` partagé par `create` et `update` (`AddictionRepositoryTest`). Vérifié sur émulateur
- [x] **R8a** (2026-11-06) Faux positif de la pastille corrigé : l'écouteur d'accessibilité se déclenche avant qu'Android lie le service → `ShieldStatusEffect` relit l'état 2 s après chaque changement (Accueil + page Bouclier). Pastille vue à l'écran (rendu OK). Captures `docs/screenshots/` refaites (émulateur Pixel_7, mode démo, données fictives 47 j / 11 €), README à jour, version **1.2.0 (versionCode 5)**
- [ ] **R8** Vérif Redmi (repli carte Fiabilité, pastille si HyperOS coupe le bouclier, temps d'ouverture de la base, « evaluation took N ms ») puis push + release v1.2.0

## ⏳ Reste à faire
- [ ] Date « Sobre depuis » affichée en anglais (« Sep 20, 2026 ») dans l'écran Modifier un domaine en français
- [ ] Installer et tester APK sur téléphone (`adb install app-debug.apk`)
- [ ] Vérifier sur device : chiffrement backup (export + réimport avec passphrase), swipe-to-delete journal, undo rechute, Crisis screen (dial 3114), suppression addiction
- [ ] Regénérer keystore avec `storePassword ≠ keyPassword` avant publication + déplacer `.jks` hors dossier projet vers `~/.android/keystores/`
- [ ] Build release signé (`./gradlew assembleRelease`) — vérifier taille avec R8
- [ ] Publication Play Store : screenshots, store listing FR/EN, politique de confidentialité
- [ ] **v0.3 features possibles** : auto-backup WorkManager (nécessite décision sur stockage passphrase), Vico 2.0.0 stable dès sortie, remontée des raisons perso sur HomeScreen

## 📋 Notes / gotchas
- **Jalons et date modifiée** : toute écriture qui recule `startDate` doit passer par `AddictionRepository.update` (remplissage des jalons passés comme vus), sinon l'Accueil les insère non vus → cascade de célébrations.
- **`SwipeToDismissBox` + carte translucide** : le `backgroundContent` se voit à travers la carte au repos → ne le dessiner que si `dismissState.dismissDirection == EndToStart`.
- **`adb shell input text`** : bloque ou sort de l'app avec certains caractères (virgule, apostrophe) → texte ASCII simple, espaces en `%s`, et `timeout` autour de chaque commande adb.
- **Écran imbriqué avec son propre `Scaffold`** (Accueil) : le padding de la barre du bas (`contentPadding`) doit aller sur le `Scaffold` lui-même, sinon FAB et snackbar sont dessinés sous la barre.
- **Ouverture SQLCipher ~15 s sur l'émulateur** (traduction ARM, APK sans x86) : normal en émulateur, à chronométrer sur le Redmi en R8.
- **Émulateur + abiFilters ARM** : l'émulateur Pixel_7 (x86_64) lance quand même l'app grâce à la traduction ARM (abilist `x86_64,arm64-v8a`), premier démarrage lent (splash quelques secondes).
- **adb depuis Git Bash** : `/sdcard/...` est converti en chemin Windows → `export MSYS_NO_PATHCONV=1`. `uiautomator dump` peut se bloquer : l'encadrer d'un `timeout 30`.
- **Barre de progression « coupée » avec un point** : style par défaut du `LinearProgressIndicator` M3 récent (gap + stop indicator) → `gapSize = 0.dp`, `drawStopIndicator = {}`.
- **AGP 9** : plugin `kotlin-android` retiré (Kotlin intégré à AGP), bloc `kotlinOptions` supprimé (jvmTarget suit `compileOptions`), `android.disallowKotlinSourceSets=false` requis pour KSP. Premier build ~10 min (téléchargement Gradle 9.3.1).
- **`installDebug` → `No connected devices!`** (2026-11-06) : téléphone non branché. `adb` n'est pas dans le PATH bash : utiliser `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"`.
- **Maquettes en localhost** : le port 8765 renvoyait 404 (déjà pris par un autre serveur) → servir `mockups/` sur un autre port (`python -m http.server 8790`).
- **Barre de navigation lavande/grise** : `NavigationBar` utilise `surfaceContainer`, non défini dans `Theme.kt` → valeurs Material par défaut. Définir tous les `surfaceContainer*`.
- **Bouclier — calibration** : la version debug embarque une télécommande (`adb shell am broadcast -a com.clairjour.shield.DEBUG -e command dump|back|click_id|click_label|scroll|tap|type|enter|rule_on|rule_off|shield_on|shield_off -e argument '...'`), logs sous le tag `ClairjourShield`. Le geste `tap` n'existe qu'en debug (`src/debug/res/xml/instagram_shield_service.xml` ajoute `canPerformGestures`).
- **Bouclier — « Paramètre restreint »** (Android 13+, app hors Play Store) : l'interrupteur d'accessibilité est grisé → `adb shell cmd appops set com.clairjour.app.debug ACCESS_RESTRICTED_SETTINGS allow` (ou Infos appli → ⋮ → Autoriser les paramètres restreints).
- **Bouclier — limites connues** : les vidéos qui se lancent toutes seules dans le fil ne sont pas bloquées (seulement à l'ouverture en plein écran) ; les libellés EN des onglets de recherche n'ont pas été vérifiés (interface testée en FR).
- **Téléphone de test (Redmi Note 13 Pro+, HyperOS / Android 14, Instagram 449.0.0.52.84)** : sans l'option « Débogage USB (paramètres de sécurité) », adb refuse `settings put secure` (`SecurityException: WRITE_SECURE_SETTINGS`) et `input tap/keyevent` (`INJECT_EVENTS`). `uiautomator dump` échoue sur Instagram (« could not get idle state », vidéos) → calibrer via le mode diagnostic du service + `adb logcat -s ClairjourShield`. Sous Git Bash, préfixer `MSYS_NO_PATHCONV=1` pour les chemins `/sdcard/...`. Si l'appareil est `offline` : `adb kill-server` puis `adb devices`.
- **Bouclier — sélecteurs Instagram** : tous dans `shield/InstagramSelectors.kt` (Instagram appelle les Reels « clips » et les Stories « reels »). Après une MAJ Instagram qui casse la détection : activer le mode diagnostic (debug), `adb logcat -s ClairjourShield`, corriger les IDs.
- **Bouclier — pas de filtre `packageNames`** dans `instagram_shield_service.xml` : volontaire, sinon le service ne voit pas Instagram passer en arrière-plan et le cache opaque resterait sur l'écran d'accueil. Les événements des autres apps sont ignorés dans le code.
- **Bouclier — Xiaomi/HyperOS coupe le service** : sans « Démarrage automatique » + batterie « Pas de restrictions », HyperOS retire le service d'accessibilité (il peut même rester coché alors qu'il ne tourne plus). Le statut se base donc sur `AccessibilityManager.getEnabledAccessibilityServiceList` (services réellement liés) et pas seulement sur `ENABLED_ACCESSIBILITY_SERVICES`.
- **Bouclier — onglet sélectionné instable** : au retour sur le fil, Instagram ne marque AUCUN onglet `SELECTED` pendant ~1 s → la liste `android:id/list` seule suffit à reconnaître le fil ; la boîte de messages a la même liste, d'où la détection de `direct_tab`.
- **Bouclier — détection du swipe** : la clé d'un reel = nom de l'auteur (`clips_author_username`) ou, à défaut, celui de la description « Reel de X. » (les noms peuvent contenir des points). Collaborations : « alice et bob » → garder le premier nom. Ne jamais se rabattre sur d'autres libellés (sous-titres, « Appuyez deux fois… ») : ils clignotent et provoquent de faux swipes.
- **Bouclier — démarrage auto HyperOS** : état lisible par `adb shell appops get <pkg> 10008` (`allow`/`ignore`), modifiable par `adb shell appops set <pkg> 10008 allow`. ⚠️ Chaque `adb install -r` le remet à `ignore`.
- **Bouclier — « non activé » alors que coché** : balayer Clairjour dans les récentes (HyperOS `SwipeUpClean`) tue le processus ; sans démarrage auto, HyperOS refuse de relancer le service (« process is not permitted to auto start ») → Android le classe en « Crashed services » et ne le relance plus tant que l'interrupteur n'est pas désactivé puis réactivé (ou l'appli réinstallée). Vérifier avec `adb shell dumpsys accessibility | grep -E "Bound|Crashed"`.
- **Bouclier — processus `:shield`** : ne jamais lire `container`/la DB depuis le service (l'Application ne l'initialise pas dans ce processus). Les réglages du bouclier passent UNIQUEMENT par `ShieldSettingsStore` (une instance par processus) ; ne pas les remettre dans le DataStore `clairjour_prefs`, qui est mono-processus.
- **Build en ligne de commande** : Gradle refuse Java 8 (« requires at least JVM runtime version 11 ») → `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` avant `./gradlew.bat`.
- **Bouclier — Play Console** : déclarer l'usage de l'API Accessibilité (non-outil d'accessibilité, `isAccessibilityTool=false`) avec vidéo de démo + l'avertissement in-app (dialogue « Avant d'activer »). Texte : « Clairjour utilise l'API AccessibilityService uniquement pour reconnaître les écrans d'Instagram choisis par l'utilisateur (Reels, Explorer, recherche) et y revenir en arrière ou les masquer, afin de l'aider à limiter le défilement compulsif. Aucune donnée n'est collectée, stockée ni transmise. »
- **Migration DB v1→v2** : `fallbackToDestructiveMigration()` actif — passage à SQLCipher réinitialise la DB de toute façon (bad-magic sur ancien fichier plaintext). Les utilisateurs v0.1 doivent réonboarder OU utiliser un backup JSON legacy (pas chiffré) exporté avant la mise à jour. À documenter dans le changelog.
- **BackupRepository.import** est backward-compat : détecte le magic `CLAIRJOUR1` et fallback sur JSON legacy si absent — permet la migration en douceur des rares testeurs v0.1.
- **Passphrase backup** : jamais stockée. Le user doit la retenir. C'est un choix conscient (sécurité > praticité). Une auto-backup nécessiterait de la stocker dans le Keystore Android, ce qui serait à trancher.
- **SQLCipher** : `System.loadLibrary("sqlcipher")` chargé une seule fois via `ensureNativeLibs()`. La lib pèse ~15 Mo (raison de la taille APK). Alternative future : réduire via ABI splits.
- **`allowBackup=false`** : bloque le cloud-backup Google et le device-transfer. Les données restent locales — la seule copie externe possible est un backup manuel chiffré.
- **Keystore** : commentaire ajouté dans `keystore.properties`. En prod, passer par variables d'env `CLAIRJOUR_STORE_PASSWORD` / `CLAIRJOUR_KEY_PASSWORD`. Actuellement `storePassword == keyPassword == Cl4irj0ur!2026` — à changer lors de la génération du nouveau keystore.
- **CrisisScreen numbers** : 3114 (prévention suicide FR) + 0800235236 (Drogues Info Service FR). Adapter si i18n étendue.
- **compileSdk = 36** (imposé par androidx.core 1.15 + work 2.10)
- **JDK 21** bundled Android Studio : `C:\Program Files\Android\Android Studio\jbr\` (exporter `JAVA_HOME` pour CLI)
- **Chemins APK** : `app/build/outputs/apk/debug/app-debug.apk` (~46 Mo) et `app/build/outputs/apk/release/app-release.apk`

## 🚀 Pour installer sur téléphone
```powershell
adb install "C:\users\flora\documents\dev\Application\Clair jour\app\build\outputs\apk\debug\app-debug.apk"
```
