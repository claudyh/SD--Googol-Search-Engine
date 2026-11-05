## Googol: Web Search Engine
*in Java (Spring Boot) with RMI, UDP Multicast and WebSockets*

&nbsp;

### 📋 **Project Description**

---

Googol is a distributed web search engine, with a subset of the features of services like Google: automatic indexing (web crawler) and search. Users can suggest URLs to be indexed, and the system then recursively indexes every link found on each page. A search returns the pages containing the searched words, showing the title, URL and a short text citation, grouped in pages of 10 results.

The project was developed in two stages:

&nbsp;

**⇨ Stage 1 - Distributed Backend (console client)**

- **Downloaders:** web crawlers running in parallel that fetch URLs from a shared queue (over *RMI*), parse each page with *jsoup* and send the extracted words, links and metadata to the Barrels via *UDP Multicast*.
- **Index Storage Barrels:** replicated servers that receive the multicast data, keep the inverted index and persist it to object files (`.ser`). Each Barrel can be started or stopped at any time, since the system keeps working while at least one is active. A Barrel saves its index when it shuts down and restores it when it starts again.
- **Multicast reliability:** since *UDP Multicast* can lose messages, the Gateway counts how many multicast messages each Barrel received and compares them every 15 seconds. If a Barrel is behind, it loads the missing data from the Barrel with the most messages, so all replicas end up with the same information.
- **RMI Gateway:** the single entry point for clients. It keeps a registry of the active Barrels and chooses one for each search, so clients never talk to Barrels directly.
- **URL Queue:** shared queue of URLs waiting to be visited, accessed by the Gateway and the Downloaders through RMI.
- **RMI Client:** simple console menu that calls the Gateway's remote methods (index a URL, search by words, list the pages that link to a URL, admin information).

&nbsp;

**⇨ Stage 2 - Web Interface & API Integration**

- **Spring Boot + Thymeleaf (MVC):** web application that acts as an RMI client of the Gateway, with the same features as the console client.
- **Real-time admin information with WebSockets:** top 10 searches, active Barrels and their response times, pushed to every open browser whenever something changes (no periodic refresh).
- **Hacker News integration (REST):** after a search, the matching top stories' URLs are sent to be indexed.
- **Generative AI integration (REST):** the results page shows a short description of the searched terms, generated with Google Gemini (Vertex AI).

&nbsp;

### 🧩 **Architecture**

---

<img width="546" height="495" alt="image" src="https://github.com/user-attachments/assets/2b138dff-02e6-4b96-91e4-d08a777cd396" />

**Note:** Default addresses are defined in `properties.txt`.

&nbsp;

### ⚙️ **How to Run**

---

**Requirements:** Java 17 and Maven. The *jsoup* and *json* libraries are declared in `pom.xml`.

**Note:** Refer to the `README.md` file on `src/main/java/com/example/demo` for complete run instructions.

&nbsp;

**1 - Backend (start the Gateway first):**

*java Gateway.java*

*java Barrel.java* (as many as needed, on different consoles)

*java Downloader.java* (as many as needed, on different consoles)

*java Client.java* (console client - optional)

&nbsp;

**2 - Web Interface (after the Gateway is running):**

*./mvnw spring-boot:run*

Then open `http://localhost:8080/googol`.

**Gemini setup:** the AI description uses Google Cloud Vertex AI. Set your own project id and region in `WebController.java` and authenticate with `gcloud auth application-default login`. Without it the search still works, only the AI description is not shown.

&nbsp;

---

**NOTE:** The Hacker News integration only checks the first 10 top stories and does not index all the stories of a given Hacker News user. This project was developed within the scope of a Computer Science course by Cláudia Torres and Maria João Rosa.
