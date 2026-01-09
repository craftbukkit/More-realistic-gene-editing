# Contributing to GenomeWorkbench

First off, thank you for considering contributing to GenomeWorkbench! This project is ambitious, and your help is greatly appreciated. Whether you're a developer, a tester, or a domain expert in biology, there are many ways to contribute.

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior.

## How Can I Contribute?

### Reporting Bugs

*   **Ensure the bug was not already reported** by searching on GitHub under [Issues](https://github.com/your-username/GenomeWorkbench/issues).
*   If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/your-username/GenomeWorkbench/issues/new). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample** or an **executable test case** demonstrating the expected behavior that is not occurring.

### Suggesting Enhancements

*   Open a new issue and describe the enhancement you have in mind.
*   Explain why this enhancement would be useful to other GenomeWorkbench users.
*   Provide a clear and concise description of the enhancement.

### Pull Requests

1.  **Fork the repository** and create your branch from `main`.
2.  **Follow the coding style** used in the project. Our style is based on the standard Google Java Style Guide.
3.  **Add tests!** If you add a new feature, please add tests for it. If you fix a bug, add a test that demonstrates the bug was fixed.
4.  **Ensure the test suite passes** locally by running `./gradlew test`.
5.  **Make sure your code lints.** Run `./gradlew check`.
6.  **Issue that pull request!**

## Development Setup

1.  Clone your fork of the repository: `git clone https://github.com/your-username/GenomeWorkbench.git`
2.  Navigate to the project directory: `cd GenomeWorkbench`
3.  Run `./gradlew genSources` to set up the development environment.
4.  Import the project into your IDE of choice (IntelliJ IDEA is recommended).
5.  Run `./gradlew runClient` to test your changes in the game.

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification. This makes the commit history easier to read and allows us to automate the release process.

Each commit message consists of a **header**, a **body** and a **footer**.

```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

**Example:**

```
feat(genome): add 2-bit encoding for base pairs

This commit introduces a new utility class, `TwoBitEncoding`, to efficiently store DNA sequences. This will be used in the networking layer to reduce packet size.

Fixes #12
```

### Type

Must be one of the following:

*   **feat**: A new feature
*   **fix**: A bug fix
*   **docs**: Documentation only changes
*   **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
*   **refactor**: A code change that neither fixes a bug nor adds a feature
*   **perf**: A code change that improves performance
*   **test**: Adding missing tests or correcting existing tests
*   **build**: Changes that affect the build system or external dependencies
*   **ci**: Changes to our CI configuration files and scripts
*   **chore**: Other changes that don't modify `src` or `test` files

## Questions?

If you have any questions, feel free to open an issue and ask!
