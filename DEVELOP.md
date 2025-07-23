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

- One-line format.
- Indication of merge commits.
- Accepts all options from git-log.
- Has same completions as git-log.
- Indication of commits with differing author and committer.
- Clickable text linking to a ticket in a configured issue tracker.
- Clickable text linking to a pull-request in a configured VCS host.

## Build GraalVM native image

```
mvn -Pnative package
```