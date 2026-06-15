# GitAegis Test Sandbox

This repository is a pre-configured environment for testing the GitAegis CLI assistant.


## Directory Structure
<!-- git-aegis-structure-start -->
```plaintext
project/
├── README.md # Markdown documentation
├── run_integration_tests.py # Ensure test repo path is clean
├── src/
│   ├── Addition.py # this function adds two numbers
│   ├── Division.py # this function divides two numbers
│   ├── Multiplication
│   ├── Subtraction.py # this function subtracts two numbers
│   ├── leaked_secrets.py # git-aegis: local-only
│   └── main.py # Baseline helper function
└── test_cli_wrapper.py # Ensure the primary codebase is in the python path
```
<!-- git-aegis-structure-end -->

<!-- git-aegis-changelog -->
* 2026-06-15 14:24:44 - docs(changelog): add entry for simulated secret token update
* 2026-06-15 14:21:08 - chore(leaked-secrets): update simulated secret token value
* 2026-06-15 14:19:02 - feat(math): add division function and refactor addition script to function
* 2026-06-15 14:17:23 - feat(arithmetic): add basic arithmetic operations
