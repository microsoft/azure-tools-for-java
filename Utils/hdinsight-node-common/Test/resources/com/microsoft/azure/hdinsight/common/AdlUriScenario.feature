Feature: ADLS Gen1 URI operation

  Scenario: Convert Gen1 restful path to ADL URI
    Then convert Gen ONE URL restful path to URI should be
      | url                                                                         | uri                                                          |
      | https://accountName.azuredatalakestore.net                                  | <invalid_restful_path>                                       |
      | https://accountName.dfs.core.windows.net                                    | <invalid_restful_path>                                       |
      | https://accountName.azuredatalakestore.net/                                 | <invalid_restful_path>                                       |
      | https://accountName.azuredatalakestore.net/fs0                              | <invalid_restful_path>                                       |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/fs0                   | adl://accountName.azuredatalakestore.net/fs0                   |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/fs0/                  | adl://accountName.azuredatalakestore.net/fs0/                  |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/fs0/subPath0          | adl://accountName.azuredatalakestore.net/fs0/subPath0          |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/fs0/subPath0/         | adl://accountName.azuredatalakestore.net/fs0/subPath0/         |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/fs0/subPath0/subPath1 | adl://accountName.azuredatalakestore.net/fs0/subPath0/subPath1 |

  Scenario: Convert Gen1 ABFS URI to restful path
    Then convert ADL URI to Gen ONE URL restful path should be
      | uri                                                        | url                                                                     |
      | adl://accountName.azuredatalakestore.net                   | https://accountName.azuredatalakestore.net/webhdfs/v1/                  |
      | adl://accountName.azuredatalakestore.net/                  | https://accountName.azuredatalakestore.net/webhdfs/v1/                  |
      | adl://accountName.azuredatalakestore.net/subPath0          | https://accountName.azuredatalakestore.net/webhdfs/v1/subPath0          |
      | adl://accountName.azuredatalakestore.net/subPath0/         | https://accountName.azuredatalakestore.net/webhdfs/v1/subPath0/         |
      | adl://accountName.azuredatalakestore.net/subPath0/subPath1 | https://accountName.azuredatalakestore.net/webhdfs/v1/subPath0/subPath1 |

  Scenario: Check Gen1 path parameters from ADL URI
    Then check ADL URI parameters as below
      | uri                                                                         | path                | storageName |
      | adl://accountName.azuredatalakestore.net                                    | /                   | accountName |
      | adl://accountName.azuredatalakestore.net/                                   | /                   | accountName |
      | adl://accountName.azuredatalakestore.net/subPath0                           | /subPath0           | accountName |
      | adl://accountName.azuredatalakestore.net/subPath0/                          | /subPath0/          | accountName |
      | adl://accountName.azuredatalakestore.net/subPath0/subPath1                  | /subPath0/subPath1  | accountName |
      | https://accountName.azuredatalakestore.net                                  | <invalid>           | <invalid>   |
      | https://accountName.azuredatalakestore.net/fs0                              | <invalid>           | <invalid>   |
      | https://accountName.azuredatalakestore.net/webhdfs/v1                       | /                   | accountName |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/                      | /                   | accountName |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/subPath0              | /subPath0           | accountName |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/subPath0/             | /subPath0/          | accountName |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/subPath0/subPath1     | /subPath0/subPath1  | accountName |

  Scenario: Check Gen1 URI equality
    Then check ADL URI equality as below
      | src                                               | dest                                                           | isEqualed |
      | adl://accountName.azuredatalakestore.net/sp0      | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0      | true      |
      | adl://accountName.azuredatalakestore.net/         | https://accountName.azuredatalakestore.net/webhdfs/v1/         | true      |
      | adl://accountName.azuredatalakestore.net          | https://accountName.azuredatalakestore.net/webhdfs/v1/         | true      |
      | adl://accountName.azuredatalakestore.net/sp0      | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/     | false     |
      | adl://accountName.azuredatalakestore.net/sp0/sp1/ | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/sp1/ | true      |
      | adl://accountName.azuredatalakestore.net/sp0/sp1/ | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/s1/  | false     |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0      | adl://accountName.azuredatalakestore.net/sp0      | true      |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/         | adl://accountName.azuredatalakestore.net/         | true      |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/         | adl://accountName.azuredatalakestore.net          | true      |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/     | adl://accountName.azuredatalakestore.net/sp0      | false     |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/sp1/ | adl://accountName.azuredatalakestore.net/sp0/sp1/ | true      |
      | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/s1/  | adl://accountName.azuredatalakestore.net/sp0/sp1/ | false     |

  Scenario: Check Gen1 URI resolve as root path
    Then check ADL URI resolve as root path as below
      | uri                                           | path     | result                                            |
      | adl://account.azuredatalakestore.net/         | sp0      | adl://account.azuredatalakestore.net/sp0          |
      | adl://account.azuredatalakestore.net          | sp0      | adl://account.azuredatalakestore.net/sp0          |
      | adl://account.azuredatalakestore.net/sp0      | sp1      | adl://account.azuredatalakestore.net/sp0/sp1      |
      | adl://account.azuredatalakestore.net/sp0/     | sp1      | adl://account.azuredatalakestore.net/sp0/sp1      |
      | adl://account.azuredatalakestore.net/sp0      | /sp1     | adl://account.azuredatalakestore.net/sp0/sp1      |
      | adl://account.azuredatalakestore.net/sp0/     | /sp1     | adl://account.azuredatalakestore.net/sp0/sp1      |
      | adl://account.azuredatalakestore.net/sp0/     | /sp1/    | adl://account.azuredatalakestore.net/sp0/sp1/     |
      | adl://account.azuredatalakestore.net/root/sp0 | sp1      | adl://account.azuredatalakestore.net/root/sp0/sp1 |

  Scenario: Check Gen1 URI relativize
    Then check ADL URI relativize as below
      | src                                           | dest                                                           | result |
      | adl://accountName.azuredatalakestore.net      | adl://accountName.azuredatalakestore.net/                      |        |
      | adl://accountName.azuredatalakestore.net/     | adl://accountName.azuredatalakestore.net/sp0                   | sp0    |
      | adl://accountName.azuredatalakestore.net/sp0  | adl://accountName.azuredatalakestore.net/sp0                   |        |
      | adl://accountName.azuredatalakestore.net/sp0  | adl://accountName.azuredatalakestore.net/sp0/sp1               | sp1    |
      | adl://accountName.azuredatalakestore.net/sp0  | https://accountName.azuredatalakestore.net/webhdfs/v1/sp0/sp1  | sp1    |
      | adl://accountName.azuredatalakestore.net/sp0  | adl://accountName.azuredatalakestore.net/sp1                   | <null> |
      | adl://accountName.azuredatalakestore.net/sp0/ | adl://accountName.azuredatalakestore.net/sp1                   | <null> |
      | adl://accountName.azuredatalakestore.net/sp0/ | adl://otherName.azuredatalakestore.net/sp1                     | <null> |
      | adl://accountName.azuredatalakestore.net/sp0/ | adl://otherName.azuredatalakestore.net/sp0/sp1                 | <null> |

