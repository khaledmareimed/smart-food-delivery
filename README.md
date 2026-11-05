# Smart Food Delivery Management System

Java 8+ / Maven / JUnit 5 / Mockito / JaCoCo.

## Run tests with coverage
    mvn clean verify
Coverage report: `target/site/jacoco/index.html`

## Run the console app
    mvn -q compile exec:java -Dexec.mainClass=com.fooddelivery.Main
or run `com.fooddelivery.Main` from your IDE.

Demo logins: `alice / alice123` (customer), `bob / manager123` (manager).

## Layers
presentation -> service -> domain, repository (in-memory)

## Progress
- [x] Sprint 1 - Authentication & Restaurant/Menu catalog
- [ ] Sprint 2 - Order operations
- [ ] Sprint 3 - Driver assignment & notifications
- [ ] Sprint 4 - Status lifecycle, pricing, payment
- [ ] Sprint 5 - Order types & driver types
