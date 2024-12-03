# Web Crawler


Introduction
The project is designed to crawl the web starting from three seed URLs:

The Metropolitan Museum of Art
National Park Service
Museum of Fine Arts, Boston
The crawler efficiently handles multiple requests in parallel using asynchronous operations and a thread-safe priority queue. By leveraging advanced concurrency techniques and integrating with a Neo4j graph database, it indexes and analyzes web content, providing valuable insights into the structure and connections of web pages.

Features
Asynchronous Operations: Utilizes Java's CompletableFuture to perform multiple web requests in parallel, ensuring efficient resource usage and faster crawling.
Priority-Based URL Processing: Manages URLs using a PriorityBlockingQueue, processing more relevant or important URLs first based on a custom heuristic.
Thread-Safe Operations: Utilizes concurrent data structures like PriorityBlockingQueue and ConcurrentHashMap for safe operations in a multi-threaded environment.
Robust Error Handling: Logs any errors encountered during the crawling process using log4j, ensuring the application does not terminate unexpectedly.
Neo4j Integration: Stores URLs and their relationships in a Neo4j graph database for efficient querying and analysis of the web structure.
Scalability and Extensibility: Designed to handle a growing number of URLs and web pages, with a modular architecture that allows for easy feature extensions and heuristic updates.
Installation
Clone the Repository
Clone this repository to your local machine:

bash
Copy code
git clone https://github.com/yourusername/web-crawler.git
cd web-crawler
Set up Neo4j

Install Neo4j and start the database.
Ensure Neo4j is running on localhost:7687 with the default username neo4j and password password.
Build the Project
Build the project using Maven:

bash
Copy code
./mvnw clean install
Run the Application
Run the application with Spring Boot:

bash
Copy code
./mvnw spring-boot:run
Usage
To start the crawling process, send a POST request to the following endpoint using Postman or any other HTTP client:

bash
Copy code
POST http://localhost:8080/crawl/start
Configuration
You can configure the seed URLs and other settings in the application.properties file located at src/main/resources/application.properties.

License
This project is licensed under the Northeastern University License. See the LICENSE file for details.

Contributing
Contributions are welcome! Feel free to open an issue or submit a pull request for any improvements or bug fixes.

Contact
For any questions or inquiries, please contact:

chakola.j@northeastern.edu
zheng.jiey@northeastern.edu
