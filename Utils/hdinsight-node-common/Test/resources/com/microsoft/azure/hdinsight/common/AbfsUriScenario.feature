Feature: ADLS Gen2 URI operation

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

  Scenario: Get properties from ABFS URI
    Then properties of abfs URI should be
      | url                                                            | accountName | fileSystem | rawPath             | path               | directoryParam    |
      | abfs://accountName.dfs.core.windows.net                        | <invalid>   | <invalid>  | <invalid>           | <invalid>          | <invalid>         |
      | abfs://fs0@accountName.dfs.core.windows.net                    | accountName | fs0        |                     |                    | /                 |
      | abfs://fs0@accountName.dfs.core.windows.net/                   | accountName | fs0        | /                   | /                  | /                 |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0           | accountName | fs0        | /subPath0           | /subPath0          | subPath0          |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/          | accountName | fs0        | /subPath0/          | /subPath0/         | subPath0/         |
      | abfs://fs0@accountName.dfs.core.windows.net/subPath0/subPath1  | accountName | fs0        | /subPath0/subPath1  | /subPath0/subPath1 | subPath0/subPath1 |
      | https://accountName.dfs.core.windows.net                       | <invalid>   | <invalid>  | <invalid>           | <invalid>          | <invalid>         |
      | https://accountName.dfs.core.windows.net/fs0                   | accountName | fs0        |                     |                    | /                 |
      | https://accountName.dfs.core.windows.net/fs0/                  | accountName | fs0        | /                   | /                  | /                 |
      | https://accountName.dfs.core.windows.net/fs0/subPath0          | accountName | fs0        | /subPath0           | /subPath0          | subPath0          |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/         | accountName | fs0        | /subPath0/          | /subPath0/         | subPath0/         |
      | https://accountName.dfs.core.windows.net/fs0/subPath0/subPath1 | accountName | fs0        | /subPath0/subPath1  | /subPath0/subPath1 | subPath0/subPath1 |
      | abfs://fs0@accountName.dfs.core.windows.net/new%20%23%25folder | accountName | fs0        | /new%20%23%25folder | /new #%folder      | new #%folder      |
      | abfs://fs0@accountName.dfs.core.windows.net/.~_@:!$'()*+,;=    | accountName | fs0        | /.~_@:!$'()*+,;=    | /.~_@:!$'()*+,;=   | .~_@:!$'()*+,;=   |
      | abfs://fs0@accountName.dfs.core.windows.net/aaa%3Fbbb          | accountName | fs0        | /aaa%3Fbbb          | /aaa?bbb           | aaa?bbb           |
      | abfs://fs0@accountName.dfs.core.windows.net/aaa?bbb            | <invalid>   | <invalid>  | <invalid>           | <invalid>          | <invalid>         |
      | abfs://fs0@accountName.dfs.core.windows.net/new folder         | <invalid>   | <invalid>  | <invalid>           | <invalid>          | <invalid>         |
      | abfs://fs0@accountName.dfs.core.windows.net/new#folder         | <invalid>   | <invalid>  | <invalid>           | <invalid>          | <invalid>         |
      | abfs://fs0@accountName.dfs.core.windows.net/new%folder         | <invalid>   | <invalid>  | <invalid>           | <invalid>          | <invalid>         |
