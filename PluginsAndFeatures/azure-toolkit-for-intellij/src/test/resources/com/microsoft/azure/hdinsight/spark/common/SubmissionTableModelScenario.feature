Feature: SubmissionTableModel unit tests

  Scenario: The Spark configuration key-value pairs should be added into 'conf' key
    Given create the SparkSubmissionTable with the following config
      | driverMemory   | 2G |
      | driverCores    | 1  |
      | executorMemory | 8G |
      | numExecutors   | 4  |
      | executorCores  | 2  |
      | spark.driver.extraJavaOptions | -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006 |
    Then check to get config map should be '{"numExecutors":"4", "driverMemory":"2G", "executorMemory":"8G", "driverCores":"1", "executorCores":"2", "conf":{"spark.driver.extraJavaOptions":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006"}}'