Feature: SparkBatchAzureSubmission tests

  Scenario: OAuth token should be added into default headers within requests
    Given mock a http service in SparkBatchAzureSubmissionScenario for GET request '/test' to return '{"stdout": "Hello world!"}' with status code 200
    And mock a Spark batch job Azure submission with access token 'access-token-mock'
    Then check GET request to '/test' should be with the following headers
      | Authorization  | Bearer access-token-mock |
      | X-Requested-By | ambari                   |
