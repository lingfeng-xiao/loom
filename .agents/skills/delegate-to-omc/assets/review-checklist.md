# Review Checklist

- Read `brief.md` first. Treat it as the source of truth.
- Verify `Done when` is fully satisfied.
- Verify `Non-goals` were respected.
- Check whether any changed files fall outside the declared scope.
- Confirm validation commands were actually run and their outcome is reported honestly.
- Confirm the worker output contains all required contract sections.
- Mark `PASS` only if the result is complete, bounded, and reviewable.
- Mark `NEEDS_FIX` when scope drift, missing validation, or missing output structure appears.
- If a fix pass is needed, ask only for the smallest correction list.
- Do not close the task until `REVIEW_RESULT: PASS` and `closeout.json` is written.
