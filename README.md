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
│   ├── Multiplication
│   ├── Subtraction.py # this function subtracts two numbers
│   └── main.py # Baseline helper function
└── test_cli_wrapper.py # Ensure the primary codebase is in the python path
```
<!-- git-aegis-structure-end -->

<!-- git-aegis-changelog -->
* 2026-06-15 14:17:23 - feat(arithmetic): add basic arithmetic operations
