# Étude de cas – Benchmark de performances des Web Services REST

Ce dépôt contient trois variantes d’une même API CRUD (Category / Item) pour comparer l’impact des choix de stack REST sur la latence, le débit et l’empreinte JVM.

- Variante A — Jersey (JAX‑RS) + Spring Boot + JPA/Hibernate (port 8081)
- Variante C — Spring Boot @RestController + JPA/Hibernate (port 8082)
- Variante D — Spring Data REST (exposition des repositories, HAL) + JPA/Hibernate (port 8083)

Base commune:
- Java 21, PostgreSQL 14+, HikariCP (maxPoolSize=20, minIdle=10)
- Flyway (schema identique), JPA `ddl-auto=validate`, L2 cache désactivé
- Observabilité: Actuator + Micrometer Prometheus, Prometheus + Grafana (provisionnés), InfluxDB v2 pour JMeter
- Jeu de données CSV (2k catégories, 100k items) avec chargeur `profile=init`


## 1) Démarrage de l’infrastructure (DB + observabilité)

Prérequis: Docker Desktop

```bash
# À la racine du repo
docker compose up -d
# Services exposés:
# - PostgreSQL: 5432
# - InfluxDB v2: http://localhost:8086 (org: perf, bucket: jmeter, token: admin-token)
# - Prometheus: http://localhost:9090
# - Grafana:    http://localhost:3000 (admin / admin)
```

Prometheus scrappe automatiquement les variantes sur `host.docker.internal:8081/8082/8083` (voir `./ops/prometheus/prometheus.yml`).

Grafana est pré-provisionné avec:
- Datasources: Prometheus et InfluxDB v2 (JMeter)
- Dashboards: JVM (Micrometer) et JMeter (InfluxDB)


## 2) Lancer les variantes A, C, D

Chacune utilise la même DB `benchdb` (utilisateur `bench` / mdp `bench`). Les scripts Flyway créent le schéma si nécessaire.

- Variante C (@RestController, 8082):
```bash
mvn -f rest-controller spring-boot:run                        # sans import CSV
mvn -f rest-controller "-Dspring-boot.run.profiles=init" spring-boot:run  # avec import CSV (PowerShell)
# Alternatives PowerShell:
#   mvn --% -f rest-controller spring-boot:run -Dspring-boot.run.profiles=init
#   $env:SPRING_PROFILES_ACTIVE=init; mvn -f rest-controller spring-boot:run
```
Sanity checks:
- http://localhost:8082/actuator/health
- http://localhost:8082/actuator/prometheus
- http://localhost:8082/items?page=0&size=50
- http://localhost:8082/items?categoryId=1&page=0&size=50
- http://localhost:8082/categories/1/items?page=0&size=50

- Variante D (Spring Data REST, 8083):
```bash
mvn -f spring-data-rest spring-boot:run
mvn -f spring-data-rest "-Dspring-boot.run.profiles=init" spring-boot:run  # PowerShell
# Alternatives PowerShell:
#   mvn --% -f spring-data-rest spring-boot:run -Dspring-boot.run.profiles=init
#   $env:SPRING_PROFILES_ACTIVE=init; mvn -f spring-data-rest spring-boot:run
```
Sanity checks:
- http://localhost:8083/actuator/health
- http://localhost:8083/actuator/prometheus
- http://localhost:8083/items?page=0&size=50
- http://localhost:8083/items/1
- http://localhost:8083/items/search/byCategoryId?categoryId=1&page=0&size=50
- http://localhost:8083/items/search/byCategoryJoin?cid=1&page=0&size=50
- http://localhost:8083/categories/1 (suivre les liens `_links` HAL)

- Variante A (Jersey / JAX‑RS, 8081):
```bash
mvn -f jersey spring-boot:run
mvn -f jersey "-Dspring-boot.run.profiles=init" spring-boot:run  # PowerShell
# Alternatives PowerShell:
#   mvn --% -f jersey spring-boot:run -Dspring-boot.run.profiles=init
#   $env:SPRING_PROFILES_ACTIVE=init; mvn -f jersey spring-boot:run
```
Sanity checks:
- http://localhost:8081/actuator/health
- http://localhost:8081/actuator/prometheus
- http://localhost:8081/items?page=0&size=50
- http://localhost:8081/items?categoryId=1&page=0&size=50
- http://localhost:8081/categories/1/items?page=0&size=50
- http://localhost:8081/categories?page=0&size=50


## 3) Dashboards Grafana

