
# Sistem de Monitorizare Postură Multi-Utilizator — Specificație Iterație (Actualizată)

**Domeniu Proiect:** Actualizare Protocol Hardware & Îmbunătățire Funcționalități Aplicație Android

---

## 🔧 CERINȚE HARDWARE (Responsabilitatea Dumneavoastră)

### Protocol Curent (Utilizator Singular)

#### Mesaj trimis via Bluetooth:
```

0xAB3311\n

```
- Un singur cod hex per transmisie  
- Reprezintă postura unui singur utilizator  
- Terminat cu newline

---

## 🔄 Protocol Nou (Packet Multi-Utilizator)

### Format Mesaj
```

sensor001;0xAB3311\n
sensor002;0xEF0112\n
sensor003;0xBA3311\n
sensor001;0xBA3311\n
END_PACKET\n

```

### Modificări Protocol

1. **Adaugă ID Copil**  
Prefixați fiecare citire cu un identificator unic (UUID, hash etc.) urmat de `;`  
Exemplu:
```

5d6d75ee-b6c8-42d4-a233-b13d137fea38;0xAB3311

```

2. **Terminator Packet (OBLIGATORIU)**
```

END_PACKET\n

```
Packet-ul nu este procesat fără acest terminator.

3. **Citiri Multiple per Packet**  
Packet-ul poate include 1…∞ citiri.  
Se pot repeta ID-uri în același pachet.

---

### Exemplu Packet
```

// Packet 1 (3 copii, 4 citiri în total)
sensor001;0xAB3311\n
sensor002;0xEF0112\n
sensor003;0xBA3311\n
sensor001;0xAB3311\n
END_PACKET\n

[așteptare 2 secunde]

// Packet 2 (2 copii, 2 citiri)
sensor001;0xBA3311\n
sensor002;0xAB3311\n
END_PACKET\n

```

---

# 📱 FUNCȚIONALITĂȚI NOI APLICAȚIE

## 1. Colectare Multi-Utilizator
- Telefonul Android colectează date pentru un număr **nelimitat** de persoane.
- Identificarea se face prin ID de la hardware.
- Stocare automată separată per persoană.

**Beneficiu:** un singur dispozitiv hardware → monitorizare la nivel de clădire.

---

## 2. Interfață Agregator Date (Debug Friendly)

### Tab 1: Monitor Mesaje Live
- Log în timp real
- Timestamp + posture
- Număr mesaje per persoană
- **Color coding + risc**
  - Roșu → căzut (⚠️ risc mare)
  - Galben → mers / stat în picioare (risc mediu)
  - Verde → stat pe scaun (risc mic)

### Tab 2: Vizualizator Postură Live
- Selectarea oricărei persoane
- Animație postură live
- Debug individual
- **Adaptare video în funcție de persona monitorizată**

---

## 3. Sistem Denumire Persoane

- Logarea copiilor / persoanelor se face **prin Agregator**
- Se va deschide o fereastră în care se asociază:
**UUID → nume persoană**
- Exemplu:
```

5d6d75ee-b6c8-42d4-a233-b13d137fea38 → Ion Popescu

```
- Numele se sincronizează la toate conturile de supraveghere conectate la acest agregator.

---

## 4. Dashboard Supervisor Îmbunătățit

Afișează:
- Nume
- Postură curentă
- Timp ultimă actualizare
- **Nivel risc**

### Niveluri Risc
| Postură | Nivel Risc |
|--------|-----------|
| Căzut | ❗ Mare |
| Mers / În picioare | ⚠️ Mediu |
| Stat pe scaun | 🟢 Mic |

---

## 5. Vizualizare Detalii Persoană Individuală
- Ecran dedicat unui singur utilizator
- Animație postură live
- Timeline istoric
- Statistici:
  - timp în picioare
  - timp așezat
  - timp mers
- Analiza consum energie
- **Adaptare video individuală**

---

## 6. Performanță Îmbunătățită
- Upload batch cloud (nu pe fiecare linie)
- Interogări agregate pentru mai mulți utilizatori
- Latență redusă notificări live

---

# ✨ FUNCȚIONALITĂȚI NOI (Solicitate)

### 🔹 A. Adaptare Video Per Persoană
- UI schimbă animațiile/video în funcție de postura ID-ului selectat
- Modul dedicat dacă se deschide profilul persoanei

### 🔹 B. Sign Out → Deconectare Bluetooth
- Apăsarea butonului **Sign Out**:
  - Conexiunea Bluetooth se închide imediat
  - listener-ele sunt curățate
  - aplicația revine în starea inițială

### 🔹 C. Sistem Risc
Regulă simplă:
- Căzut → risc mare
- În picioare / deplasare → risc mediu
- Stat pe scaun → risc mic

Implementat în:
- Feed live
- Dashboard
- Notificări

### 🔹 D. Logare prin Agregator
- NU există conturi pentru copii
- Doar UUID
- Agregatorul introduce nume friendly
- Mapping sincronizat automat cloud

### 🔹 E. Vizualizare Multi-Supervisor
- Datele de la o persoană pot fi vizualizate simultan de mai mulți supervisori
- Niciun limit numeric

---

# 🧩 PREZENTARE ARHITECTURĂ

## Sistem Actual
```

1 Telefon <-> 1 Dispozitiv Bluetooth <-> 1 Persoană

```

## Sistem Nou
```

1 Telefon Agregator <-> 1 Dispozitiv Bluetooth <-> Persoane Multiple
↓
Bază Cloud
↓
Supraveghetori Multipli

```

---

# 👤 ROLURI UTILIZATORI

## 🔷 Cont Agregator (Colectare)
- Conexiune hardware Bluetooth
- Primește pachete multi-ID
- Gestionare nume persoane
- Upload cloud
- Dashboard de debugging
- **Sign Out = întrerupe conexiunea Bluetooth**

## 🔷 Cont Supervisor
- Conectat la un agregator
- Vizualizează ansamblu
- Alerte risc
- Vizualizare detaliu persoană
- **Mai mulți supervisori pot vedea aceeași persoană**

## 🔷 Copil / Persoană Monitorizată
- Nu are cont
- Trimite doar ID via hardware
- Apare cu numele atribuit de Agregator

---

# 🔁 FLUX DATE

```

Hardware-ul dumneavoastră
↓ transmite packeturi multi-ID
Telefon Agregator
↓ procesează + mapare nume + upload batch
Bază de Date Cloud
↓ sincronizare timp real
Telefoane Supervisor (multipli)
↓ vizualizare + risc + video individual

```

---

# 📅 TIMELINE + COST

**Timeline:** 3–4 săptămâni  
**Cost Total:** €3.000

### Termeni Plată:
- 10% avans (€300) — demarare dezvoltare
- 90% (€2.700) — livrare + testare

Include:
- Implementare completă
- Testare hardware/software
- Documentație
- Suport post-deployment

---

**Upgrade-ul transformă sistemul dumneavoastră din single-user → multi-user, cu modificări minime în hardware și expansiune majoră în software.**
