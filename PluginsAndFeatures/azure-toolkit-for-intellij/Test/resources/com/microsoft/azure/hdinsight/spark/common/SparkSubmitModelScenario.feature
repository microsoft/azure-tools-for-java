Feature: SparkSubmitModel tests

  Scenario: The model with local artifact XML Elements serialization
    Given set SparkSubmitModel properties as following
      | cluster_name        | clusterMock |
      | is_local_artifact   | true        |
      | local_artifact_path | c:\abc.jar  |
      | classname           | test.Main   |
      | cmd_line_args       | 1 2 3       |
      | ref_jars            | a;b;c       |
      | ref_files           | x;y;z       |
    Then checking XML serialized should to be '<spark_submission artifact_name="" cluster_name="clusterMock" file_path="" is_local_artifact="true" local_artifact_path="c:\abc.jar" classname="test.Main"><cmd_line_args><option value="1" /><option value="2" /><option value="3" /></cmd_line_args><jobConfigs><array><option value="driverMemory" /><option value="4G" /></array><array><option value="driverCores" /><option value="1" /></array><array><option value="executorMemory" /><option value="4G" /></array><array><option value="executorCores" /><option value="1" /></array><array><option value="numExecutors" /><option value="5" /></array></jobConfigs><ref_files><option value="x" /><option value="y" /><option value="z" /></ref_files><ref_jars><option value="a" /><option value="b" /><option value="c" /></ref_jars><ssh_cert auth_type="UsePassword" private_key_path="" user="sshuser" remote_debug_enabled="false" /><job_upload_storage storage_account_type="DEFAULT_STORAGE_ACCOUNT" /></spark_submission>'

  Scenario: The model XML Elements regression deserialization
    Given the SparkSubmitModel XML input '<spark_submission artifact_name="" cluster_name="clusterMock" file_path="" is_local_artifact="true" local_artifact_path="c:\abc.jar" classname="test.Main"><cmd_line_args><option value="1" /><option value="2" /><option value="3" /></cmd_line_args><ref_files><option value="x" /><option value="y" /><option value="z" /></ref_files><ref_jars><option value="a" /><option value="b" /><option value="c" /></ref_jars><ssh_cert store_account="Azure IntelliJ Plugin Spark Debug SSH - ssh:/" auth_type="UsePassword" private_key_path="" user="sshuser" remote_debug_enabled="false" /><job_upload_storage storage_account="account" upload_path="wasbs://container@account.blob.core.windows.net/SparkSubmission/" selected_container="container" storage_account_type="Blob" /><job_conf k1="v1" k2="v2"/></spark_submission>' to deserialize
    Then check SparkSubmitModel properties as following
      | cluster_name        | clusterMock |
      | is_local_artifact   | true        |
      | local_artifact_path | c:\abc.jar  |
      | classname           | test.Main   |
      | cmd_line_args       | 1 2 3       |
      | ref_jars            | a;b;c       |
      | ref_files           | x;y;z       |

  Scenario: The model XML Elements deserialization
    Given the SparkSubmitModel XML input '<spark_submission artifact_name="" cluster_name="clusterMock" file_path="" is_local_artifact="true" local_artifact_path="c:\abc.jar" classname="test.Main"><cmd_line_args><option value="1" /><option value="2" /><option value="3" /></cmd_line_args><ref_files><option value="x" /><option value="y" /><option value="z" /></ref_files><ref_jars><option value="a" /><option value="b" /><option value="c" /></ref_jars><ssh_cert store_account="Azure IntelliJ Plugin Spark Debug SSH - ssh:/" auth_type="UsePassword" private_key_path="" user="sshuser" remote_debug_enabled="false" /><job_upload_storage storage_account="account" storage_key="key" upload_path="wasbs://container@account.blob.core.windows.net/SparkSubmission/" selected_container="container" storage_account_type="Blob" /><job_conf><map></map></job_conf></spark_submission>' to deserialize
    Then check SparkSubmitModel properties as following
      | cluster_name        | clusterMock |
      | is_local_artifact   | true        |
      | local_artifact_path | c:\abc.jar  |
      | classname           | test.Main   |
      | cmd_line_args       | 1 2 3       |
      | ref_jars            | a;b;c       |
      | ref_files           | x;y;z       |
