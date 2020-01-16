Feature: Wasb URI operation

  Scenario: Convert Wasb URL path to WasbURI
    Then convert Wasb URL restful path to URI should be
      | url                                                             | uri                                                             |
      | https://accountName.blob.core.windows.net                       | <invalid_restful_path>                                          |
      | https://accountName.dfs.core.windows.net                        | <invalid_restful_path>                                          |
      | https://accountName.blob.core.windows.net/                      | <invalid_restful_path>                                          |
      | https://accountName.blob.core.windows.net/fs0                   | wasbs://fs0@accountName.blob.core.windows.net/                  |
      | https://accountName.blob.core.windows.net/fs0/                  | wasbs://fs0@accountName.blob.core.windows.net/                  |
      | https://accountName.blob.core.windows.net/fs0/subPath0          | wasbs://fs0@accountName.blob.core.windows.net/subPath0          |
      | https://accountName.blob.core.windows.net/fs0/subPath0/         | wasbs://fs0@accountName.blob.core.windows.net/subPath0/         |
      | https://accountName.blob.core.windows.net/fs0/subPath0/subPath1 | wasbs://fs0@accountName.blob.core.windows.net/subPath0/subPath1 |

  Scenario: Convert Wasb URI to restful path
    Then convert Wasb URI to URL restful path should be
      | uri                                                                    | url                                                                    |
      | wasbs://container0@accountName.blob.core.windows.net                   | https://accountName.blob.core.windows.net/container0/                  |
      | wasbs://container0@accountName.blob.core.windows.net/                  | https://accountName.blob.core.windows.net/container0/                  |
      | wasbs://container0@accountName.blob.core.windows.net/subPath0          | https://accountName.blob.core.windows.net/container0/subPath0          |
      | wasbs://container0@accountName.blob.core.windows.net/subPath0/         | https://accountName.blob.core.windows.net/container0/subPath0/         |
      | wasbs://container0@accountName.blob.core.windows.net/subPath0/subPath1 | https://accountName.blob.core.windows.net/container0/subPath0/subPath1 |

  Scenario: Check Wasb path parameters from URI
    Then check Wasb URI parameters as below
      | uri                                                                    | container  | endpointSuffix   | path               | account     |
      | wasbs://container0@accountName.blob.core.windows.net                   | container0 | core.windows.net | /                  | accountName |
      | wasbs://container0@accountName.blob.core.windows.net/                  | container0 | core.windows.net | /                  | accountName |
      | wasbs://container0@accountName.blob.core.windows.net/subPath0          | container0 | core.windows.net | /subPath0          | accountName |
      | wasbs://container0@accountName.blob.core.windows.net/subPath0/         | container0 | core.windows.net | /subPath0/         | accountName |
      | wasbs://container0@accountName.blob.core.windows.net/subPath0/subPath1 | container0 | core.windows.net | /subPath0/subPath1 | accountName |
      | https://accountName.blob.core.windows.net                              | <invalid>  | <invalid>        | <invalid>          | <invalid>   |
      | https://accountName.blob.core.windows.net/fs0                          | fs0        | core.windows.net | /                  | accountName |
      | https://accountName.blob.core.windows.net/                             | <invalid>  | <invalid>        | <invalid>          | <invalid>   |
      | https://accountName.blob.core.windows.net/fs0/subPath0                 | fs0        | core.windows.net | /subPath0          | accountName |
      | https://accountName.blob.core.windows.net/fs0/subPath0/                | fs0        | core.windows.net | /subPath0/         | accountName |
      | https://accountName.blob.core.windows.net/fs0/subPath0/subPath1        | fs0        | core.windows.net | /subPath0/subPath1 | accountName |

  Scenario: Check Wasb URI equality
    Then check Wasb URI equality as below
      | src                                                           | dest                                                          | isEqualed |
      | wasbs://container0@accountName.blob.core.windows.net/sp0      | https://accountName.blob.core.windows.net/container0/sp0      | true      |
      | wasbs://container0@accountName.blob.core.windows.net/         | https://accountName.blob.core.windows.net/container0/         | true      |
      | wasbs://container0@accountName.blob.core.windows.net          | https://accountName.blob.core.windows.net/container0/         | true      |
      | wasbs://container0@accountName.blob.core.windows.net/sp0      | https://accountName.blob.core.windows.net/container0/sp0/     | false     |
      | wasbs://container0@accountName.blob.core.windows.net/sp0/sp1/ | https://accountName.blob.core.windows.net/container0/sp0/sp1/ | true      |
      | wasbs://container0@accountName.blob.core.windows.net/sp0/sp1/ | https://accountName.blob.core.windows.net/container0/sp0/s1/  | false     |
      | https://accountName.blob.core.windows.net/container0/sp0      | wasbs://container0@accountName.blob.core.windows.net/sp0      | true      |
      | https://accountName.blob.core.windows.net/container0/         | wasbs://container0@accountName.blob.core.windows.net/         | true      |
      | https://accountName.blob.core.windows.net/container0          | wasbs://container0@accountName.blob.core.windows.net          | true      |
      | https://accountName.blob.core.windows.net/container0/sp0/     | wasbs://container0@accountName.blob.core.windows.net/sp0      | false     |
      | https://accountName.blob.core.windows.net/container0/sp0/sp1/ | wasbs://container0@accountName.blob.core.windows.net/sp0/sp1/ | true      |
      | https://accountName.blob.core.windows.net/container0/sp0/s1/  | wasbs://container0@accountName.blob.core.windows.net/sp0/sp1/ | false     |

  Scenario: Check Wasb URI resolve as root path
    Then check Wasb URI resolve as root path as below
      | uri                                                       | path                                 | result                                                                               |
      | wasbs://container0@account.blob.core.windows.net/         | sp0                                  | wasbs://container0@account.blob.core.windows.net/sp0                                 |
      | wasbs://container0@account.blob.core.windows.net          | sp0                                  | wasbs://container0@account.blob.core.windows.net/sp0                                 |
      | wasbs://container0@account.blob.core.windows.net/sp0      | sp1                                  | wasbs://container0@account.blob.core.windows.net/sp0/sp1                             |
      | wasbs://container0@account.blob.core.windows.net/sp0/     | sp1                                  | wasbs://container0@account.blob.core.windows.net/sp0/sp1                             |
      | wasbs://container0@account.blob.core.windows.net/sp0      | /sp1                                 | wasbs://container0@account.blob.core.windows.net/sp0/sp1                             |
      | wasbs://container0@account.blob.core.windows.net/sp0/     | /sp1                                 | wasbs://container0@account.blob.core.windows.net/sp0/sp1                             |
      | wasbs://container0@account.blob.core.windows.net/sp0/     | /sp1/                                | wasbs://container0@account.blob.core.windows.net/sp0/sp1/                            |
      | wasbs://container0@account.blob.core.windows.net/root/sp0 | sp1                                  | wasbs://container0@account.blob.core.windows.net/root/sp0/sp1                        |
      | wasbs://container0@account.blob.core.windows.net/         | -a-zA-Z0-9.~_@:!$'()*+,;=            | wasbs://container0@account.blob.core.windows.net/-a-zA-Z0-9.~_@:!$'()*+,;=           |
      | wasbs://container0@account.blob.core.windows.net          | -a-zA-Z0-9.~_@:!$'()*+,;=/words.txt  | wasbs://container0@account.blob.core.windows.net/-a-zA-Z0-9.~_@:!$'()*+,;=/words.txt |
      | wasbs://container0@account.blob.core.windows.net          | /-a-zA-Z0-9.~_@:!$'()*+,;=/words.txt | wasbs://container0@account.blob.core.windows.net/-a-zA-Z0-9.~_@:!$'()*+,;=/words.txt |
      | wasbs://container0@account.blob.core.windows.net/         | -a-zA-Z0-9.~_@:!$'()*+,;=/words.txt  | wasbs://container0@account.blob.core.windows.net/-a-zA-Z0-9.~_@:!$'()*+,;=/words.txt |
      | wasbs://container0@account.blob.core.windows.net/         | /-a-zA-Z0-9.~_@:!$'()*+,;=/words.txt | wasbs://container0@account.blob.core.windows.net/-a-zA-Z0-9.~_@:!$'()*+,;=/words.txt |

  Scenario: Check Wasb URI relativize
    Then check Wasb URI relativize as below
      | src                                                       | dest                                                         | result |
      | wasbs://container0@accountName.blob.core.windows.net      | wasbs://container0@accountName.blob.core.windows.net/        |        |
      | wasbs://container0@accountName.blob.core.windows.net/     | wasbs://container0@accountName.blob.core.windows.net/sp0     | sp0    |
      | wasbs://container0@accountName.blob.core.windows.net/sp0  | wasbs://container0@accountName.blob.core.windows.net/sp0     |        |
      | wasbs://container0@accountName.blob.core.windows.net/sp0  | wasbs://container0@accountName.blob.core.windows.net/sp0/sp1 | sp1    |
      | wasbs://container0@accountName.blob.core.windows.net/sp0  | https://accountName.blob.core.windows.net/container0/sp0/sp1 | sp1    |
      | wasbs://container0@accountName.blob.core.windows.net/sp0  | wasbs://container0@accountName.blob.core.windows.net/sp1     | <null> |
      | wasbs://container0@accountName.blob.core.windows.net/sp0/ | wasbs://container0@accountName.blob.core.windows.net/sp1     | <null> |
      | wasbs://container0@accountName.blob.core.windows.net/sp0/ | wasbs://container0@otherName.blob.core.windows.net/sp1       | <null> |
      | wasbs://container0@accountName.blob.core.windows.net/sp0/ | wasbs://container0@otherName.blob.core.windows.net/sp0/sp1   | <null> |

  Scenario: Encode path
    Then check the encoded path as below
      | rawPath                          | encodedPath                     |
      | subPath                          | subPath                         |
      | /subPath                         | /subPath                        |
      | /subPath/file                    | /subPath/file                   |
      | new folder                       | new%20folder                    |
      | % ?                              | %25%20%3F                       |
      | /.~_@:!$'()*+,;=% ?              | /.~_@:!$'()*+,;=%25%20%3F       |
      | ./.~_@:!$'()*+,;////=%?%?aa//./ | .~_@:!$'()*+,;/=%25%3F%25%3Faa/ |
      | ./bbb                            | bbb                             |
      | ./:bbb                           | :bbb                            |
