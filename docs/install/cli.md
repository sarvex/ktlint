!!! note Command Line usage
    If you don't plan to use `ktlint`'s command line interface then you can skip this section.

## Download and verification

### Download manually from github

All releases of `ktlint` can be downloaded from the [releases](https://github.com/pinterest/ktlint/releases) page.

### Download using curl

A particular version of `ktlint` can be downloaded with next command which also changes the file to an executable in directory `/usr/local/bin`:

```sh title="Download"
curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.49.0/ktlint && chmod a+x ktlint && sudo mv ktlint /usr/local/bin/
```

!!! tip "Curl not installed or behind proxy"
    If you don't have curl installed - replace `curl -sL` with `wget -qO-`.  
    If you are behind a proxy see - [curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / [wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. Usually simple:  
    ```shell
    http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...
    ```

### Verification of download

`ktlint.asc` contains PGP signature which you can verify with:

```sh title="Verify releases 0.32.0 and above"
curl -sS https://keybase.io/ktlint/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc
```

```sh title="Verify releases up through 0.31.0"
curl -sS https://keybase.io/shyiko/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc
```

### Package managers

`ktlint` can be installed via several OS specific package managers.

Install with [brew on macOS](https://brew.sh/) or [Homebrew on Linux](https://docs.brew.sh/Homebrew-on-Linux)
```sh
brew install ktlint
```

Install with [MacPorts](https://www.macports.org/)
```sh
port install ktlint
```

On Arch Linux install package [ktlint <sup>AUR</sup>](https://aur.archlinux.org/packages/ktlint/).

## Command line usage

### Rule set(s)

When no arguments are specified, the style of all Kotlin files (ending with '.kt' or '.kts') inside the current dir (recursively) are validated with the (non-experimental) rules from the [standard ruleset](../../rules/standard/). Hidden folders will be skipped.

```shell title="Default validation with standard ruleset"
ktlint
```

To validate with the [standard ruleset](../../rules/standard/) including the experimental rules run command below: 

```shell title="Validation with standard ruleset including the experimental rules"
ktlint --experimental
```

!!! note
    Instead of using this command line flag, it is advised to set `.editorconfig` property `ktlint_experimental = enabled` if you want the project always to be checked with the experimental rules.

To validate with a [custom ruleset](../../api/custom-rule-set/) run command below:  

```shell title="Validation with standard and a custom ruleset"
ktlint --ruleset=/path/to/custom-ruleset.jar
# or
ktlint -R /path/to/custom-ruleset.jar
```

!!! note
    If the custom rule set contains rules that are marked as experimental, those rule will only be run when `.editorconfig` property `ktlint_experimental = enabled` is set (or command line parameter `--experimental` is specified).

### Format (autocorrect)

Most style violations can be corrected automatically. Errors that can not be corrected, are printed to `stderr`.

```shell title="Autocorrect style violations"
ktlint --format
# or
ktlint -F
```

### Globs

Globs can be used to specify more exactly what files and directories are to be validated. `ktlint` uses the [`.gitignore` pattern style syntax for globs](https://git-scm.com/docs/gitignore). Globs are processed from left to right. Prepend a glob with `!` to negate it. Hidden folders will be skipped.

```shell title="Check only certain locations starting from the current directory"
# Check all '.kt' files in 'src/' directory, but ignore files ending with 'Test.kt':
ktlint 'src/**/*.kt' '!src/**/*Test.kt'

# Check all '.kt' files in 'src/' directory, but ignore 'generated' directory and its subdirectories:
ktlint 'src/**/*.kt' '!src/**/generated/**'
```

### Violation reporting

`ktlint` supports different type of reporters for lint violations. When not specified the `plain` reporter is used. Optionally the `plain` reporter can group the violations per file.

```shell title="Style violation grouped by file"
$ ktlint --reporter=plain?group_by_file
```

When using `ktlint` on an existing project, the number of violations can be huge. To get more insights in which rules are causing the most violations, the `plain-summary` reporter can be used.
```shell title="Style violations counted per rule"
$ ktlint --reporter=plain-summary
```

Other built-in reporters are: `json`, `sarif`, `checkstyle`, and `html`

Style violations can be written to an output file which is convenient when multiple reporters are specified. In example below, the plain reporter is used to write to the console while the checkstyle reports is written to a file:

```shell title="Multiple reporters"
ktlint --reporter=plain --reporter=checkstyle,output=ktlint-report-in-checkstyle-format.xml
```

If resolving all existing errors in a project is unwanted, it is possible to create a baseline and in following invocations compare violations against this baseline. Violations that are registered in the baseline, will be ignored silently. Remove the baseline file in case you want to reset it.

```shell title="Check against a baseline file"
ktlint --baseline=ktlint-baseline.xml # Baseline is created when not existing
```

### Logging

Logging information is written to `stdout`. The amount of logging can be influenced by setting the minimal log level using option `--log-level` or `-l` to one of values `trace`, `debug`, `info`, `warn`, `error`, or `none` to suppress all logging.

By default, the `info` log level is used meaning that all log lines at level `info`, `warn` and `error` are shown while suppressing log lines at level `debug` or `trace`.

### Rule configuration (`.editorconfig`)

Some rules can be tweaked via the [`editorconfig file`](../../rules/configuration-ktlint/).

A scaffold of the `.editorconfig file` can be generated with command below. Note: that the generated file only contains configuration settings which are actively used by the [rules which are loaded](#rule-sets):

```shell title="Generate .editorconfig"
ktlint generateEditorConfig
# or
ktlint --experimental generateEditorConfig
# or
ktlint --experimental --ruleset=/path/to/custom-ruleset.jar generateEditorConfig
```

Normally this file is located in the root of your project directory. In case the file is located in a sub folder of the project, the settings of that file only applies to that subdirectory and its folders (recursively). Ktlint automatically detects and reads all `.editorconfig` files in your project.

Use command below, to specify a default `editorconfig`. In case a property is not defined in any `.editorconfig` file on the path to the file, the value from the default file is used. The path may point to any valid file or directory. The path can be relative or absolute. Depending on your OS, the "~" at the beginning of a path is replaced by the user home directory.

```shell title="Override '.editorconfig'"
ktlint --editorconfig=/path/to/.editorconfig
```

!!! warning "Overrides '.editorconfig' in project directory" in KtLint 0.46 and older
    When specifying this option using ktlint 0.46 or older, all `.editorconfig` files in the project directory are being ignored. Starting from KtLint 0.47 the properties in this file are used as fallback.

### Stdin && stdout

With command below, the input is read from `stdin` and the violations are printed to `stderr`. Logging is written to `stdout`.

```shell title="Lint from stdin"
ktlint --stdin
```

When combined with the `--format` option, the formatted code is written to `stdout` and the violations are printed to `stderr`:

```shell title="Format from stdin and write to stdout"
ktlint --stdin -F
```

!!! tip Suppress logging and error output
    Logging output printed to `stdout` can be suppressed by setting `--log-level=none` (see [logging](#logging)).
    Output printed to `stderr` can be suppressed in different ways. To ignore all error output, add `2> /dev/null` to the end of the command line. Otherwise, specify a [reporter](#violation-reporting) to write the error output to a file.


### Git hooks

Predefined git hooks can be installed, to automatically validate lint errors before commit or push.

```shell title="Install git pre-commit hook"
ktlint installGitPreCommitHook
```

```shell title="Install git pre-push hook"
ktlint installGitPrePushHook
```

### Miscellaneous flags and commands

`-a` or `--android`: Turn on Android Kotlin Style Guide compatibility. This flag is most likely to be removed in a future version. Use [`.editorconfig ktlint_code_style`](../../rules/configuration-ktlint/#code-style). 

`--color` and `--color-name=<colorName>`: Make output colorful and optionally set the color name to use.

`--disabled_rules=<disabledRules>`: A comma-separated list of rules to globally disable. To disable the standard ktlint rule-set use `--disabled_rules=standard`.  This flag is most likely to be removed in a future version. Use [`.editorconfig disabled_rules`](../../rules/configuration-ktlint/#disabled-rules).

`-h` or `--help`: Prints help information.

`--limit=<limit>`: Maximum number of errors to show (default: show all)

`--relative`: Print files relative to the working directory (e.g. dir/file.kt instead of /home/user/project/dir/file.kt)

`--patterns-from-stdin[=<delimiter>]`: Reads additional patterns from `stdin`, where the patterns are separated by `<delimiter>`. If `=<delimiter>` is omitted, newline is used as fallback delimiter. If an empty string is given, the `NUL` byte is used as delimiter instead.
If this option is given, then the default patterns are disabled.
Options `--stdin` and `--patterns-from-stdin` are mutually exclusive, only one of them can be given at a time.

`-V` or `--version`: Prints version information and exit.

### Microsoft Windows users

!!! tip "Microsoft Windows"
    On Microsoft Windows you'll have to use `java -jar ktlint ...`.
