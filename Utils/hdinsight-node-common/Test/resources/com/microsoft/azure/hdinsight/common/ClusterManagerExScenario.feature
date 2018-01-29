Feature: ClusterManagerEx tests
  Scenario: getClusterDetails unit tests without Azure subscription, with linked clusters
    Given Linked HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
      | link0Mock | link0sa0       | link0saKey | my@foo   | myPass   |              |
      | link1Mock | link1sa0       | link1saKey | my@foo   | myPass   |              |
    Given emulated HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
    Then check get all Cluster details should be:
      | link0Mock [Linked] |
      | link1Mock [Linked] |

  Scenario: getClusterDetails unit tests without Azure subscription, with linked cluster and emulated clusters
    Given Linked HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
      | link0Mock | link0sa0       | link0saKey | my@foo   | myPass   |              |
    Given emulated HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
      | emu0Mock  | emu0sa0        |            |          |          |              |
    Then check get all Cluster details should be:
      | link0Mock [Linked]               |
      | emu0Mock (Spark: 1.6.0 Emulator) |

  Scenario: getClusterDetails unit tests with Azure subscription, without linked cluster
    Given subscriptions mocked are:
      | name      | isSelected |
      | subscrip0 | true       |
      | subscripA | false      |
    Given Linked HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
    Given in subscription HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
      | sub0      |                |            | admin    | myPass   | subscrip0    |
      | sub1      |                |            | admin    | myPass   | subscrip0    |
      | subA      |                |            | admin    | myPass   | subscripA    |
    Given emulated HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
    Then check get all Cluster details should be:
      | sub0 (Spark: 2.2) |
      | sub1 (Spark: 2.2) |

  Scenario: getClusterDetails unit tests with Azure subscription, with linked cluster, the duplicated should be replaced by linked cluster
    Given subscriptions mocked are:
      | name      | isSelected |
      | subscrip0 | true       |
      | subscripA | true       |
    Given Linked HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
      | link0Mock | link0sa0       | link0saKey | my@foo   | myPass   |              |
      | sub1      |                |            | admin    | myPass   |              |
    Given in subscription HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
      | sub0      |                |            | admin    | myPass   | subscrip0    |
      | sub1      |                |            | admin    | myPass   | subscrip0    |
      | subA      |                |            | admin    | myPass   | subscripA    |
    Given emulated HDInsight clusters are:
      | name      | storageAccount | storageKey | username | password | subscription |
    Then check get all Cluster details should be:
      | sub0 (Spark: 2.2)  |
      | sub1 [Linked]      |
      | link0Mock [Linked] |
      | subA (Spark: 2.2)  |
