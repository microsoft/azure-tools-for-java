Feature: SparkSubmissionParameter unit test

  Scenario: Private function getSparkSubmissionParameterMap
    Given create SparkSubmissionParameter spark config parameter
      | spark.driver.extraJavaOptions | -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006 |
    And create SparkSubmissionParameter with the following job config
      |||
    Then the parameter map should include key 'conf' with value '{spark.driver.extraJavaOptions=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006}'

  Scenario: serializeToJson would serialize the conf map well
    Given mock args to farg1,farg2
    And mock file to fFilePath
    And mock reference files to ffile1,ffile2
    And mock reference jars to fjar1,fjar2
    And mock className to fakeClassName
    And create SparkSubmissionParameter with the following job config
      |||
    Then the serialized JSON should be '{"args":["farg1","farg2"],"file":"fFilePath","files":["ffile1","ffile2"],"jars":["fjar1","fjar2"],"className":"fakeClassName"}'

    Given create SparkSubmissionParameter spark config parameter
      | spark.driver.extraJavaOptions | -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006 |
      | other                         | Other values                                                       |
    And create SparkSubmissionParameter with the following job config
      |||
    Then the serialized JSON should be '{"args":["farg1","farg2"],"file":"fFilePath","files":["ffile1","ffile2"],"jars":["fjar1","fjar2"],"className":"fakeClassName","conf":{"other":"Other values","spark.driver.extraJavaOptions":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006"}}'

    Given create SparkSubmissionParameter spark config parameter
      | invalid | half close" |
    And create SparkSubmissionParameter with the following job config
      |||
    Then the serialized JSON should be '{"args":["farg1","farg2"],"file":"fFilePath","files":["ffile1","ffile2"],"jars":["fjar1","fjar2"],"className":"fakeClassName","conf":{"invalid":"half close\""}}'