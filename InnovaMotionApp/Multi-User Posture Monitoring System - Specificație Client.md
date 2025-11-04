# Sistem de Monitorizare Postură Multi-Utilizator - Specificație Iteratie

**Domeniu Proiect:** Actualizare Protocol Hardware & Îmbunătățire Funcționalități Aplicație Android

---

## CERINȚE HARDWARE (Responsabilitatea Dumneavoastră)

### Protocol Curent (Utilizator Singular)
#### Mesaj trimis via Bluetooth:
```
0xAB3311\n
```
- Un singur cod hex per transmisie
- Reprezintă postura unui singur utilizator
- Terminat cu newline

### Protocol Nou (Packet Multi-Utilizator)

#### Format Mesaj
```
sensor001;0xAB3311\n
sensor002;0xEF0112\n
sensor003;0xBA3311\n
sensor001;0xBA3311\n
END_PACKET\n
```

**Modificări Protocol:**
1. **Adaugă ID Copil:** Prefixați fiecare citire cu un identificator unic(UUID, hash, etc.) + punct și virgulă
   - Exemplu: `5d6d75ee-b6c8-42d4-a233-b13d137fea38;0xAB3311`
   - Folosiți orice format ID preferați

2. **Terminator Packet:** Terminați fiecare transmisie cu `END_PACKET\n` pe o linie nouă
   - Aceasta este OBLIGATORIE - fără ea, datele nu vor fi procesate
   
3. **Citiri Multiple:** Puteți trimite de la 1 la un număr nelimitat de citiri per packet
   - Aceeași persoană poate apărea de mai multe ori dacă este necesar

#### Exemplu Transmisie Packet
```
// Packet 1 (3 copii, 4 citiri în total)
sensor001;0xAB3311\n     ← Copil 001: În picioare
sensor002;0xEF0112\n     ← Copil 002: Căzut
sensor003;0xBA3311\n     ← Copil 003: Mers
sensor001;0xAB3311\n     ← Copil 001: Încă în picioare (copil duplicat - OK)
END_PACKET\n             ← Terminator obligatoriu

[așteptare 2 secunde]

// Packet 2 (2 copii, 2 citiri)
sensor001;0xBA3311\n     ← Copil 001: Acum merge
sensor002;0xAB3311\n     ← Copil 002: Acum în picioare
END_PACKET\n             ← Terminator obligatoriu
```

---

## FUNCȚIONALITĂȚI NOI APLICAȚIE

### 1. Colectare Date Multi-Utilizator
**Ce face:**
- Un telefon Android colectează date pentru un număr nelimitat de persoane monitorizate (anterior: doar 1 persoană)
- Fiecare persoană identificată prin ID de la hardware-ul dumneavoastră (sensor001, sensor002, etc.)
- Toate datele organizate și stocate automat per persoană

**Beneficiu:** Un dispozitiv de colectare poate servi o clădire/facilitate întreagă

---

### 2. Interfață Agregator Date (Telefon Colectare - Debugging Friendly)

**Tab 1: Monitor Mesaje Live**
- Logare în timp real al tuturor mesajelor primite
- Vedeți ce persoană a trimis ce citire/hexa și când
- Număr de mesaje per persoană
- Alerte codificate color (roșu pentru căderi)
- Depanați instantaneu problemele de colectare

**Tab 2: Vizualizator Postură Live**
- Selectați orice persoană din dropdown
- Vedeți postura lor curentă cu animație
- Depanați datele individuale ale persoanei
- Verificați citirile corecte

**De ce:** Vizibilitate clară asupra datelor colectate și de la cine

---

### 3. Sistem Denumire Persoane

**Ce face:**
- Atribuiți nume prietenoase ID-urilor de senzori
- Exemplu: "5d6d75ee-b6c8-42d4-a233-b13d137fea38" devine "Ion Popescu"
- Numele se sincronizează automat pe dispozitivele supervisor

**Beneficiu:** Monitorizare lizibilă pentru oameni în loc de ID-uri criptice

---

### 4. Dashboard Supervisor Îmbunătățit

**Ce face:**
- Vedeți TOATE persoanele monitorizate simultan (anterior: una)
- Afișează: Nume, postură curentă, timpul ultimei actualizari

**Beneficiu:** Monitorizați întreaga facilitate de pe un singur ecran

---

### 5. Vizualizare Detalii Persoană Individuală (Reutilizam vizualizarea unei singure persoane din versiunea anteriara a aplicatiei)

**Ce face:**
- Ecran complet pentru o persoană
- Animație postură live
- Timeline istoric
- Statistici (timp în picioare, așezat, mers, etc.)
- Analiza consumului energiei

**Beneficiu:** Analiză aprofundată a activității unei persoane specifice

---

### 6. Performanță Îmbunătățită

**Îmbunătățiri tehnice (invizibile pentru utilizator):**
- Stocare cloud mai eficientă (upload-uri batch/pachet), in loc de linie cu linie
- Interogare unică pentru toți copiii (vs una per copil)

---

## PREZENTARE GENERALĂ ARHITECTURĂ SISTEM

### Înainte (Curent)
```
1 Telefon <-> 1 Dispozitiv Bluetooth <-> 1 Persoană
```

### După (Nou)
```
1 Telefon <-> 1 Dispozitiv Bluetooth <-> Persoane Multiple
              ↓
         Stocare Cloud
              ↓
    Supervisori Multipli (toți văd toate persoanele)
```

---

## ROLURI UTILIZATORI

### Cont "Agregator" (Colectare Date) - inlocuieste fostul cont "Supervised"
- Se conectează la hardware-ul Bluetooth
- Primește packet-uri de la toate persoanele
- Gestionează nume/metadata persoane
- Upload la cloud
- Vizualizează log date brute + vizualizare debug live

### Cont "Supervisor" (Monitorizare)
- Se leagă de un agregator
- Vede toate persoanele de la acel agregator
- Primește actualizări în timp real
- Primește alerte pentru căderi
- Vizualizează detalii persoane individuale

### Copil/Persoană Monitorizată
- Nu este un cont efectiv
- Doar un ID trimis de hardware-ul dumneavoastră
- Apare în dashboard-ul supervisor cu nume prietenos

---

## REZUMAT FLUX DATE

```
Hardware-ul Dumneavoastră
    ↓ (trimite packet-uri cu ID-uri persoane)
Telefon Agregator
    ↓ (stochează local + upload)
Bază de Date Cloud
    ↓ (sincronizare timp real)
Telefoane Supervisor
    ↓ (afișare în dashboard)
Personal Îngrijire Monitorizare
```

---

## TIMELINE PROIECT & INVESTIȚIE

**Timeline:** 3 - 4 săptămâni

**Cost Total:** €3,000

**Termeni Plată:**
- 10% avans (€300) - pentru a începe dezvoltarea
- 90% final (€2700) - la livrare și testare

**Include:**
- Implementare completă a tuturor funcționalităților
- Testare și documentație
- suport post-deployment

---

**Aceast upgrade transformă sistemul dumneavoastră de la monitorizare single-user la monitorizare multi-user cu modificări hardware minime și îmbunătățirile software explicate mai sus.**

