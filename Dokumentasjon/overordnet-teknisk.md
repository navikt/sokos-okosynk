# Okosynk - teknisk oversikt

```mermaid
sequenceDiagram
    Batch               ->> TinyFtpReader        : ftp-config+filnavn
    TinyFtpReader       ->> Batch                : List<String>
    Batch               ->> Melding              : String
    Melding             ->> Batch                : Melding
    Batch               ->> OppgaveOppretter     : Melding
    OppgaveOppretter    ->> PDL                  : fnr, dnr
    PDL                 ->> OppgaveOppretter     : aktørid
    OppgaveOppretter    ->> Batch                : Oppgave
    Batch               ->> OppgaveSynkroniserer : Oppgaver
    OppgaveSynkroniserer->> OppgaveRestClient    : aktørider
    OppgaveRestClient   ->> OppgaveSynkroniserer : Åpne oppgaver
    OppgaveSynkroniserer->> OppgaveRestClient    : Opprett oppgaver
    OppgaveSynkroniserer->> OppgaveRestClient    : Oppdater oppgaver
    OppgaveSynkroniserer->> OppgaveRestClient    : Fullfør oppgaver
```

```mermaid
flowchart LR
    innfil(("Fil"))
    PDL(("PDL"))
    GOSYS(("GOSYS"))
    GOSYS2(("GOSYS"))
    subgraph Okosynk 
        Batch["Batch"] --> lesFraFil
        subgraph HentBatchoppgaver 
            lesFraFil --> parseLinje
            parseLinje --> lagOppgave
        end
        lagOppgave --> OppgaveSynkroniserer
        subgraph synkroniserer
            OppgaveSynkroniserer
        end
    end
        OppgaveSynkroniserer -- opprett --> GOSYS2
        OppgaveSynkroniserer -- oppdater --> GOSYS2
        OppgaveSynkroniserer -- fullfør --> GOSYS2
    innfil -- melding --> lesFraFil
    PDL -- aktørid --> lagOppgave
    GOSYS -- eksisterende oppgaver --> OppgaveSynkroniserer
```


# Konfigurasjon av Okosynk
Konfigurasjon er satt opp ved at man oppretter en singleton av klassen OkosynkConfiguration

```mermaid
classDiagram
    CliMain --|> OkosynkConfiguration
    OkosynkConfiguration : addVaultProperties()
    
    OkosynkConfiguration : getString()
    OkosynkConfiguration : System configuration    
    OkosynkConfiguration --|> SystemConfiguration    
    OkosynkConfiguration --|> CompositeConfiguration    
    OkosynkConfiguration --|> EnvironmentConfiguration    
    OkosynkConfiguration --|> Vault
    
    CompositeConfiguration ..|> file    
    SystemConfiguration ..|> Systemenv
    EnvironmentConfiguration ..|> Naiserator
        
    Vault : kv/preprod/fss/okosynkos/sftpcredentials
    Vault : username
    Vault : private key
    Vault : kv/preprod/fss/okosynkos/oppgavecredentials
    Vault : username
    Vault : password
    
    CliMain : createOkosynkConfiguration()
    Naiserator : FTP-filnavn
    Naiserator : OPPGAVE_URL
    Naiserator : PDL_URL
    Naiserator : SHOULD_RUN_OS_OR_UR
    Systemenv : NAIS_APP_NAME
    file : okosynk.configuration
```


## Les fra fil
Her leses meldingene fra OS og UR fra fil og meldinger uten mappingregel filtreres bort. 
Se [Les fra fil](lesfrafil.md)

## OppgaveOppretter
Meldingene aggregeres og oversettes til oppgaver.
Se [Oppgaveopprettelse](oppgave.md)

## Les fra Gosys
Meldinger som er opprettet av okosynk for de aktuelle aktørene hentes fra Gosys.
Disse sammenlignes med oppgavene som er opprettet fra fil.

## Synkronisering
Sammenligning av oppgaver gjøres med hashcode og equals-metodene til klassen Oppgave.
Det er en manglende symmetri mellom dem i at hashcode ikke tar med aktørid/folkeregisterident, 
som likevel brukes i equals-metoden.

### Opprettelse av nye oppgaver
Oppgaver som er opprettet fra meldinger i fila, men ikke finnes fra før i Gosys, opprettes.

### Ferdigstilling av oppgaver
Oppgaver som finnes fra før, men ikke er i filen, ferdigstilles.

### Oppdatering av oppgaver som fortsatt er åpne
Oppgaver som finnes fra før oppdateres med ny informasjon fra fila, og vi tar vare på inntil 10 tegn som er lagret i 
meldingen som ligger i Gosys og skal videreføres til den oppdaterte oppgaven.
