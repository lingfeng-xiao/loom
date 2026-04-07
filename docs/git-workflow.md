# Template Git Workflow

## Recommended Flow

1. Create a feature branch from `main`.
2. Run the local validation commands that apply to your change.
3. Open a pull request and let `validate.yml` run.
4. Merge to `main` only after CI is green.

## Remotes

The template assumes the repository can push through GitHub over SSH on port `443` when needed:

`ssh://git@ssh.github.com:443/<owner>/<repo>.git`

## Keep In Sync

- directory names under `apps/`
- image names in GitHub Actions
- env keys in `.env.example`
- service names in compose and smoke tests
