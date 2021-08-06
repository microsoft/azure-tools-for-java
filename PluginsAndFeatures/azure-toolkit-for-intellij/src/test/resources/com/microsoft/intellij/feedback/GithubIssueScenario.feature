Feature: GithubIssue tests

  Scenario: Can cut well encoded and dual encoded URL ending
    Then check the following source and result rows of URL ending replacement
      | doFilter%28AuthFilter.java%3A83%29% | doFilter%28AuthFilter.java%3A83%29 |
      | doFilter%28AuthFilter.java%3A83%29%2 | doFilter%28AuthFilter.java%3A83%29 |
      | doFilter%28AuthFilter.java%3A83%29%20 | doFilter%28AuthFilter.java%3A83%29%20 |
      | doFilter%28AuthFilter.java | doFilter%28AuthFilter.java |
      | doFilter%2528AuthFilter.java%253A83%2529%25 | doFilter%2528AuthFilter.java%253A83%2529 |
      | doFilter%2528AuthFilter.java%253A83%2529%252 | doFilter%2528AuthFilter.java%253A83%2529 |
      | doFilter%2528AuthFilter.java%253A83%2529%2520 | doFilter%2528AuthFilter.java%253A83%2529%2520 |
