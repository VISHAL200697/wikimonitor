# WikiMonitor

WikiMonitor is a comprehensive tool designed to monitor Wikimedia's RecentChanges stream in real-time. It empowers users to define custom filter rules using Spring Expression Language (SpEL) to automatically flag specific edits, detect vandalism, or track specific patterns across Wikimedia projects.

## Features

-   **Real-time Monitoring**: Connects to the global Wikimedia `recentchange` stream via Server-Sent Events (SSE).
-   **Custom Abuse Filters**: Define complex filter rules using SpEL (Spring Expression Language) to analyze edits as they happen.
-   **Multi-User Support**: Individual users can maintain their own set of private filter rules.
-   **OAuth2 Integration**: Secure sign-in and authentication using Wikimedia OAuth2 (via Meta-Wiki or other MediaWiki instances).
-   **Live Updates**: Filtered events are broadcasted to the web interface in real-time.
-   **Lightweight Storage**: Uses SQLite for efficient and easy-to-manage data storage.
-   **Responsive UI**: Modern web interface for managing filters and viewing live streams.

## Prerequisites

-   **Java**: Version 17 or higher.
-   **Diff Utilities**: `diff` command line tool (usually available on Linux/Unix).
-   **Wikimedia Account**: Required for OAuth2 authentication.

## Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/wikiconnect.git
    cd wikiconnect/wikimonitor
    ```

2.  **Configure Environment Variables:**
    Create a `.env` file in the root directory (same level as `pom.xml`) with the following keys:
    ```ini
    # Wikimedia OAuth2 Configuration
    ACCESS_TOKEN=your_wikidata_access_token
    MEDIAWIKI_CLIENT_ID=your_oauth_client_id
    MEDIAWIKI_CLIENT_SECRET=your_oauth_client_secret
    ```
    *Note: You need to register an OAuth2 consumer on Meta-Wiki or your target MediaWiki instance to obtain the Client ID and Secret. The `ACCESS_TOKEN` is used for initial API interactions or bot actions.*

3.  **Build the project:**
    ```bash
    mvn clean install
    ```

## Usage

1.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
    Alternatively, run the built jar file:
    ```bash
    java -jar target/wikimonitor.jar
    ```

2.  **Access the Dashboard:**
    Open your web browser and navigate to `http://localhost:8000`.

3.  **Login:**
    Click the login button to authenticate using your Wikimedia account via OAuth2.

4.  **Define Filters:**
    Navigate to the settings or filter management page to add SpEL-based rules.
     *Example Rule:*
    ```spel
    # Flag edits by specific user or containing specific text
    user == 'VandalUser' || title matches '.*Spam.*'
    ```

## Configuration

The application uses `src/main/resources/application.properties` for core settings:

-   **Server Port**: `server.port=8000`
-   **Database**: SQLite (`wikimonitor.db`)
-   **JPA/Hibernate**: Configured for SQLite dialect.

## Project Structure

-   `src/main/java`:
    -   `org.qrdlife.wikiconnect.wikimonitor`: Main application package.
    -   `controller`: Spring MVC and REST controllers for handling web requests.
    -   `service`: Core business logic including `WikiStreamService` (SSE), `AbuseFilterService` (SpEL filtering), and `OAuth2Service`.
    -   `model`: JPA Entities (`User`, `RecentChange`).
-   `src/main/resources`:
    -   `templates`: Thymeleaf templates for the frontend.
    -   `static`: Static assets (CSS, JS).
    -   `application.properties`: Configuration file.

## Technologies Used

-   **Backend**: Java 17, Spring Boot 4.0.2
-   **Database**: SQLite
-   **Streaming**: OkHttp (SSE)
-   **Authentication**: ScribeJava (OAuth2)
-   **Templating**: Thymeleaf
-   **Utilities**: Java Diff Utils, Jsoup, Caffeine (Caching), Dotenv

## License

This project is licensed under the GNU General Public License v3.0 (GPLv3). See the `LICENSE` file for details.
