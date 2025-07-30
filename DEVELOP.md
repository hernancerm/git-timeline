# Develop

## Value Git-timeline provides

- Improved readability for the output of git-log.

## User experience

```
$ git timeline
```

Options:

- `--help`
- `--version`

## Design

Steps to implement:

1. Git-timeline calls Git-log with `--format` to get log data as JSON.
2. Git-timeline handles JSON to get desired output.

## Features

- [x] One-line format.
- [x] Indication of merge commits.
- [ ] Has same completions as git-log.
- [x] Indication of commits with differing author and committer.
- [x] Accepts all options from git-log (yes, including `--graph`).
- [x] Clickable text linking to a ticket in issue tracker.
- [x] Clickable text linking to a pull-request in VCS host.
- [ ] Configuration file: Date format, colors for items.
- [ ] Paging works as expected.
- [x] Works well on big repos.

## Constraints

- No support for listing commits from a repo in a dir different from cwd.
- Should not work in Windows due to at least the pager. Should require tweaking to work there.

## How to use

1. Download binary for `git-timeline` (or build it).
2. Put the binary in the env var `PATH`.
3. Configure pager via env var `PAGER`.
4. (optional) Create a Git alias:

    ```
    [alias]
        l = timeline
    ```

5. Use it: `git timeline` or with the alias: `git l`.