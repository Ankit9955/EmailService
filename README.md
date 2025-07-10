Email Service
A resilient email sending service implemented in Java with Spring Boot, featuring retry with exponential backoff, provider fallback, idempotency, rate limiting, status tracking, circuit breaker, logging, and a basic queue system.
Features

Retry Mechanism: Uses Resilience4j for retry with exponential backoff (3 attempts, 500ms initial delay).
Fallback: Switches to a secondary mock provider if the primary fails.
Idempotency: Prevents duplicate email sends using a message ID check.
Rate Limiting: Limits requests per client to 10 per minute.
Status Tracking: Tracks email send status (SUCCESS/FAILED) with attempt count and error messages.
Circuit Breaker: Uses Resilience4j to prevent cascading failures.
Logging: Logs send attempts and failures using SLF4J.
Queue System: Processes emails asynchronously using an in-memory queue.

Prerequisites

Java 17
Maven 3.8+
Eclipse IDE (or any Java IDE)
Postman (for API testing)

Setup Instructions

Clone the Repository:git clone <your-repo-url>
cd EmailServiceProject


Import into Eclipse:
Open Eclipse IDE.
Go to File > Import > Maven > Existing Maven Projects.
Select the project folder and click Finish.


Build the Project:mvn clean install


Run the Application:
In Eclipse, right-click EmailServiceApplication.java > Run As > Spring Boot App.
The application runs on http://localhost:8080.


Test the API:
Use Postman to send a POST request to http://localhost:8080/api/email/send with the following JSON body:{
    "messageId": "test-123",
    "to": "recipient@example.com",
    "subject": "Test Email",
    "body": "This is a test email."
}





Running Tests

Run unit tests in Eclipse:
Right-click EmailServiceTest.java > Run As > JUnit Test.


Alternatively, use Maven:mvn test



Deployment to Cloud

Package the Application:mvn package


Deploy to a Cloud Provider (e.g., AWS Elastic Beanstalk, Heroku, or Render):
Heroku Example:
Install Heroku CLI.
Create a Procfile in the project root:web: java -jar target/emailservice-0.0.1-SNAPSHOT.jar


Run:heroku create
git push heroku main




Access the deployed API at the provided Heroku URL.


Test the Deployed API:
Use Postman to send requests to the cloud endpoint (e.g., https://your-app.herokuapp.com/api/email/send).



Assumptions

Mock providers simulate email sending with random failures (30% for Provider A, 20% for Provider B).
Rate limiting uses a placeholder client IP ("client-ip"). In production, extract the IP from the HTTP request.
The queue system is in-memory. For production, consider using RabbitMQ or Kafka.
Logging is basic (SLF4J). Enhance with a logging framework like Logback for production.
No actual email sending is performed (mock providers only).

Creating a GitHub Repository

Create a new public repository on GitHub (https://github.com/new).
Initialize the local project with Git:git init
git add .
git commit -m "Initial commit"
git remote add origin <your-repo-url>
git push -u origin main



Recording a Screencast

Use a screen recording tool (e.g., OBS Studio, Zoom, or Screencast-O-Matic).
Record:
Your face (via webcam) to meet the requirement.
A demo of the application running in Eclipse (send a request via Postman and show the response).
A walkthrough of the code (open key files like EmailService.java and explain the implementation).


Save the video and upload it to a platform like YouTube (unlisted) or Google Drive.
Share the video link in your submission.

API Endpoint

Endpoint: /api/email/send
Method: POST
Request Body:{
    "messageId": "string",
    "to": "string",
    "subject": "string",
    "body": "string"
}


Response:
200 OK: Email queued successfully.
409 Conflict: Email already sent (idempotency).
429 Too Many Requests: Rate limit exceeded.



Notes

The circuit breaker opens after a 50% failure rate (configurable in application.properties).
Retry configuration is set in application.properties (3 attempts, 500ms base delay, exponential backoff).
The application is designed with SOLID principles (e.g., single responsibility, dependency inversion via interfaces).
