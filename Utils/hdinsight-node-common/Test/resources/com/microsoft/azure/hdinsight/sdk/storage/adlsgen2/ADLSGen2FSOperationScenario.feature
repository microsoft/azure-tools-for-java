Feature: ADLS Gen2 File system operation

  Scenario: Convert Gen2 restful path to ABFS URI
    Given Gen two restful path is
      | https://accountName.dfs.core.windows.net                       |
      | https://accountName.dfs.core.windows.net/                      |
      | https://accountName.dfs.core.windows.net/fs0                   |
      | https://accountName.dfs.core.windows.net/fs0/                  |
      | https://accountName.dfs.core.windows.net/fs0/subPath0          |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/         |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/subPath1 |
    Then convert the restful path to ABFS URI should be
      | invalid restful path                                          |
      | invalid restful path                                          |
      | abfs://fs0@accountName.dfs.core.windows.net                   |
      | abfs://fs0@accountName.dfs.core.windows.net/                  |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0          |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/         |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/subPath1 |

  Scenario: Convert Gen2 ABFS URI to restful path
    Given ABFS URI is
      | abfs://accountName.dfs.core.windows.net                       |
      | abfs://fs0@accountName.dfs.core.windows.net                   |
      | abfs://fs0@accountName.dfs.core.windows.net/                  |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0          |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/         |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/subPath1 |
    Then convert the ABFS URI to restful path should be
      | invalid Gen2 URI                                               |
      | https://accountName.dfs.core.windows.net/fs0                   |
      | https://accountName.dfs.core.windows.net/fs0/                  |
      | https://accountName.dfs.core.windows.net/fs0/subPath0          |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/         |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/subPath1 |

  Scenario: Get base path from Gen2 restful path
    Given Gen two restful path is
      | https://accountName.dfs.core.windows.net                       |
      | https://accountName.dfs.core.windows.net/fs0                   |
      | https://accountName.dfs.core.windows.net/fs0/                  |
      | https://accountName.dfs.core.windows.net/fs0/subPath0          |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/         |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/subPath1 |
    Then the Gen two base restful path should be
      | invalid Gen2 URI                             |
      | https://accountName.dfs.core.windows.net/fs0 |
      | https://accountName.dfs.core.windows.net/fs0 |
      | https://accountName.dfs.core.windows.net/fs0 |
      | https://accountName.dfs.core.windows.net/fs0 |
      | https://accountName.dfs.core.windows.net/fs0 |

  Scenario: Get Gen2 directory param from ABFS URI
    Given ABFS URI is
      | abfs://accountName.dfs.core.windows.net                       |
      | abfs://fs0@accountName.dfs.core.windows.net                   |
      | abfs://fs0@accountName.dfs.core.windows.net/                  |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0          |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/         |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/subPath1 |
    Then the Gen two directory param should be
      | invalid Gen2 URI   |
      | /                  |
      | /                  |
      | subPath0          |
      | subPath0/         |
      | subPath0/subPath1 |
