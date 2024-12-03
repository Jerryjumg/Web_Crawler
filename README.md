
# Web Crawler

## Introduction
The project is designed to crawl the web starting from three seed URLs:
- [The Metropolitan Museum of Art](https://www.metmuseum.org/)
- [National Park Service](https://www.nps.gov/index.htm)
- [Museum of Fine Arts, Boston](https://www.mfa.org/)

The crawler efficiently handles multiple requests in parallel using asynchronous operations and a thread-safe priority queue. By leveraging advanced concurrency techniques and integrating with a Neo4j graph database, it indexes and analyzes web content, providing valuable insights into the structure and connections of web pages.

## Features
- **Asynchronous Operations**: Utilizes Java's `CompletableFuture` to perform multiple web requests in parallel, ensuring efficient resource usage and faster crawling.
- **Priority-Based URL Processing**: Manages URLs using a `PriorityBlockingQueue`, processing more relevant or important URLs first based on a custom heuristic.
- **Thread-Safe Operations**: Utilizes concurrent data structures like `PriorityBlockingQueue` and `ConcurrentHashMap` for safe operations in a multi-threaded environment.
- **Robust Error Handling**: Logs any errors encountered during the crawling process using `log4j`, ensuring the application does not terminate unexpectedly.
- **Neo4j Integration**: Stores URLs and their relationships in a Neo4j graph database for efficient querying and analysis of the web structure.
- **Scalability and Extensibility**: Designed to handle a growing number of URLs and web pages, with a modular architecture that allows for easy feature extensions and heuristic updates.

## Neo4j Integration

The web crawler integrates with Neo4j to store the URLs and their relationships as a graph. Each URL is a **node**, and each link between pages is represented as a **relationship** between those nodes. This integration allows us to easily visualize and analyze the interconnections between crawled pages. Using Neo4j, we can efficiently perform graph-based queries to explore the relationships between pages and gain insights into the structure of the web.

<img width="1512" alt="Screenshot 2024-12-02 at 10 43 11 PM" src="https://github.com/user-attachments/assets/464f53a2-387d-47a4-9001-9377933be966">

---
## Swagger UI

To interact with the Web Crawler API, you can use the Swagger UI, which provides a user-friendly interface to explore and test all available endpoints. Swagger generates API documentation and allows you to send HTTP requests directly from the browser.

### How to Access Swagger UI:
You can access the Swagger UI by navigating to the following link:
```
http://localhost:8080/swagger-ui/index.html#/
```
### What Swagger Displays:
- **API Documentation**: It automatically generates documentation for the Web Crawler API, listing all available endpoints, their descriptions, and the HTTP methods they support (GET, POST, etc.).
- **Interactive Interface**: Swagger allows you to interact with the API by providing input data for each endpoint and seeing the response directly within the UI.
- **OpenAPI Specification**: The API follows the OpenAPI specification, providing structured and standardized API documentation that is both human-readable and machine-readable.
<img width="1512" alt="Screenshot 2024-12-02 at 10 56 32 PM" src="https://github.com/user-attachments/assets/99f42106-215d-4c7a-aa0c-f5684a219869">
<img width="1512" alt="Screenshot 2024-12-02 at 10 57 36 PM" src="https://github.com/user-attachments/assets/41aeaa7f-a245-49b6-84b5-f48336c61a8b">

### How it Works:
- The Swagger UI dynamically reads the OpenAPI specification, which is a description of the API in a JSON or YAML format. This specification defines the structure of the API, the parameters for each endpoint, and the expected responses.
- You can use Swagger to:
  - Test the `/crawl/start` endpoint by sending a POST request to initiate the crawling process.
  - View other API endpoints (if implemented) to manage and monitor the web crawling process.


## Installation
### Clone the Repository
Clone this repository to your local machine:
```bash
git clone https://github.com/yourusername/web-crawler.git
cd web-crawler
```

### Set up Neo4j
Install Neo4j and start the database. Ensure Neo4j is running on `localhost:7687` with the default username `neo4j` and password `password`.

### Build the Project
Build the project using Maven:
```bash
./mvnw clean install
```

### Run the Application
Run the application with Spring Boot:
```bash
./mvnw spring-boot:run
```

## Usage
To start the crawling process, send a POST request to the following endpoint using Postman or any other HTTP client:
```bash
POST http://localhost:8080/crawl/start
```

## Configuration
You can configure the seed URLs and other settings in the `application.properties` file located at `src/main/resources/application.properties`.

## License
This project is licensed under the Northeastern University License. See the LICENSE file for details.

## Contributing
Contributions are welcome! Feel free to open an issue or submit a pull request for any improvements or bug fixes.

## Contact
For any questions or inquiries, please contact:
- chakola.j@northeastern.edu
- zheng.jiey@northeastern.edu

