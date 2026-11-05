# Smart Food Delivery Management System

Software Engineering (10636313) course project – Phase 1.

Team: khaledmareimed (s12323961), zainsiksik (s12323511)

Java 8+ · Maven · Swing · SQLite · JUnit 5 · Mockito · JaCoCo · GitHub Actions

## Run the app

    mvn clean package -DskipTests
    java -jar target/smart-food-delivery-1.0-SNAPSHOT.jar

or `mvn compile exec:java`, or run `com.fooddelivery.Main` from your IDE.

The data is stored in `food_delivery.db` (SQLite) in the folder you run from.
Delete that file to start again with the demo data.

Demo logins:

| User  | Password    | Role     |
|-------|-------------|----------|
| alice | alice123    | customer |
| bob   | manager123  | manager  |

## Run the tests with coverage

    mvn clean verify

Coverage report: `target/site/jacoco/index.html`.
The build fails if line or branch coverage drops below 80%.
The Swing screens (`presentation` package) are tested by hand and are not counted.

## Layers

    presentation (Swing)  ->  service  ->  domain
                                  |
                              repository  ->  persistence (SQLite)

## Sprints

- [x] Sprint 1 – Login, open restaurants, available menu items (US1.1–US1.3)
- [x] Sprint 2 – Place order, invalid orders, delivery policies, cancel (US2.1–US2.4) + SQLite
- [x] Sprint 3 – Driver assignment (nearest / fastest), notifications (US3.1–US3.4) + Swing GUI
- [x] Sprint 4 – Order lifecycle, delivery fee, pricing types, cash payment (US4.1–US4.4)
- [x] Sprint 5 – Order types and driver types (US5.1–US5.4)

## Business rules

- Delivery fee = 5 + 1.5 per km (restaurant to customer).
- Pricing: NORMAL; PEAK (fee × 1.5, applied automatically 12–14 and 18–21);
  EXPRESS (fee × 2); PROMO (15% off items, max 20).
- Order types: STANDARD; EXPRESS (+10 delivery fee); SCHEDULED (needs a future delivery time).
- Drivers: BICYCLE (≤ 3 km), MOTORCYCLE (≤ 10 km), CAR (any distance, required for 10+ items),
  ELECTRIC (battery range must cover driver → restaurant → customer).
- Lifecycle: Pending → Confirmed → Preparing → Ready → Out for Delivery → Delivered.
  Pending and Confirmed orders can be cancelled. Delivered orders cannot be changed.

## Docs

- UML class diagram: `docs/class-diagram.puml`
- Fully documented class (Javadoc): `domain/Order.java`