Les fichiers sont préprovisionnés et montés via Docker Compose:
- Datasources: `ops/grafana/provisioning/datasources/datasources.yml`
- Dashboards:  `ops/grafana/provisioning/dashboards/dashboards.yml`
- JSON:        `ops/grafana/provisioning/dashboards/json/jvm-dashboard.json`, `.../jmeter-dashboard.json`

Accéder à Grafana: http://localhost:3000
- Dashboard JVM: "Bench - JVM (Micrometer)"
- Dashboard JMeter: "Bench - JMeter (InfluxDB v2)"


## 4) Plans JMeter (.jmx) fournis

Dossier: `./jmeter`
- `read-heavy.jmx` — Mix: 50% GET /items?page=50, 20% GET /items?categoryId, 20% GET /categories/{id}/items, 10% GET /categories?page. Threading: 50 (par défaut), ramp-up 60s, durée 10 min. Ajustez `THREADS`/`DURATION_SECONDS` dans les variables du Test Plan.
- `join-filter.jmx` — 70% GET /items?categoryId, 30% GET /items/{id}. Threads 60, 8 min.
- `mixed.jmx` — 40% GET items, 20% POST items (1 KB), 10% PUT items (1 KB), 10% DELETE items, 10% POST categories, 10% PUT categories. Threads 50, 10 min. Utilise `data/items_payload_small.jsonl`.
- `heavy-body.jmx` — 50% POST items (5 KB), 50% PUT items (5 KB). Threads 30, 8 min. Utilise `data/items_payload_large.jsonl`.

Chaque plan inclut:
- HTTP Request Defaults (UTF‑8)
- CSV Data Set Config:
  - `../data/category_ids.csv` (colonne `CATEGORY_ID`)
  - `../data/item_ids.csv`     (colonne `ITEM_ID`)
  - `../data/items_payload_small.jsonl` (variable `ITEM_JSON`)
  - `../data/items_payload_large.jsonl` (variable `ITEM_JSON5K`)
