Feature: Mock File System Unit Test

  Scenario: Local file scheme with Window path
    Then convert mocked file system path 'file:/C:/abc' to File should be '/C:/abc'

  Scenario: Default WASB scheme root path
    Given set mocked file system local working directory to '/x/user/current'
    Then convert mocked file system path 'wasb:///abc' to File should be '/x/abc'

  Scenario: Default mockfs scheme root path
    Given set mocked file system local working directory to '/x/user/current'
    Then convert mocked file system path 'mockfs:/abc' to File should be '/abc'

  Scenario: Default working path without scheme
    Given set mocked file system local working directory to '/x/user/current'
    Then convert mocked file system path 'abc/def' to File should be '/x/user/current/abc/def'

  Scenario: WASB container blob path
    Given set mocked file system local working directory to '/data/__default__/user/current'
    Then convert mocked file system path 'wasb://account@blob1/abc/def' to File should be '/data/account@blob1/abc/def'

  Scenario: WASB container blob root path
    Given set mocked file system local working directory to '/data/__default__/user/current'
    Then convert mocked file system path 'wasb://account@blob1/' to File should be '/data/account@blob1/'

  Scenario: Resources file exists
    Given set mocked file system local working directory to 'test/resources/data/__default__/user/current'
    Then mocked file 'my_words.txt' should exists
