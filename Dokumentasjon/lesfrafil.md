# Les fra fil

`sokos-okosynk` kobler til en sikker ftp-server som er spesifisert av variabelen SFTP_SERVER i yaml-filene i nais-folderen.

Innloggingsinformasjonen er et brukernavn, en RSA-nøkkel og en host-key som ligger i kubernetes secret.
Plasseringen av dette er også gitt i naiserator-filene.

Hvis public key som samsvarer med privatnøkkelen er satt i ftp-serveren skal `sokos-okosynk` kunne hente linjene fra fila
i `BatchService`

# Oversett til Meldinger

Linjene i tekstfila er bygget opp med faste lengder som er forskjellig fra OS og UR.

## UR-meldinger

```
1        10        20        30        40        50        60        70        80        90        100       110       120       130       140       150       160
|          |           |                  | |         |              |   |      |    |         |        |                                                |
27026534807PERSON      2020-01-21T12:38:3724GKA2960   00000000006860A8020GHBATCHUR2302020-01-21001618071Manuell retur - fra bank                         27026534807
```

Plasseringen av variablene er gitt i UrMeldingFormat.

## OS-meldinger

```
1        |10       |20       |30       |40       |50       |60       |70       |80       |90       |100      |110      |120
           |         |         |         |   |       |         |          |           ||           |        |          |
01234567891029568753 2009-11-062009-11-30AVVEX123456 2009-11-012009-11-30000000072770æ 8020         HELSEREF01234567891      
```

Plasseringen av variablene er gitt i OsMeldingFormat.

## Logikk ved oversetting til meldinger

Det skjer veldig lite logikk ved oversetting av linjer til meldinger,

### Tolking av desimaltall

Totalt nettobeløp er oppgitt i et spesielt format fra stormaskin.
Beløpet er paddet med nuller, og de to siste tegnene viser desimaltall og fortegn.
Nest siste tegn er et tall som angir første desimal.
Det siste tegnet er en bokstav eller et spesialtegn som angir fortegn og verdien på andre desimal.
I Util-klassen er det oppgitt to lister KODER_FOR_POSITIVT_FORTEGN og KODER_FOR_NEGATIVT_FORTEGN.
Symboler i de to listene angir om hele beløpet er hhv positivt eller negativt, indexen i arrayen angir tallverdien.

### Datoer og tidspunkter

Datoer og tidspunkter i fila leses inn som LocalDate og tidspunktet kastes.

### Blanke tegn

Blanke tegn på starten og slutten av hver variabel blir trimmet bort.

# Oversetting av Meldinger til Oppgaver

## Mappingregler

Meldinger som ikke har en mapping angitt i filene **os_mapping_regel.properties** og **ur_mapping_regel.properties** blir filtrert ut.
Det vil si at regelnøkkelen, som er satt sammen av Behandlende enhet og Faggruppe i OS eller Oppdragskode i UR, har en linje i filene.

## Gruppering av Meldinger

Før Oppgaver opprettes, grupperes Meldinger som har samme GjelderId, GjelderIdType, AnsvarligEnhetId og enten Faggruppe(OS) eller Oppdragskode(UR).

## Opprettelse av Oppgaver

Det opprettes 1 Oppgave fra hver liste med Meldinger.
Totalantallet meldinger i hver Gruppe telles, og meldingen med tidligste Beregningsdato(OS) eller DatoPostert(UR)
beholdes og brukes til å opprette oppgave, mens de andre kun brukes til å lage en samlet Beskrivelse.

## Samlet beskrivelse OS

Meldingens tidligste forsteFomIPeriode og seneste sisteTomIPeriode med samme NyesteVentestatus og summen av beløpene for hver NyesteVentestatus skrives etter hverandre.
Se OsBeskrivelseInfo.

## Samlet beskrivelse UR

Ingen logikk, alle feltene skrives ut.
Se UrBeskrivelseInfo.

