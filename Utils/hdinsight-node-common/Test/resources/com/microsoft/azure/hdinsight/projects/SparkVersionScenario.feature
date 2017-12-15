Feature: SparkVersion Unit Tests

  Scenario: Spark Version comparison
    Then Spark version 'Spark 2.1.0 (Scala 2.11.8)' should large than 'Spark 2.0.2 (Scala 2.11.8)'
    Then Spark version 'Spark 2.1.0 (Scala 2.11.8)' should equal to 'Spark 2.1.0 (Scala 2.11.8)'
