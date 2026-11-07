# Gemma spike A5

Aprire `GemmaSpike.xcodeproj` in Xcode. Il progetto diagnostico usa LiteRT-LM **0.18.1** e il bundle `com.emanuel5014.trainable.gemmaspike`.

La risoluzione del pacchetto Google richiede Git LFS nel `PATH` del processo Xcode. Usare `GIT_LFS_SKIP_SMUDGE=1` per evitare di scaricare anche i file dimostrativi del repository Google; i framework necessari vengono scaricati da SwiftPM e verificati con il checksum del pacchetto.

1. Selezionare il proprio team in Signing & Capabilities e un iPhone fisico con almeno 8 GB di RAM. Lo schema avvia una build Release senza debugger.
2. Importare da File un modello E2B o E4B nel formato usato da Trainable. I file ufficiali sono indicati in [AiModels.kt](../../shared/src/commonMain/kotlin/com/emanuel5014/trainable/data/ai/AiModels.kt). Importare un modello alla volta, mantenendo almeno 512 MB liberi oltre alla copia.
3. Usare la pagina sintetica predefinita oppure scegliere una foto. Il prompt viene letto dal file condiviso [ProgramScan.kt](../../shared/src/commonMain/kotlin/com/emanuel5014/trainable/data/ai/ProgramScan.kt), incluso come risorsa.
4. Eseguire prima **CPU + visione GPU**, come Android, poi **GPU + visione GPU**. Ripetere per E2B ed E4B. Distinguere il primo caricamento dalle prove successive con cache già presente.
5. Provare Interrompi durante la generazione e ripetere una scansione. Il pulsante diventa disponibile dopo l'inizializzazione del motore. La conversazione e il motore vengono trattenuti fino alla conclusione dello stream nativo.
6. Esportare le misure JSON. Controllare che il testo riproduca nome, massimale e le due righe della pagina sintetica. Per questa immagine il risultato atteso è `Squat con Bilanciere (115kg)`, `W1: 70% 4x5`, `W2: 80% 3x4`.

Il report contiene identificativo hardware, versione iOS/runtime, tempi di inizializzazione, primo chunk e durata totale, picco della memoria fisica campionato ogni 100 ms, stato termico, testo, eventuale canale thinking, token e velocità riportati dal runtime quando disponibili. Il primo chunk è misurato dall'inizio della prova e comprende il caricamento. Il picco campionato può perdere variazioni più brevi dell'intervallo. Un errore o un'interruzione viene registrato come tale. La prova viene salvata prima di caricare il motore: dopo una chiusura del processo, al riavvio compare come `incomplete`; la causa va cercata nei log del dispositivo.

Il processo usa il normale limite di memoria dell'app. Un'eventuale terminazione iOS per memoria richiede il relativo log Jetsam. La valutazione dell'entitlement per il limite di memoria aumentato dipende da queste misure.

Compilazione per iPhone, senza firma né installazione, dalla radice del repository.

```sh
GIT_LFS_SKIP_SMUDGE=1 xcodebuild -project iosApp/GemmaSpike/GemmaSpike.xcodeproj \
  -scheme GemmaSpike -configuration Release \
  -destination 'generic/platform=iOS' \
  -derivedDataPath /tmp/trainable-a5-derived \
  CODE_SIGNING_ALLOWED=NO build
```

La UI può essere ispezionata nel simulatore, mentre il pulsante Avvia prova richiede l'iPhone fisico. A5 prosegue con il bridge Kotlin/Swift, l'importazione e il download nell'app dopo la valutazione delle misure sul dispositivo.

[API Swift ufficiale](https://github.com/google-ai-edge/LiteRT-LM/tree/v0.18.1/swift)
