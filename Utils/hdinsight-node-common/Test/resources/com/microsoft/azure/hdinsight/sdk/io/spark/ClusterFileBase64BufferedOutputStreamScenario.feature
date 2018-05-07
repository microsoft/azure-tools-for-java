Feature: ClusterFileBase64KBBufferedOutputStream tests

  Scenario: Unit tests with mocked Livy session
    Given create a mocked Livy session for ClusterFileBase64KBBufferedOutputStream
    And create a Spark cluster file BASE64 output stream '/tmp/test.jar' with page size 1KB
    Then uploading the following BASE64 string
      | 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     |
    Then check the statements send to Livy session should be:
      """
      import java.io._
      import java.util.Base64

      val jarOutput = "/tmp/test.jar"
      val fs = org.apache.hadoop.fs.FileSystem.get(sc.hadoopConfiguration)
      val jarFileOutput = fs.create(new org.apache.hadoop.fs.Path(jarOutput), true)
      val out = new DataOutputStream(new BufferedOutputStream(jarFileOutput))

      def writePage(encodedBase64: String) = {
          val pageBytes = Base64.getDecoder.decode(encodedBase64)

          out.write(pageBytes, 0, pageBytes.size)
      }###__CMD_END__###
      writePage("UEsDBBQACAgIADV2OUwAAAAAAAAAAAAAAAAUAAQATUVUQS1JTkYvTUFOSUZFU1QuTUb+ygAA803My0xLLS7RDUstKs7Mz7NSMNQz4OXyTczM03XOSSwutlLwSM3JyQ/PL8pJ4eXi5QIAUEsHCMTT9BozAAAAMQAAAFBLAwQUAAgICAA0djlMAAAAAAAAAAAAAAAAEQAAAEhlbGxvV29ybGQkLmNsYXNzfVPtbhJBFD3D17ILthQs2tpKa1EBa7e1fiU0JqbW2GRLTaiYxl/DMtCtyy5ZtsbXUpOSSOID+FDGOwMRTEpJmLt7751z7jkz+/vPz18AnuAlQ+qdcF3/ox+4raIGxpA551+46XKvYx43z4UdaohSdtK21be5yxm0o+M3H6yDIsMNawqkypDcs13Hc8JXDNFSucGQ2FOvaRjQDUSQYoh1ueMx5EufrAlhPQwcr1OVW5b9oGPyHrfPhNnv8eCzWZfrvu+1NcwbyEiQ0fBrijiJLIPRF+HrXq/Gu4Jhu3QFtDUbt5rGTSxK6DzD6sy+UHwlT24zFErXgZUbUu6ygSXcIYuUaeb7QLREm4xeZZi3/ssRewIFHXexxpCeEraexD1yu0fzhy5Ztjgta3REiqyI+wbtfkDehmdOnwIPOhRyVzjMoKt5bZqUYeU6HQyRvk1iZ/dIQ6oGYkhJCZpOB7xN7Pt+iw4ha/mkssEDhzddcSIXKd3xRO2i2xTBOGPU/YvAFm8dVa5LYw49ukTi0Gv7DHGVwQ5Bx+jmMjKVmOgpCh1J+jO6zUCOovxpQ0QGSH9TL2Q+5sYNPm2QAIUhMqcDLGRzl7h1NMTS6eYAK7XvWM9uXOKh3BjBLq2VUTtKik0+lSknIRfwCJvUlSf+x9gi4KdUn6MIxKmuUUVaYfybbpeinE+vDGBWfmBnQpMeFcY0EiihMimCe6a64ngu4ekDVZBKFMMLOdNfUEsHCOhwxzQoAgAAzwMAAFBLAwQUAAgICAA0djlMAAAAAAAAAAAAAAAAEAAAAEhl")###__CMD_END__###
      writePage("bGxvV29ybGQuY2xhc3NNkd1uEkEUx8+ZYdhdvhdYKPEDF6mRJpZsrWkV02SXthaDpZUUg00vBjrgkmVXl6WJvpVeeOGFD+DL+AbGoYnBSSb/8/HLPznn/Prz4ycA7EAZIXEiPC94F4TetQKIkJvxG970uD9t9kYzMY4UoLK6prYXY+5xhPvd26AZioknsWZ/lfXdqc+jZShaCGz0ORILhN9xNF6cm0g0Ymgm6T+dDw9e3pze2xMGq+icVCeXTn3j7IygqVmfzEOkacM28SEmWMO+SN/d+9g8KJ5k3idotoE1TtUMtc51ycWnmdSr7SGjVUvnpr7/ZbO0q9QcTdWonrdJdXb1qP5BPca4bpgh1rBEdbf85M6+wYYd0rbjGwXNdhykA8mpNZIpYmyYR8da9E5jO5xmqwXNdRybOuJ1qpxj5MHRc90iHUbblsblTJNOabfs2PlNVqkc966eBb0tXWcxhNicuz5C6fFld73NfhS6/rTVGCAk19usK5BEUN70Di+6R3WEdPe/XisFachokIJsCjRIJGSkS/t2cC3k5frBMhyLY9eTSeXt0o/cuRi4C3fkCdv3g4hHbuDLA6j/TgMWEIjB6qH0k45SCzIryjpKVb9Bbus75L/eMlTWQXYYGFKTkojLryxAhdKqzQD+AlBLBwjuUBF24gEAAEoCAABQSwMECgAACAAANXY5TAAAAAAAAAAAAAAAAAkAAABNRVRBLUlORi9QSwECFAAUAAgICAA1djlMxNP0GjMAAAAxAAAAFAAEAAAAAAAAAAAAAAAAAAAATUVUQS1JTkYvTUFOSUZFU1QuTUb+ygAAUEsBAhQAFAAICAgANHY5TOhwxzQoAgAAzwMAABEAAAAAAAAAAAAAAAAAeQAAAEhlbGxvV29ybGQkLmNsYXNzUEsBAhQAFAAICAgANHY5TO5QEXbiAQAASgIAABAAAAAAAAAAAAAAAAAA4AIAAEhlbGxvV29ybGQuY2xhc3NQSwECCgAKAAAIAAA1djlMAAAAAAAA")###__CMD_END__###
      writePage("AAAAAAAACQAAAAAAAAAAAAAAAAAABQAATUVUQS1JTkYvUEsFBgAAAAAEAAQA+gAAACcFAAAAAA==")###__CMD_END__###
      out.close()###__CMD_END__###
      """
