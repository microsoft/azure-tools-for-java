Feature: Spark Local Runner Integration Test

  Scenario: Mocked default wasb folder read
    Given locally run job 'com.microsoft.azure.hdinsight.spark.mock.jobapp.WordCountTest' with args
      | wasb:///word_count_input.txt |
    Then locally run stand output should be
      | a,1      |
      | mocked,1 |
      | fs,1     |
      | with,1   |
      | Spark,1  |
      | run,1    |
      | Hello,1  |
      | local,2  |
      | world,1  |

  Scenario: Mocked default folder without scheme read
    Given locally run job 'com.microsoft.azure.hdinsight.spark.mock.jobapp.WordCountTest' with args
      | my_words.txt |
    Then locally run stand output should be
      | a,1      |
      | word,2   |
      | is,1     |
      | not,1    |
      | My,1     |

  Scenario: Mocked root folder without scheme read
    Given locally run job 'com.microsoft.azure.hdinsight.spark.mock.jobapp.WordCountTest' with args
      | /word_count_input.txt |
    Then locally run stand output should be
      | a,1      |
      | mocked,1 |
      | fs,1     |
      | with,1   |
      | Spark,1  |
      | run,1    |
      | Hello,1  |
      | local,2  |
      | world,1  |

  Scenario: Mocked blob container file write
    Given locally run job 'com.microsoft.azure.hdinsight.spark.mock.jobapp.WordCountTest' with args
      | /word_count_input.txt | wasb://container1/wc_result |
    Then locally cat 'wasb://container1/wc_result' should be
      | (a,1)      |
      | (mocked,1) |
      | (fs,1)     |
      | (with,1)   |
      | (Spark,1)  |
      | (run,1)    |
      | (Hello,1)  |
      | (local,2)  |
      | (world,1)  |

  Scenario: SparkSQL test
    Given locally run job 'com.microsoft.azure.hdinsight.spark.mock.jobapp.SparkSQLTest' with args
      | wasb:///people.json | people | SELECT * FROM people |
    Then locally run stand output table should be
      | age  | name    |
      | null | Michael |
      | 30   | Andy    |
      | 19   | Justin  |