- Backend Listener InfluxDB v2 (URL http://localhost:8086, org `perf`, bucket `jmeter`, token `admin-token`).

Paramétrage de la cible:
- Par défaut `BASE_URL = http://localhost:8082` (variante C). Pour tester A ou D, modifiez la variable `BASE_URL` au niveau du Test Plan:
  - A (Jersey): `http://localhost:8081`
  - C (@RestController): `http://localhost:8082`
  - D (Spring Data REST): `http://localhost:8083`

Bonnes pratiques:
- Désactiver les Listeners lourds (View Results Tree…) pendant les runs.
- Vérifier via Grafana (JMeter dashboard) la réception des métriques (RPS, erreurs, percentiles).


## 5) Mesure anti‑N+1

- Variantes A et C: flag `app.items.join-fetch.enabled=true|false` (dans `application.properties` ou variable d’environnement Spring) pour que `GET /items?categoryId=...` utilise la requête JOIN FETCH.
- Variante D: utiliser l’endpoint `GET /items/search/byCategoryJoin?cid=...` (vs `byCategoryId`) pour mesurer l’effet de l’élimination du N+1.

Conserver la pagination `page=&size=` identique dans les runs (par défaut `size=50`).


## 6) Procédure type pour un scénario (ex: READ-heavy)

1. Démarrer l’infra Docker (DB, Prometheus, Influx, Grafana).
2. Lancer UNE seule variante cible (A OU C OU D). Laisser les autres arrêtées.
3. Vérifier `/actuator/health` et `/actuator/prometheus` de la variante.
4. Ouvrir Grafana et le dashboard "Bench - JVM (Micrometer)".
5. Ouvrir JMeter, charger `jmeter/read-heavy.jmx`.
6. Régler `BASE_URL` selon la variante; régler `THREADS`/`DURATION_SECONDS` si besoin.
7. Lancer le test. Surveiller Grafana (JVM + JMeter). Noter RPS, p50/p95/p99, Err%.
8. À la fin, exporter des captures / CSV depuis Grafana si nécessaire.
9. Répéter pour 100 threads, puis 200 threads (ou utilisez 3 Thread Groups configurés si souhaité).
10. Répéter pour les autres variantes (redémarrer l’appli cible pour isolation).


## 7) Tableaux résultats à compléter (T0 → T7)

Copiez-collez et remplissez après vos runs.

### T0 — Configuration matérielle & logicielle

| Élément | Valeur |
|---|---|
| Machine (CPU, cœurs, RAM) | |
| OS / Kernel | |
| Java version | |
| Docker/Compose versions | |
| PostgreSQL version | |
| JMeter version | |
| Prometheus / Grafana / InfluxDB | |
| JVM flags (Xms/Xmx, GC) | |
| HikariCP (min/max/timeout) | |

### T1 — Scénarios

| Scénario | Mix | Threads (paliers) | Ramp-up | Durée/palier | Payload |
|---|---|---:|---:|---:|---|
| READ-heavy (relation) | 50% items list, 20% items by category, 20% cat→items, 10% cat list | 50→100→200 | 60s | 10 min | – |
| JOIN-filter | 70% items?categoryId, 30% item id | 60→120 | 60s | 8 min | – |
| MIXED (2 entités) | GET/POST/PUT/DELETE items + categories | 50→100 | 60s | 10 min | 1 KB |
| HEAVY-body | POST/PUT items 5 KB | 30→60 | 60s | 8 min | 5 KB |

### T2 — Résultats JMeter (par scénario et variante)

| Scénario | Mesure | A : Jersey | C : @RestController | D : Spring Data REST |
|---|---|---:|---:|---:|
| READ-heavy | RPS | | | |
|  | p50 (ms) | | | |
|  | p95 (ms) | | | |
|  | p99 (ms) | | | |
|  | Err % | | | |
| JOIN-filter | RPS | | | |
|  | p50 (ms) | | | |
|  | p95 (ms) | | | |
|  | p99 (ms) | | | |
|  | Err % | | | |
| MIXED | RPS | | | |
|  | p50 (ms) | | | |
|  | p95 (ms) | | | |
|  | p99 (ms) | | | |
|  | Err % | | | |
| HEAVY-body | RPS | | | |
|  | p50 (ms) | | | |
|  | p95 (ms) | | | |
|  | p99 (ms) | | | |
|  | Err % | | | |

### T3 — Ressources JVM (Prometheus)

| Variante | CPU proc. (%) moy/pic | Heap (Mo) moy/pic | GC time (ms/s) moy/pic | Threads actifs moy/pic | Hikari (actifs/max) |
|---|---:|---:|---:|---:|---:|
| A : Jersey | | | | | |
| C : @RestController | | | | | |
| D : Spring Data REST | | | | | |

### T4 — Détails par endpoint (JOIN-filter)

| Endpoint | Variante | RPS | p95 (ms) | Err % | Observations |
|---|---|---:|---:|---:|---|
| GET /items?categoryId= | A | | | | JOIN/Projection/N+1 |
|  | C | | | | |
|  | D | | | | |
| GET /categories/{id}/items | A | | | | |
|  | C | | | | |
|  | D | | | | |

### T5 — Détails par endpoint (MIXED)

| Endpoint | Variante | RPS | p95 (ms) | Err % | Observations |
|---|---|---:|---:|---:|---|
| GET /items | A | | | | |
|  | C | | | | |
|  | D | | | | |
| POST /items | A | | | | |
|  | C | | | | |
|  | D | | | | |
| PUT /items/{id} | A | | | | |
|  | C | | | | |
|  | D | | | | |
| DELETE /items/{id} | A | | | | |
|  | C | | | | |
|  | D | | | | |
| GET /categories | A | | | | |
|  | C | | | | |
|  | D | | | | |
| POST /categories | A | | | | |
|  | C | | | | |
|  | D | | | | |

### T6 — Incidents / erreurs

| Run | Variante | Type d’erreur (HTTP/DB/timeout) | % | Cause probable | Action corrective |
|---|---|---|---:|---|---|

### T7 — Synthèse & conclusion

| Critère | Meilleure variante | Écart (justifier) | Commentaires |
|---|---|---|---|
| Débit global (RPS) | | | |
| Latence p95 | | | |
| Stabilité (erreurs) | | | |
| Empreinte CPU/RAM | | | |
| Facilité d’expo relationnelle | | | |


## 8) Notes importantes

- Assurez-vous qu’une seule variante est lancée pendant un run pour isoler les mesures.
- Les payloads POST/PUT 1 KB et 5 KB sont fournis sous forme JSONL. Les contrôleurs valident les champs requis.
- Le port cible peut être surchargé via la variable `BASE_URL` dans les plans JMeter.
- Si vous exécutez les applis à l’intérieur de Docker au lieu de l’hôte, ajustez Prometheus (targets) et BASE_URL en conséquence.


## 9) Arborescence utile

- `jersey/` (A), `rest-controller/` (C), `spring-data-rest/` (D)
- `data/` — CSV et JSONL (ids & payloads)
- `jmeter/` — plans des scénarios
- `ops/prometheus/prometheus.yml` — scrape configs
- `ops/grafana/provisioning/` — datasources + dashboards
- `docker-compose.yml` — stack observabilité et DB

Bon benchmark !
