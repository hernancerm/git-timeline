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
- [ ] Clickable text linking to a ticket in a configured issue tracker.
- [ ] Clickable text linking to a pull-request in a configured VCS host.
- [ ] Configuration file: Date format, colors for items.
- [ ] Paging works as expected.
- [x] Works well on big repos.