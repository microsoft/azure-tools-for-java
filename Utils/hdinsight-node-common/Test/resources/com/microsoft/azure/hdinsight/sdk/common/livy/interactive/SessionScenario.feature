Feature: Livy Interactive Session Tests

  Scenario: Session.create() IT positive case with mocked http server
    Given setup a mock livy interactive service for POST request '/sessions' to return '{"id":6,"appId":null,"owner":null,"proxyUser":null,"state":"starting","kind":"spark","appInfo":{"driverLogUrl":null,"sparkUiUrl":null},"log":[]}' with status code 200
    And create a livy Spark interactive session instance with name 'testSparkREPL'
    And create the Spark interactive session, and save the response
    Then check the returned livy interactive session after creating should be
      | id        | 6 |

  Scenario: Session.create() IT negative case with mocked http server
    Given setup a mock livy interactive service for POST request '/sessions' to return 'Bad Request' with status code 400
    And create a livy Spark interactive session instance with name 'testSparkREPL'
    Then check the BadRequestHttpErrorStatus(400) when creating livy interactive session after creating should be thrown

  Scenario: Session.getAppId() IT positive case with mocked http server
    Given setup a mock livy interactive service for POST request '/sessions' to return '{"id":6,"appId":null,"owner":null,"proxyUser":null,"state":"starting","kind":"spark","appInfo":{"driverLogUrl":null,"sparkUiUrl":null},"log":[]}' with status code 200
    And setup a mock livy interactive service for GET request '/sessions/6' to return '{"id":6,"appId":"application_1517029729598_0086","owner":null,"proxyUser":null,"state":"idle","kind":"spark","appInfo":{"driverLogUrl":"https://zhwe-spkdbg.azurehdinsight.net/yarnui/10.0.0.8/node/containerlogs/container_e04_1517029729598_0086_01_000001/livy","sparkUiUrl":"https://zhwe-spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1517029729598_0086/"},"log":[]}' with status code 200
    And create a livy Spark interactive session instance with name 'testSparkREPL'
    And create the Spark interactive session, and save the response
    Then check the returned livy interactive session after creating should be
      | id        | 6 |
    Then check getting app ID with waiting for livy interactive session application run should be 'application_1517029729598_0086'

  Scenario: Session.kill() IT positive case with mocked http server
    Given setup a mock livy interactive service for POST request '/sessions' to return '{"id":6,"appId":null,"owner":null,"proxyUser":null,"state":"starting","kind":"spark","appInfo":{"driverLogUrl":null,"sparkUiUrl":null},"log":[]}' with status code 200
    And setup a mock livy interactive service for DELETE request '/sessions/6' to return '{}' with status code 200
    And create a livy Spark interactive session instance with name 'testSparkREPL'
    And create the Spark interactive session, and save the response
    Then check the returned livy interactive session after creating should be
      | id        | 6 |
    And kill the livy Spark interactive session
    Then check the delete operation request sent to '/sessions/6' when killing the session

  Scenario: Session.runCodes() IT positive case with mocked http server
    Given setup a mock livy interactive service for POST request '/sessions' to return '{"id":6,"appId":null,"owner":null,"proxyUser":null,"state":"starting","kind":"spark","appInfo":{"driverLogUrl":null,"sparkUiUrl":null},"log":[]}' with status code 200
    And setup a mock livy interactive service for GET request '/sessions/6' to return '{"id":6,"appId":"application_1517029729598_0086","owner":null,"proxyUser":null,"state":"idle","kind":"spark","appInfo":{"driverLogUrl":"https://zhwe-spkdbg.azurehdinsight.net/yarnui/10.0.0.8/node/containerlogs/container_e04_1517029729598_0086_01_000001/livy","sparkUiUrl":"https://zhwe-spkdbg.azurehdinsight.net/yarnui/hn/proxy/application_1517029729598_0086/"},"log":[]}' with status code 200
    And create a livy Spark interactive session instance with name 'testSparkREPL'
    And create the Spark interactive session, and save the response
    Then check the returned livy interactive session after creating should be
      | id        | 6 |
    Given setup a mock livy interactive service for POST request '/sessions/6/statements' to return '{"id":0,"state":"waiting","output":null}' with status code 200
    And setup a mock livy interactive service for GET request '/sessions/6/statements/0' to return '{"id":0,"state":"available","output":{"status":"ok","execution_count":0,"data":{"text/plain":"Hello World!"}}}' with status code 200
    And run the following codes in livy Spark interactive session
      | println("Hello World!") |
    Then check Spark interactive session statement run result stdout should be
      | Hello World! |

  Scenario: Session.create() UA with random UUID IT positive case with mocked http server
    Given setup a mock livy interactive service for POST request '/sessions' to return '{"id":6,"appId":null,"owner":null,"proxyUser":null,"state":"starting","kind":"spark","appInfo":{"driverLogUrl":null,"sparkUiUrl":null},"log":[]}' with status code 200
    And create a livy Spark interactive session instance with name 'testSparkREPL1'
    And create the Spark interactive session, and save the response
    And create a livy Spark interactive session instance with name 'testSparkREPL2'
    And create the Spark interactive session, and save the response
    Then those request headers UA fields are different

  Scenario: getUserAgent() shouldn't generate multiple UUID
    Given create a livy Spark interactive session instance with name 'testGetUserAgent'
    And get and set the session UserAgent
    And get and set the session UserAgent
    Then the UserAgent should only has one UUID